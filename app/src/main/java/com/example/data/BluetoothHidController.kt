package com.example.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.example.domain.hid.HidKeyCodes
import com.example.domain.hid.HidReportDescriptor
import com.example.domain.model.DiscoveredBtDevice
import com.example.domain.model.HidConnectionPhase
import com.example.domain.model.HidStatusState
import com.example.domain.model.PairedPcDevice
import com.example.service.HidConnectionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Production Bluetooth HID Device controller.
 * Registers the smartphone as a standard Composite Bluetooth Keyboard + Mouse + Consumer Remote
 * so Windows 11 (e.g. HP ProBook 445R G6) connects natively without any PC software or driver install.
 */
class BluetoothHidController(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository
) {
    private val tag = "DeckLinkHID"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val hidExecutor = Executors.newSingleThreadExecutor()

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    private var hidDeviceProxy: BluetoothHidDevice? = null
    private var activeHostDevice: BluetoothDevice? = null
    private var manualDisconnectRequested = false
    private var autoReconnectEnabled = true
    private var autoConnectConnectedDevices = true
    private var foregroundServiceEnabled = true
    private var connectionTimeoutSec = 15
    private var lastConnectedAddress: String? = null
    private var autoConnectAddresses: Set<String> = emptySet()
    private var disabledAutoConnectAddresses: Set<String> = emptySet()

    private var connectTimeoutJob: Job? = null
    private var reconnectJob: Job? = null
    private var autoConnectJob: Job? = null
    private var isReceiversRegistered = false

    private val rssiCache = mutableMapOf<String, Int>()
    private val aclConnectedAddresses = mutableSetOf<String>()
    private val suppressedAutoConnectAddresses = mutableSetOf<String>()

    private val _hidStatus = MutableStateFlow(HidStatusState())
    val hidStatus: StateFlow<HidStatusState> = _hidStatus.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<PairedPcDevice>>(emptyList())
    val pairedDevices: StateFlow<List<PairedPcDevice>> = _pairedDevices.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredBtDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredBtDevice>> = _discoveredDevices.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val btReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    when (state) {
                        BluetoothAdapter.STATE_ON -> initializeHidProfile()
                        BluetoothAdapter.STATE_OFF, BluetoothAdapter.STATE_TURNING_OFF -> {
                            activeHostDevice = null
                            hidDeviceProxy = null
                            stopForegroundServiceSafe()
                            _hidStatus.update {
                                it.copy(
                                    phase = HidConnectionPhase.BLUETOOTH_OFF,
                                    connectedDeviceName = null,
                                    connectedDeviceAddress = null,
                                    isHidAppRegistered = false,
                                    statusMessage = "Bluetooth is turned off"
                                )
                            }
                        }
                    }
                }

                BluetoothDevice.ACTION_FOUND -> {
                    if (!hasScanPermission()) return
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device ?: return
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                    val validRssi = if (rssi > -120 && rssi < 20) rssi else -75
                    rssiCache[device.address] = validRssi

                    val name = safeDeviceName(device) ?: return
                    val majorClass = runCatching { device.bluetoothClass?.majorDeviceClass ?: 0 }.getOrDefault(0)
                    val isComputer = majorClass == BluetoothClass.Device.Major.COMPUTER
                    val bondState = safeBondState(device)

                    _discoveredDevices.update { current ->
                        val updated = current.filterNot { it.address == device.address }.toMutableList()
                        updated.add(
                            DiscoveredBtDevice(
                                name = name,
                                address = device.address,
                                rssiDbm = validRssi,
                                bondState = bondState,
                                isLikelyComputer = isComputer
                            )
                        )
                        updated.sortedWith(
                            compareByDescending<DiscoveredBtDevice> { it.isLikelyComputer }
                                .thenByDescending { it.rssiDbm }
                        )
                    }
                    refreshPairedDevices()
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isDiscovering.value = false
                }

                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    refreshPairedDevices()
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
                    if (bondState == BluetoothDevice.BOND_BONDED && device != null) {
                        _discoveredDevices.update { list ->
                            list.map {
                                if (it.address == device.address) it.copy(bondState = BluetoothDevice.BOND_BONDED)
                                else it
                            }
                        }
                        if (_hidStatus.value.isHidAppRegistered &&
                            _hidStatus.value.phase != HidConnectionPhase.CONNECTED
                        ) {
                            connectToDevice(device.address)
                        }
                    }
                }

                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    if (!hasConnectPermission()) return
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device ?: return
                    aclConnectedAddresses.add(device.address)
                    refreshPairedDevices()

                    if (autoConnectConnectedDevices &&
                        safeBondState(device) == BluetoothDevice.BOND_BONDED &&
                        _hidStatus.value.isHidAppRegistered &&
                        _hidStatus.value.phase != HidConnectionPhase.CONNECTED &&
                        _hidStatus.value.phase != HidConnectionPhase.CONNECTING &&
                        device.address !in suppressedAutoConnectAddresses
                    ) {
                        val name = safeDeviceName(device) ?: device.address
                        val major = runCatching { device.bluetoothClass?.majorDeviceClass ?: 0 }.getOrDefault(0)
                        val likelyComputer = isLikelyComputerDevice(name, major)
                        if (isDeviceAutoConnectEnabled(
                                address = device.address,
                                isLikelyComputer = likelyComputer,
                                lastConnectedAddress = lastConnectedAddress,
                                autoConnectAddresses = autoConnectAddresses,
                                disabledAutoConnectAddresses = disabledAutoConnectAddresses
                            )
                        ) {
                            autoConnectSingleAclDevice(device.address, name)
                        }
                    }
                }

                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device ?: return
                    aclConnectedAddresses.remove(device.address)
                    refreshPairedDevices()
                }

                HidConnectionService.BROADCAST_DISCONNECT_REQUEST -> {
                    disconnect()
                }
            }
        }
    }

    fun updateSettings(
        autoReconnect: Boolean,
        autoConnectConnected: Boolean,
        timeoutSec: Int,
        foregroundService: Boolean,
        lastAddress: String?,
        pinnedAutoConnectAddresses: Set<String>,
        excludedAutoConnectAddresses: Set<String>
    ) {
        val newlyEnabled = !autoConnectConnectedDevices && autoConnectConnected
        autoReconnectEnabled = autoReconnect
        autoConnectConnectedDevices = autoConnectConnected
        connectionTimeoutSec = timeoutSec
        foregroundServiceEnabled = foregroundService
        lastConnectedAddress = lastAddress
        autoConnectAddresses = pinnedAutoConnectAddresses
        disabledAutoConnectAddresses = excludedAutoConnectAddresses
        // Clear suppression for addresses user explicitly enabled
        suppressedAutoConnectAddresses.removeAll(pinnedAutoConnectAddresses)
        refreshPairedDevices()
        if (newlyEnabled) {
            triggerAutoConnectIfIdle()
        }
    }

    fun start() {
        registerReceiversIfNeeded()
        initializeHidProfile()
    }

    fun stop() {
        connectTimeoutJob?.cancel()
        reconnectJob?.cancel()
        autoConnectJob?.cancel()
        cancelDiscovery()
        if (isReceiversRegistered) {
            runCatching { context.unregisterReceiver(btReceiver) }
            isReceiversRegistered = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && hasConnectPermission()) {
            runCatching {
                hidDeviceProxy?.let { proxy ->
                    @SuppressLint("MissingPermission")
                    val unregistered = proxy.unregisterApp()
                    Log.d(tag, "HID app unregistered: $unregistered")
                    bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, proxy)
                }
            }
        }
        stopForegroundServiceSafe()
    }

    private fun registerReceiversIfNeeded() {
        if (isReceiversRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(HidConnectionService.BROADCAST_DISCONNECT_REQUEST)
        }
        ContextCompat.registerReceiver(
            context,
            btReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        isReceiversRegistered = true
    }

    fun initializeHidProfile() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            _hidStatus.update {
                it.copy(
                    phase = HidConnectionPhase.HID_UNSUPPORTED,
                    statusMessage = "Bluetooth HID Device API requires Android 9+"
                )
            }
            return
        }

        if (!hasConnectPermission()) {
            _hidStatus.update {
                it.copy(
                    phase = HidConnectionPhase.MISSING_PERMISSIONS,
                    statusMessage = "Grant Bluetooth permissions to enable wireless HID"
                )
            }
            return
        }

        val adapter = bluetoothAdapter
        if (adapter == null) {
            _hidStatus.update {
                it.copy(
                    phase = HidConnectionPhase.HID_UNSUPPORTED,
                    statusMessage = "No Bluetooth hardware adapter detected"
                )
            }
            return
        }

        if (!adapter.isEnabled) {
            _hidStatus.update {
                it.copy(
                    phase = HidConnectionPhase.BLUETOOTH_OFF,
                    statusMessage = "Turn on Bluetooth to connect to your Windows PC"
                )
            }
            return
        }

        refreshPairedDevices()

        if (hidDeviceProxy != null && _hidStatus.value.isHidAppRegistered) {
            return
        }

        _hidStatus.update {
            it.copy(
                phase = HidConnectionPhase.REGISTERING_HID,
                statusMessage = "Registering Composite HID Device (Keyboard + Mouse)..."
            )
        }

        val ok = adapter.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE)
        if (!ok) {
            _hidStatus.update {
                it.copy(
                    phase = HidConnectionPhase.HID_UNSUPPORTED,
                    statusMessage = "Device firmware denied Bluetooth HID Device profile"
                )
            }
        }
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile != BluetoothProfile.HID_DEVICE || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
            val hid = proxy as? BluetoothHidDevice ?: return
            hidDeviceProxy = hid
            registerCompositeHidApp(hid)
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDeviceProxy = null
                activeHostDevice = null
                stopForegroundServiceSafe()
                _hidStatus.update {
                    it.copy(
                        phase = HidConnectionPhase.REGISTERING_HID,
                        isHidAppRegistered = false,
                        connectedDeviceName = null,
                        connectedDeviceAddress = null,
                        statusMessage = "Bluetooth HID service disconnected"
                    )
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("MissingPermission")
    private fun registerCompositeHidApp(hid: BluetoothHidDevice) {
        if (!hasConnectPermission()) return
        val sdpSettings = BluetoothHidDeviceAppSdpSettings(
            "DeckLink Wireless HID",
            "Android Composite Keyboard, Mouse & Media Remote",
            "DeckLink",
            BluetoothHidDevice.SUBCLASS1_COMBO,
            HidReportDescriptor.DESCRIPTOR
        )

        val qosOut = BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
            800,
            9,
            0,
            11250,
            BluetoothHidDeviceAppQosSettings.MAX
        )

        val registered = hid.registerApp(sdpSettings, null, qosOut, hidExecutor, hidCallback)
        if (!registered) {
            _hidStatus.update {
                it.copy(
                    phase = HidConnectionPhase.ERROR,
                    isHidAppRegistered = false,
                    statusMessage = "Failed to register HID SDP profile on adapter"
                )
            }
        }
    }

    @get:RequiresApi(Build.VERSION_CODES.P)
    private val hidCallback by lazy {
        object : BluetoothHidDevice.Callback() {
            @SuppressLint("MissingPermission")
            override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
                scope.launch {
                    if (registered) {
                        syncActiveOsBluetoothConnections()
                        val connectedHidHosts = if (hasConnectPermission()) {
                            runCatching { hidDeviceProxy?.connectedDevices }.getOrNull().orEmpty()
                        } else emptyList()
                        val existingHost = pluggedDevice?.takeIf {
                            hasConnectPermission() &&
                                hidDeviceProxy?.getConnectionState(it) == BluetoothProfile.STATE_CONNECTED
                        } ?: connectedHidHosts.firstOrNull()

                        if (existingHost != null) {
                            onHostConnected(existingHost)
                        } else {
                            _hidStatus.update {
                                it.copy(
                                    phase = HidConnectionPhase.READY_TO_PAIR,
                                    isHidAppRegistered = true,
                                    statusMessage = "HID Profile Active · Checking connected devices..."
                                )
                            }
                            refreshPairedDevices()
                            triggerAutoConnectIfIdle()
                        }
                    } else {
                        activeHostDevice = null
                        stopForegroundServiceSafe()
                        _hidStatus.update {
                            it.copy(
                                phase = HidConnectionPhase.ERROR,
                                isHidAppRegistered = false,
                                connectedDeviceName = null,
                                connectedDeviceAddress = null,
                                statusMessage = "HID profile unregistered"
                            )
                        }
                    }
                    refreshPairedDevices()
                }
            }

            @SuppressLint("MissingPermission")
            override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
                scope.launch {
                    when (state) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            onHostConnected(device)
                        }

                        BluetoothProfile.STATE_CONNECTING -> {
                            _hidStatus.update {
                                it.copy(
                                    phase = HidConnectionPhase.CONNECTING,
                                    connectedDeviceName = safeDeviceName(device) ?: device.address,
                                    connectedDeviceAddress = device.address,
                                    statusMessage = "Negotiating L2CAP HID with ${safeDeviceName(device) ?: "PC"}..."
                                )
                            }
                        }

                        BluetoothProfile.STATE_DISCONNECTED -> {
                            val previousAddress = activeHostDevice?.address ?: device.address
                            val previousName = safeDeviceName(device) ?: "PC"
                            connectTimeoutJob?.cancel()
                            activeHostDevice = null
                            stopForegroundServiceSafe()

                            _hidStatus.update {
                                it.copy(
                                    phase = HidConnectionPhase.READY_TO_PAIR,
                                    connectedDeviceName = null,
                                    connectedDeviceAddress = null,
                                    statusMessage = "Disconnected from $previousName"
                                )
                            }
                            refreshPairedDevices()

                            if (!manualDisconnectRequested && autoReconnectEnabled && previousAddress.isNotBlank()) {
                                scheduleAutoReconnect(previousAddress, previousName)
                            }
                            manualDisconnectRequested = false
                        }
                    }
                }
            }

            @SuppressLint("MissingPermission")
            override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
                if (!hasConnectPermission()) return
                val report = when (id.toInt()) {
                    HidReportDescriptor.REPORT_ID_KEYBOARD -> HidReportDescriptor.buildKeyboardReport(0)
                    HidReportDescriptor.REPORT_ID_MOUSE -> HidReportDescriptor.buildMouseReport(0, 0, 0)
                    HidReportDescriptor.REPORT_ID_CONSUMER -> HidReportDescriptor.buildConsumerReport(0)
                    else -> ByteArray(1)
                }
                hidDeviceProxy?.replyReport(device, type, id, report)
            }

            @SuppressLint("MissingPermission")
            override fun onSetReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray) {
                if (!hasConnectPermission()) return
                if (id.toInt() == HidReportDescriptor.REPORT_ID_KEYBOARD && data.isNotEmpty()) {
                    val leds = data[0].toInt()
                    scope.launch {
                        _hidStatus.update {
                            it.copy(
                                hostNumLock = (leds and 0x01) != 0,
                                hostCapsLock = (leds and 0x02) != 0,
                                hostScrollLock = (leds and 0x04) != 0
                            )
                        }
                    }
                }
                hidDeviceProxy?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
            }

            override fun onInterruptData(device: BluetoothDevice, reportId: Byte, data: ByteArray) {
                if (reportId.toInt() == HidReportDescriptor.REPORT_ID_KEYBOARD && data.isNotEmpty()) {
                    val leds = data[0].toInt()
                    scope.launch {
                        _hidStatus.update {
                            it.copy(
                                hostNumLock = (leds and 0x01) != 0,
                                hostCapsLock = (leds and 0x02) != 0,
                                hostScrollLock = (leds and 0x04) != 0
                            )
                        }
                    }
                }
            }
        }
    }

    private fun onHostConnected(device: BluetoothDevice) {
        connectTimeoutJob?.cancel()
        reconnectJob?.cancel()
        autoConnectJob?.cancel()
        manualDisconnectRequested = false
        suppressedAutoConnectAddresses.remove(device.address)
        aclConnectedAddresses.add(device.address)
        activeHostDevice = device
        val name = safeDeviceName(device) ?: device.address
        val rssi = rssiCache[device.address]

        _hidStatus.update {
            it.copy(
                phase = HidConnectionPhase.CONNECTED,
                isHidAppRegistered = true,
                connectedDeviceName = name,
                connectedDeviceAddress = device.address,
                lastSignalRssiDbm = rssi,
                statusMessage = "Connected via Bluetooth HID"
            )
        }
        scope.launch {
            preferencesRepository.saveLastConnectedDevice(device.address, name)
        }
        if (foregroundServiceEnabled) {
            runCatching { HidConnectionService.start(context, name) }
        }
        refreshPairedDevices()
    }

    @SuppressLint("MissingPermission")
    private fun syncActiveOsBluetoothConnections() {
        if (!hasConnectPermission()) return
        val mgr = bluetoothManager ?: return
        runCatching {
            mgr.getConnectedDevices(BluetoothProfile.GATT).forEach { aclConnectedAddresses.add(it.address) }
            mgr.getConnectedDevices(BluetoothProfile.GATT_SERVER).forEach { aclConnectedAddresses.add(it.address) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                hidDeviceProxy?.connectedDevices?.forEach { aclConnectedAddresses.add(it.address) }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        if (!hasConnectPermission()) {
            _pairedDevices.value = emptyList()
            return
        }
        syncActiveOsBluetoothConnections()
        val adapter = bluetoothAdapter ?: return
        val bonded = runCatching { adapter.bondedDevices }.getOrNull().orEmpty()
        val activeAddr = activeHostDevice?.address
        val list = bonded.map { device ->
            val name = safeDeviceName(device) ?: device.address
            val major = runCatching { device.bluetoothClass?.majorDeviceClass ?: 0 }.getOrDefault(0)
            val isComputer = isLikelyComputerDevice(name, major)
            val isConnected = device.address == activeAddr
            val isAclActive = isConnected || (device.address in aclConnectedAddresses)
            val autoEnabled = isDeviceAutoConnectEnabled(
                address = device.address,
                isLikelyComputer = isComputer,
                lastConnectedAddress = lastConnectedAddress,
                autoConnectAddresses = autoConnectAddresses,
                disabledAutoConnectAddresses = disabledAutoConnectAddresses
            )
            PairedPcDevice(
                name = name,
                address = device.address,
                majorDeviceClass = major,
                isLikelyComputer = isComputer,
                isConnected = isConnected,
                lastRssiDbm = rssiCache[device.address],
                isBluetoothAclConnected = isAclActive,
                autoConnectEnabled = autoEnabled
            )
        }.sortedWith(
            compareByDescending<PairedPcDevice> { it.isConnected }
                .thenByDescending { it.isBluetoothAclConnected }
                .thenByDescending { it.autoConnectEnabled }
                .thenByDescending { it.isLikelyComputer }
                .thenBy { it.name.lowercase() }
        )
        _pairedDevices.value = list
    }

    fun triggerAutoConnectIfIdle() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        if (!autoConnectConnectedDevices || !hasConnectPermission()) return
        val status = _hidStatus.value
        if (!status.isHidAppRegistered ||
            status.phase == HidConnectionPhase.CONNECTED ||
            status.phase == HidConnectionPhase.CONNECTING
        ) {
            return
        }
        if (autoConnectJob?.isActive == true) return

        refreshPairedDevices()
        val candidates = selectAutoConnectCandidates(
            pairedDevices = _pairedDevices.value,
            lastConnectedAddress = lastConnectedAddress,
            suppressedAddresses = suppressedAutoConnectAddresses
        )

        if (candidates.isEmpty()) {
            _hidStatus.update {
                if (it.phase == HidConnectionPhase.READY_TO_PAIR) {
                    it.copy(statusMessage = "HID Profile Active · Select your Windows PC")
                } else it
            }
            return
        }

        autoConnectJob = scope.launch {
            for (candidate in candidates.take(3)) {
                if (!autoConnectConnectedDevices ||
                    _hidStatus.value.phase == HidConnectionPhase.CONNECTED
                ) {
                    break
                }
                if (candidate.address in suppressedAutoConnectAddresses) continue

                val initiated = attemptAutoConnectProbe(candidate.address, candidate.name)
                if (initiated) {
                    // Wait up to 6s for this candidate to confirm HID connection
                    val deadline = System.currentTimeMillis() + 6000L
                    while (System.currentTimeMillis() < deadline) {
                        val currentPhase = _hidStatus.value.phase
                        if (currentPhase == HidConnectionPhase.CONNECTED) return@launch
                        if (currentPhase == HidConnectionPhase.READY_TO_PAIR &&
                            _hidStatus.value.connectedDeviceAddress == null
                        ) {
                            break
                        }
                        delay(200L)
                    }
                    if (_hidStatus.value.phase == HidConnectionPhase.CONNECTING) {
                        cancelPendingProbe(candidate.address)
                    }
                }
            }

            if (_hidStatus.value.phase != HidConnectionPhase.CONNECTED) {
                _hidStatus.update {
                    it.copy(
                        phase = HidConnectionPhase.READY_TO_PAIR,
                        connectedDeviceName = null,
                        connectedDeviceAddress = null,
                        statusMessage = "HID Profile Active · Tap a paired PC to connect"
                    )
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun autoConnectSingleAclDevice(address: String, name: String) {
        autoConnectJob?.cancel()
        autoConnectJob = scope.launch {
            delay(550L) // Allow Windows 11 SDP / link setup to settle
            if (!autoConnectConnectedDevices ||
                _hidStatus.value.phase == HidConnectionPhase.CONNECTED ||
                address in suppressedAutoConnectAddresses
            ) {
                return@launch
            }
            attemptAutoConnectProbe(address, name)
        }
    }

    @SuppressLint("MissingPermission")
    private fun attemptAutoConnectProbe(address: String, name: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || !hasConnectPermission()) return false
        val proxy = hidDeviceProxy ?: return false
        val device = runCatching { bluetoothAdapter?.getRemoteDevice(address) }.getOrNull() ?: return false
        _hidStatus.update {
            it.copy(
                phase = HidConnectionPhase.CONNECTING,
                connectedDeviceName = name,
                connectedDeviceAddress = address,
                statusMessage = "Auto-connecting to $name..."
            )
        }
        val ok = proxy.connect(device)
        if (!ok) {
            _hidStatus.update {
                it.copy(
                    phase = HidConnectionPhase.READY_TO_PAIR,
                    connectedDeviceName = null,
                    connectedDeviceAddress = null
                )
            }
        }
        return ok
    }

    @SuppressLint("MissingPermission")
    private fun cancelPendingProbe(address: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || !hasConnectPermission()) return
        val device = runCatching { bluetoothAdapter?.getRemoteDevice(address) }.getOrNull() ?: return
        runCatching { hidDeviceProxy?.disconnect(device) }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(address: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        if (!hasConnectPermission()) {
            _hidStatus.update {
                it.copy(
                    phase = HidConnectionPhase.MISSING_PERMISSIONS,
                    statusMessage = "Bluetooth Connect permission required"
                )
            }
            return
        }
        val adapter = bluetoothAdapter ?: return
        val proxy = hidDeviceProxy
        if (proxy == null || !_hidStatus.value.isHidAppRegistered) {
            initializeHidProfile()
            return
        }

        cancelDiscovery()
        autoConnectJob?.cancel()
        suppressedAutoConnectAddresses.remove(address)
        val device = runCatching { adapter.getRemoteDevice(address) }.getOrNull() ?: return
        val name = safeDeviceName(device) ?: address
        manualDisconnectRequested = false
        reconnectJob?.cancel()

        _hidStatus.update {
            it.copy(
                phase = HidConnectionPhase.CONNECTING,
                connectedDeviceName = name,
                connectedDeviceAddress = address,
                statusMessage = "Connecting to $name..."
            )
        }

        val initiated = proxy.connect(device)
        if (!initiated) {
            _hidStatus.update {
                it.copy(
                    phase = HidConnectionPhase.READY_TO_PAIR,
                    connectedDeviceName = null,
                    connectedDeviceAddress = null,
                    statusMessage = "Connection rejected. Pair from Windows Bluetooth Settings first."
                )
            }
            return
        }

        connectTimeoutJob?.cancel()
        connectTimeoutJob = scope.launch {
            delay(connectionTimeoutSec * 1000L)
            if (_hidStatus.value.phase == HidConnectionPhase.CONNECTING) {
                runCatching { proxy.disconnect(device) }
                _hidStatus.update {
                    it.copy(
                        phase = HidConnectionPhase.READY_TO_PAIR,
                        connectedDeviceName = null,
                        connectedDeviceAddress = null,
                        statusMessage = "Connection timed out after ${connectionTimeoutSec}s. Check PC Bluetooth."
                    )
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        manualDisconnectRequested = true
        connectTimeoutJob?.cancel()
        reconnectJob?.cancel()
        autoConnectJob?.cancel()
        stopForegroundServiceSafe()

        activeHostDevice?.address?.let { suppressedAutoConnectAddresses.add(it) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && hasConnectPermission()) {
            activeHostDevice?.let { host ->
                runCatching { hidDeviceProxy?.disconnect(host) }
            }
        }
        activeHostDevice = null
        _hidStatus.update {
            it.copy(
                phase = if (it.isHidAppRegistered) HidConnectionPhase.READY_TO_PAIR else it.phase,
                connectedDeviceName = null,
                connectedDeviceAddress = null,
                statusMessage = "Disconnected manually"
            )
        }
        refreshPairedDevices()
    }

    private fun scheduleAutoReconnect(address: String, name: String) {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            _hidStatus.update {
                it.copy(statusMessage = "Link dropped · Reconnecting to $name...")
            }
            delay(2500L)
            if (_hidStatus.value.phase != HidConnectionPhase.CONNECTED && !manualDisconnectRequested) {
                connectToDevice(address)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery(): Boolean {
        if (!hasScanPermission()) return false
        val adapter = bluetoothAdapter ?: return false
        if (!adapter.isEnabled) return false
        if (adapter.isDiscovering) adapter.cancelDiscovery()
        _discoveredDevices.value = emptyList()
        val started = adapter.startDiscovery()
        _isDiscovering.value = started
        return started
    }

    @SuppressLint("MissingPermission")
    fun cancelDiscovery() {
        if (!hasScanPermission()) return
        val adapter = bluetoothAdapter ?: return
        if (adapter.isDiscovering) {
            runCatching { adapter.cancelDiscovery() }
        }
        _isDiscovering.value = false
    }

    @SuppressLint("MissingPermission")
    fun pairDevice(address: String): Boolean {
        if (!hasConnectPermission()) return false
        cancelDiscovery()
        val device = runCatching { bluetoothAdapter?.getRemoteDevice(address) }.getOrNull() ?: return false
        return if (safeBondState(device) == BluetoothDevice.BOND_BONDED) {
            connectToDevice(address)
            true
        } else {
            device.createBond()
        }
    }

    // =====================================================================
    // Real HID Input Dispatch Methods (Mouse, Keyboard, Media)
    // =====================================================================

    @SuppressLint("MissingPermission")
    fun sendMouseReport(
        buttonsMask: Int,
        dx: Int,
        dy: Int,
        wheel: Int = 0,
        pan: Int = 0
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || !hasConnectPermission()) return false
        val host = activeHostDevice ?: return false
        val proxy = hidDeviceProxy ?: return false
        val report = HidReportDescriptor.buildMouseReport(buttonsMask, dx, dy, wheel, pan)
        return proxy.sendReport(host, HidReportDescriptor.REPORT_ID_MOUSE, report)
    }

    @SuppressLint("MissingPermission")
    fun sendKeyPressAndRelease(modifierMask: Int, keyCode: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || !hasConnectPermission()) return false
        val host = activeHostDevice ?: return false
        val proxy = hidDeviceProxy ?: return false
        val down = HidReportDescriptor.buildKeyboardReport(modifierMask, listOf(keyCode))
        val up = HidReportDescriptor.buildKeyboardReport(0, emptyList())
        val okDown = proxy.sendReport(host, HidReportDescriptor.REPORT_ID_KEYBOARD, down)
        val okUp = proxy.sendReport(host, HidReportDescriptor.REPORT_ID_KEYBOARD, up)
        return okDown && okUp
    }

    @SuppressLint("MissingPermission")
    fun sendKeyState(modifierMask: Int, keyCodes: List<Int>): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || !hasConnectPermission()) return false
        val host = activeHostDevice ?: return false
        val proxy = hidDeviceProxy ?: return false
        val report = HidReportDescriptor.buildKeyboardReport(modifierMask, keyCodes)
        return proxy.sendReport(host, HidReportDescriptor.REPORT_ID_KEYBOARD, report)
    }

    suspend fun typeText(text: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || !hasConnectPermission()) return false
        if (activeHostDevice == null || hidDeviceProxy == null) return false
        for (ch in text) {
            val stroke = HidKeyCodes.charToHidKey(ch) ?: continue
            sendKeyPressAndRelease(stroke.modifierMask, stroke.keyCode)
            delay(10L)
        }
        return true
    }

    @SuppressLint("MissingPermission")
    fun sendConsumerKey(usageCode: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || !hasConnectPermission()) return false
        val host = activeHostDevice ?: return false
        val proxy = hidDeviceProxy ?: return false
        val down = HidReportDescriptor.buildConsumerReport(usageCode)
        val up = HidReportDescriptor.buildConsumerReport(HidKeyCodes.CONSUMER_NONE)
        val okDown = proxy.sendReport(host, HidReportDescriptor.REPORT_ID_CONSUMER, down)
        val okUp = proxy.sendReport(host, HidReportDescriptor.REPORT_ID_CONSUMER, up)
        return okDown && okUp
    }

    private fun stopForegroundServiceSafe() {
        runCatching { HidConnectionService.stop(context) }
    }

    @SuppressLint("MissingPermission")
    private fun safeDeviceName(device: BluetoothDevice): String? {
        if (!hasConnectPermission()) return null
        return runCatching { device.name }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    @SuppressLint("MissingPermission")
    private fun safeBondState(device: BluetoothDevice): Int {
        if (!hasConnectPermission()) return BluetoothDevice.BOND_NONE
        return runCatching { device.bondState }.getOrDefault(BluetoothDevice.BOND_NONE)
    }

    private fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            val btAdmin = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) ==
                PackageManager.PERMISSION_GRANTED
            val loc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
            } else true
            btAdmin && loc
        }
    }

    companion object {
        internal fun isLikelyComputerDevice(name: String, majorDeviceClass: Int): Boolean {
            return majorDeviceClass == BluetoothClass.Device.Major.COMPUTER ||
                name.contains("HP", ignoreCase = true) ||
                name.contains("ProBook", ignoreCase = true) ||
                name.contains("PC", ignoreCase = true) ||
                name.contains("DESKTOP", ignoreCase = true) ||
                name.contains("LAPTOP", ignoreCase = true) ||
                name.contains("WIN", ignoreCase = true)
        }

        internal fun isDeviceAutoConnectEnabled(
            address: String,
            isLikelyComputer: Boolean,
            lastConnectedAddress: String?,
            autoConnectAddresses: Set<String>,
            disabledAutoConnectAddresses: Set<String>
        ): Boolean {
            if (address in disabledAutoConnectAddresses) return false
            return (address in autoConnectAddresses) ||
                (address == lastConnectedAddress) ||
                isLikelyComputer
        }

        internal fun selectAutoConnectCandidates(
            pairedDevices: List<PairedPcDevice>,
            lastConnectedAddress: String?,
            suppressedAddresses: Set<String>
        ): List<PairedPcDevice> {
            return pairedDevices
                .filter { it.autoConnectEnabled && it.address !in suppressedAddresses }
                .sortedWith(
                    compareByDescending<PairedPcDevice> { it.isBluetoothAclConnected }
                        .thenByDescending { it.address == lastConnectedAddress }
                        .thenByDescending { it.isLikelyComputer }
                )
        }
    }
}
