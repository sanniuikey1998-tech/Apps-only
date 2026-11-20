package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AirMouseSensorController
import com.example.data.BluetoothHidController
import com.example.data.PowerShellScriptHttpServer
import com.example.data.PreferencesRepository
import com.example.data.SystemTelemetryRepository
import com.example.data.WifiDiscoveryRepository
import com.example.data.WifiRemoteClient
import com.example.domain.hid.CustomShortcut
import com.example.domain.hid.FourFingerAction
import com.example.domain.hid.HidKeyCodes
import com.example.domain.hid.MacroActionStep
import com.example.domain.hid.MultiStepMacro
import com.example.domain.hid.TouchpadMath
import com.example.domain.hid.WindowsShortcut
import com.example.domain.hid.WindowsShortcutsCatalog
import com.example.domain.hid.WindowsVirtualKeyMapper
import com.example.domain.model.AppPreferences
import com.example.domain.model.BatteryStatusInfo
import com.example.domain.model.DiscoveredBtDevice
import com.example.domain.model.DiscoveredWifiPc
import com.example.domain.model.FileTransferState
import com.example.domain.model.HidConnectionPhase
import com.example.domain.model.HidStatusState
import com.example.domain.model.PairedPcDevice
import com.example.domain.model.PermissionSummary
import com.example.domain.model.PresentationTimerState
import com.example.domain.model.RemoteTransportMode
import com.example.domain.model.ScriptServerStatus
import com.example.domain.model.WifiConnectionDirection
import com.example.domain.model.WifiConnectionPhase
import com.example.domain.model.WifiRemoteStatus
import com.example.domain.model.WifiStatusInfo
import com.example.service.DeckLinkQuickTileService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeckLinkViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = PreferencesRepository(application)
    private val telemetryRepo = SystemTelemetryRepository(application)
    private val wifiRepo = WifiDiscoveryRepository(application)
    private val wifiRemoteClient = WifiRemoteClient()
    private val scriptHttpServer = PowerShellScriptHttpServer()
    private val hidController = BluetoothHidController(application, prefsRepo)
    private val touchpadMath = TouchpadMath()
    private val airMouseController = AirMouseSensorController(application) { dx, dy ->
        movePointer(dx, dy)
    }

    val hidStatus: StateFlow<HidStatusState> = hidController.hidStatus
    val wifiRemoteStatus: StateFlow<WifiRemoteStatus> = wifiRemoteClient.status
    val scriptServerStatus: StateFlow<ScriptServerStatus> = scriptHttpServer.status
    val fileTransferState: StateFlow<FileTransferState> = wifiRemoteClient.fileTransferState
    val pairedDevices: StateFlow<List<PairedPcDevice>> = hidController.pairedDevices
    val discoveredBtDevices: StateFlow<List<DiscoveredBtDevice>> = hidController.discoveredDevices
    val isBtDiscovering: StateFlow<Boolean> = hidController.isDiscovering

    val discoveredWifiPcs: StateFlow<List<DiscoveredWifiPc>> = wifiRepo.discoveredPcs
    val isWifiScanning: StateFlow<Boolean> = wifiRepo.isScanning

    val wifiStatus: StateFlow<WifiStatusInfo> = wifiRepo.observeWifiStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WifiStatusInfo())

    val batteryStatus: StateFlow<BatteryStatusInfo> = telemetryRepo.observeBatteryStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BatteryStatusInfo())

    val preferences: StateFlow<AppPreferences> = prefsRepo.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferences())

    private val _permissions = MutableStateFlow(telemetryRepo.checkPermissions())
    val permissions: StateFlow<PermissionSummary> = _permissions.asStateFlow()

    private val _latchedModifiers = MutableStateFlow(0)
    val latchedModifiers: StateFlow<Int> = _latchedModifiers.asStateFlow()

    private val _lockedModifiers = MutableStateFlow(0)
    val lockedModifiers: StateFlow<Int> = _lockedModifiers.asStateFlow()

    private val _dragLockEnabled = MutableStateFlow(false)
    val dragLockEnabled: StateFlow<Boolean> = _dragLockEnabled.asStateFlow()

    private val _laserPointerActive = MutableStateFlow(false)
    val laserPointerActive: StateFlow<Boolean> = _laserPointerActive.asStateFlow()

    private val _pendingDangerousShortcut = MutableStateFlow<WindowsShortcut?>(null)
    val pendingDangerousShortcut: StateFlow<WindowsShortcut?> = _pendingDangerousShortcut.asStateFlow()

    private val _airMouseActive = MutableStateFlow(false)
    val airMouseActive: StateFlow<Boolean> = _airMouseActive.asStateFlow()

    private val _presentationTimer = MutableStateFlow(PresentationTimerState())
    val presentationTimer: StateFlow<PresentationTimerState> = _presentationTimer.asStateFlow()

    private val _feedback = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val feedback: SharedFlow<String> = _feedback.asSharedFlow()

    private val _timerAlertPulse = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val timerAlertPulse: SharedFlow<String> = _timerAlertPulse.asSharedFlow()

    val effectiveModifierMask: StateFlow<Int> = combine(_latchedModifiers, _lockedModifiers) { l, lock ->
        l or lock
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isHidApiSupported: Boolean = telemetryRepo.isBluetoothHidApiSupported()
    val isAirMouseSupported: Boolean = airMouseController.isAvailable

    private var activeMouseButtonsMask: Int = 0
    private var presentationTimerJob: Job? = null
    private var currentActiveRoute: String = DeckLinkRoutes.HOME
    private var isPresentationTabActive: Boolean = false

    init {
        hidController.start()
        viewModelScope.launch {
            preferences.collect { prefs ->
                airMouseController.setSensitivity(prefs.airMouseSensitivity)
                hidController.updateSettings(
                    autoReconnect = prefs.autoReconnect,
                    autoConnectConnected = prefs.autoConnectConnectedDevices,
                    timeoutSec = prefs.connectionTimeoutSec,
                    foregroundService = prefs.foregroundServiceEnabled,
                    lastAddress = prefs.lastConnectedAddress,
                    pinnedAutoConnectAddresses = prefs.autoConnectAddresses,
                    excludedAutoConnectAddresses = prefs.disabledAutoConnectAddresses
                )
            }
        }
        viewModelScope.launch {
            combine(hidStatus, wifiRemoteStatus) { bt, wifi ->
                bt.phase == HidConnectionPhase.CONNECTED || wifi.phase == WifiConnectionPhase.CONNECTED
            }.collect { connected ->
                DeckLinkQuickTileService.lastKnownConnected = connected
            }
        }
        viewModelScope.launch {
            wifiRemoteClient.incomingClipboard.collect { pcText ->
                val cm = getApplication<Application>()
                    .getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                cm?.setPrimaryClip(ClipData.newPlainText("PC Clipboard", pcText))
                val preview = pcText.replace("\n", " ").take(42)
                _feedback.tryEmit(
                    if (pcText.isBlank()) "PC clipboard is empty"
                    else "Pulled from PC clipboard: \"$preview\""
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        presentationTimerJob?.cancel()
        airMouseController.stop()
        scriptHttpServer.stop()
        hidController.stop()
        wifiRemoteClient.disconnect()
    }

    fun requiredRuntimePermissions(): Array<String> = telemetryRepo.requiredRuntimePermissions()

    fun refreshPermissionsAndHid() {
        _permissions.value = telemetryRepo.checkPermissions()
        hidController.initializeHidProfile()
        hidController.refreshPairedDevices()
    }

    // =====================================================
    // Connection Controls (Bluetooth HID & Local Wi-Fi Mode)
    // =====================================================

    fun connectOrQuickConnect() {
        val prefs = preferences.value
        val btConnected = hidStatus.value.phase == HidConnectionPhase.CONNECTED
        val wifiConnected = wifiRemoteStatus.value.phase == WifiConnectionPhase.CONNECTED

        if (prefs.preferredTransport == RemoteTransportMode.WIFI_POWERSHELL) {
            if (wifiConnected || wifiRemoteStatus.value.phase == WifiConnectionPhase.LISTENING) {
                wifiRemoteClient.disconnect()
                return
            }
            if (prefs.wifiDirection == WifiConnectionDirection.PC_TO_PHONE_HOTSPOT) {
                startWifiHotspotListener(prefs.wifiPort, prefs.wifiPin)
            } else if (prefs.wifiTargetIp.isNotBlank()) {
                connectWifiRemote(prefs.wifiTargetIp, prefs.wifiPort, prefs.wifiPin)
            } else {
                _feedback.tryEmit("Set your Windows PC's LAN IP & run the PowerShell receiver in Devices.")
            }
            return
        }

        if (btConnected) {
            hidController.disconnect()
            return
        }
        val lastAddr = prefs.lastConnectedAddress
        val paired = pairedDevices.value
        val target = paired.firstOrNull { it.address == lastAddr }
            ?: paired.firstOrNull { it.isLikelyComputer }
            ?: paired.firstOrNull()

        if (target != null) {
            hidController.connectToDevice(target.address)
        } else {
            _feedback.tryEmit("No paired PC found. Open Settings or Pair New PC to connect.")
        }
    }

    fun connectToDevice(address: String) = hidController.connectToDevice(address)

    fun disconnect() = hidController.disconnect()

    fun startBtDiscovery() {
        if (!hidController.startDiscovery()) {
            _feedback.tryEmit("Enable Bluetooth and grant Scan permission to discover PCs.")
        }
    }

    fun stopBtDiscovery() = hidController.cancelDiscovery()

    fun pairBtDevice(address: String) {
        if (!hidController.pairDevice(address)) {
            _feedback.tryEmit("Unable to initiate pairing. Pair directly from Windows Bluetooth Settings.")
        }
    }

    fun triggerAutoConnectNow() = hidController.triggerAutoConnectIfIdle()

    // =====================================================
    // Wi-Fi Zero-Install Discovery, Receiver & Wake-on-LAN
    // =====================================================

    fun setPreferredTransport(mode: RemoteTransportMode) {
        viewModelScope.launch {
            prefsRepo.updatePreferredTransport(mode)
        }
    }

    fun setWifiDirection(direction: WifiConnectionDirection) {
        viewModelScope.launch {
            prefsRepo.updateWifiDirection(direction)
        }
    }

    fun saveWifiRemoteConfig(ip: String, port: Int, pin: String) {
        viewModelScope.launch {
            prefsRepo.updateWifiRemoteConfig(ip, port, pin)
        }
    }

    fun regenerateWifiPin() {
        val newPin = (100000..999999).random().toString()
        val prefs = preferences.value
        viewModelScope.launch {
            prefsRepo.updateWifiRemoteConfig(prefs.wifiTargetIp, prefs.wifiPort, newPin)
            _feedback.tryEmit("Generated new 6-digit Wi-Fi pairing PIN: $newPin")
        }
        if (wifiRemoteStatus.value.phase == WifiConnectionPhase.LISTENING) {
            startWifiHotspotListener(prefs.wifiPort, newPin)
        }
    }

    fun startWifiHotspotListener(port: Int, pin: String) {
        val cleanPort = port.coerceIn(1024, 65535)
        val cleanPin = pin.trim().ifBlank { preferences.value.wifiPin }
        viewModelScope.launch {
            prefsRepo.updateWifiRemoteConfig(preferences.value.wifiTargetIp, cleanPort, cleanPin)
            prefsRepo.updateWifiDirection(WifiConnectionDirection.PC_TO_PHONE_HOTSPOT)
            prefsRepo.updatePreferredTransport(RemoteTransportMode.WIFI_POWERSHELL)
        }
        wifiRemoteClient.startHotspotServer(
            port = cleanPort,
            pin = cleanPin,
            hotspotIpHint = wifiStatus.value.hotspotIpv4
        )
    }

    fun connectWifiRemote(ip: String, port: Int, pin: String) {
        val cleanIp = ip.trim()
        val cleanPort = port.coerceIn(1024, 65535)
        val cleanPin = pin.trim().ifBlank { preferences.value.wifiPin }
        viewModelScope.launch {
            prefsRepo.updateWifiRemoteConfig(cleanIp, cleanPort, cleanPin)
            prefsRepo.updateWifiDirection(WifiConnectionDirection.PHONE_TO_PC)
            prefsRepo.updatePreferredTransport(RemoteTransportMode.WIFI_POWERSHELL)
        }
        wifiRemoteClient.connect(cleanIp, cleanPort, cleanPin)
    }

    fun disconnectWifiRemote() {
        wifiRemoteClient.disconnect()
    }

    fun scanWifiLan() {
        val ip = wifiStatus.value.effectiveLocalIpv4
        if (!wifiStatus.value.hasLocalNetwork || ip.isNullOrBlank()) {
            _feedback.tryEmit("Connect to Wi-Fi or enable Android Hotspot first to scan local Windows PCs.")
            return
        }
        viewModelScope.launch {
            val found = wifiRepo.scanLocalNetworkForWindowsPcs(ip)
            _feedback.tryEmit(
                if (found.isEmpty()) "No Windows LAN services (SMB/RDP/WS-Discovery) answered on $ip/24"
                else "Discovered ${found.size} Windows host(s) on local network"
            )
        }
    }

    fun sendWakeOnLan(macAddress: String) {
        viewModelScope.launch {
            val result = wifiRepo.sendWakeOnLan(macAddress, wifiStatus.value.effectiveSubnetPrefix)
            _feedback.tryEmit(
                result.getOrElse { it.message ?: "Failed to send Wake-on-LAN packet" }
            )
        }
    }

    // =====================================================
    // One-Line Script Server, Clipboard Sync & Quick File Drop
    // =====================================================

    fun startOneLineScriptServer(port: Int, pin: String) {
        val prefs = preferences.value
        val cleanPort = port.coerceIn(1024, 65534)
        val cleanPin = pin.trim().ifBlank { prefs.wifiPin }
        val isHotspot = prefs.wifiDirection == WifiConnectionDirection.PC_TO_PHONE_HOTSPOT
        val displayIp = if (isHotspot) {
            wifiStatus.value.hotspotIpv4 ?: wifiStatus.value.localIpv4
        } else {
            wifiStatus.value.localIpv4 ?: wifiStatus.value.hotspotIpv4
        }
        val script = if (isHotspot) {
            WindowsVirtualKeyMapper.buildPowerShellHotspotClientScript(cleanPort, cleanPin, displayIp)
        } else {
            WindowsVirtualKeyMapper.buildPowerShellReceiverScript(cleanPort, cleanPin)
        }
        val httpPort = cleanPort + 1
        if (isHotspot && wifiRemoteStatus.value.phase != WifiConnectionPhase.CONNECTED) {
            startWifiHotspotListener(cleanPort, cleanPin)
        }
        scriptHttpServer.startServing(
            scriptText = script,
            port = httpPort,
            displayIpHint = displayIp,
            durationSeconds = 60
        )
    }

    fun stopOneLineScriptServer() {
        scriptHttpServer.stop("One-line setup server stopped")
    }

    fun pushPhoneClipboardToPc() {
        val cm = getApplication<Application>()
            .getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clipText = cm?.primaryClip?.getItemAt(0)?.coerceToText(getApplication())?.toString().orEmpty()
        if (clipText.isEmpty()) {
            _feedback.tryEmit("Your phone clipboard is empty.")
            return
        }
        if (wifiRemoteStatus.value.phase == WifiConnectionPhase.CONNECTED) {
            if (wifiRemoteClient.pushClipboardToPc(clipText)) {
                _feedback.tryEmit("Synced phone clipboard to Windows PC")
            }
        } else if (hidStatus.value.phase == HidConnectionPhase.CONNECTED) {
            viewModelScope.launch {
                hidController.typeText(clipText)
                _feedback.tryEmit("Typed phone clipboard via Bluetooth HID")
            }
        } else {
            _feedback.tryEmit("Connect via Wi-Fi / Hotspot or Bluetooth HID first.")
        }
    }

    fun pullPcClipboardToPhone() {
        if (wifiRemoteStatus.value.phase != WifiConnectionPhase.CONNECTED) {
            _feedback.tryEmit("Two-way clipboard pull requires Local Wi-Fi or Hotspot PowerShell mode.")
            return
        }
        wifiRemoteClient.pullClipboardFromPc()
    }

    fun sendFileUriToPc(uri: Uri) {
        if (wifiRemoteStatus.value.phase != WifiConnectionPhase.CONNECTED) {
            _feedback.tryEmit("Connect over Local Wi-Fi or Phone Hotspot first to drop files.")
            return
        }
        viewModelScope.launch {
            val resolver = getApplication<Application>().contentResolver
            val filePayload = withContext(Dispatchers.IO) {
                runCatching {
                    var name = "decklink_drop_${System.currentTimeMillis()}"
                    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (idx >= 0) {
                                cursor.getString(idx)?.takeIf { it.isNotBlank() }?.let { name = it }
                            }
                        }
                    }
                    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: error("Could not read selected file")
                    name to bytes
                }
            }
            filePayload.onFailure { err ->
                _feedback.tryEmit(err.message ?: "Unable to open selected file")
            }.onSuccess { (name, bytes) ->
                val result = wifiRemoteClient.sendFileToPc(name, bytes)
                _feedback.tryEmit(result.getOrElse { it.message ?: "File drop failed" })
            }
        }
    }

    // =====================================================
    // Air Mouse, Pinch-to-Zoom & Hardware Volume Keys
    // =====================================================

    fun toggleAirMouse() {
        setAirMouseEnabled(!_airMouseActive.value)
    }

    fun setAirMouseEnabled(enabled: Boolean) {
        if (enabled) {
            if (!isAirMouseSupported) {
                _feedback.tryEmit("Gyroscope sensor is unavailable on this device.")
                return
            }
            if (airMouseController.start()) {
                _airMouseActive.value = true
            }
        } else {
            airMouseController.stop()
            _airMouseActive.value = false
        }
    }

    fun pinchZoom(wheelSteps: Int) {
        if (wheelSteps == 0 || !requireConnectedSilently()) return
        dispatchKeyState(HidKeyCodes.MOD_LEFT_CTRL, emptyList())
        dispatchMouseReport(
            buttonsMask = effectiveButtonsMask(),
            dx = 0,
            dy = 0,
            wheel = wheelSteps.coerceIn(-4, 4),
            pan = 0
        )
        dispatchKeyState(0, emptyList())
    }

    fun updateActiveScreenContext(route: String, presentationTabActive: Boolean) {
        currentActiveRoute = route
        isPresentationTabActive = presentationTabActive
    }

    fun handleHardwareVolumeKey(isVolumeUp: Boolean): Boolean {
        if (!preferences.value.volumeButtonsControl) return false
        if (activeTransport() == null) return false
        return when {
            currentActiveRoute == DeckLinkRoutes.MEDIA && isPresentationTabActive -> {
                sendPresentationAction(if (isVolumeUp) "next" else "prev")
                true
            }
            currentActiveRoute == DeckLinkRoutes.MEDIA -> {
                sendMediaConsumerKey(
                    if (isVolumeUp) HidKeyCodes.CONSUMER_VOL_UP else HidKeyCodes.CONSUMER_VOL_DOWN
                )
                true
            }
            currentActiveRoute == DeckLinkRoutes.MOUSE -> {
                clickMouseButton(if (isVolumeUp) HidKeyCodes.MOUSE_LEFT else HidKeyCodes.MOUSE_RIGHT)
                true
            }
            else -> false
        }
    }

    fun handleExternalQuickAction(action: String?) {
        when (action) {
            DeckLinkQuickTileService.QUICK_ACTION_TOGGLE_CONNECT -> connectOrQuickConnect()
            DeckLinkQuickTileService.QUICK_ACTION_HOTSPOT -> {
                val prefs = preferences.value
                startWifiHotspotListener(prefs.wifiPort, prefs.wifiPin)
            }
            DeckLinkQuickTileService.QUICK_ACTION_PLAY_PAUSE ->
                sendMediaConsumerKey(HidKeyCodes.CONSUMER_PLAY_PAUSE)
            DeckLinkQuickTileService.QUICK_ACTION_MUTE ->
                sendMediaConsumerKey(HidKeyCodes.CONSUMER_MUTE)
            DeckLinkQuickTileService.QUICK_ACTION_LOCK_PC -> {
                val lockShortcut = WindowsShortcutsCatalog.builtInShortcuts.firstOrNull { it.id == "win_l" }
                if (lockShortcut != null) triggerWindowsShortcut(lockShortcut)
            }
        }
    }

    // =====================================================
    // Mouse & Touchpad Actions
    // =====================================================

    fun resetTouchpadAccumulators() = touchpadMath.reset()

    fun movePointer(rawDx: Float, rawDy: Float) {
        if (!requireConnectedSilently()) return
        val prefs = preferences.value
        val delta = touchpadMath.computePointerDelta(
            rawDx = rawDx,
            rawDy = rawDy,
            pointerSpeed = prefs.pointerSpeed,
            sensitivity = prefs.mouseSensitivity
        )
        if (delta.dx != 0 || delta.dy != 0) {
            val buttons = effectiveButtonsMask()
            dispatchMouseReport(buttons, delta.dx, delta.dy)
        }
    }

    fun scrollTwoFinger(rawDx: Float, rawDy: Float) {
        if (!requireConnectedSilently()) return
        val prefs = preferences.value
        val scroll = touchpadMath.computeScrollDelta(
            rawDx = rawDx,
            rawDy = rawDy,
            naturalScrolling = prefs.naturalScrolling,
            sensitivity = prefs.mouseSensitivity
        )
        if (scroll.wheel != 0 || scroll.pan != 0) {
            dispatchMouseReport(
                buttonsMask = effectiveButtonsMask(),
                dx = 0,
                dy = 0,
                wheel = scroll.wheel,
                pan = scroll.pan
            )
        }
    }

    fun clickMouseButton(buttonMask: Int) {
        if (!requireConnectedWithToast()) return
        if (buttonMask == HidKeyCodes.MOUSE_LEFT && _dragLockEnabled.value) {
            _dragLockEnabled.value = false
            dispatchMouseReport(HidKeyCodes.MOUSE_NONE, 0, 0)
            return
        }
        dispatchMouseReport(buttonMask, 0, 0)
        dispatchMouseReport(effectiveButtonsMask(), 0, 0)
    }

    fun doubleClickLeft() {
        if (!requireConnectedWithToast()) return
        viewModelScope.launch {
            dispatchMouseReport(HidKeyCodes.MOUSE_LEFT, 0, 0)
            dispatchMouseReport(HidKeyCodes.MOUSE_NONE, 0, 0)
            kotlinx.coroutines.delay(55)
            dispatchMouseReport(HidKeyCodes.MOUSE_LEFT, 0, 0)
            dispatchMouseReport(HidKeyCodes.MOUSE_NONE, 0, 0)
        }
    }

    fun setMouseButtonHeld(buttonMask: Int, held: Boolean) {
        if (!requireConnectedSilently()) return
        activeMouseButtonsMask = if (held) {
            activeMouseButtonsMask or buttonMask
        } else {
            activeMouseButtonsMask and buttonMask.inv()
        }
        dispatchMouseReport(effectiveButtonsMask(), 0, 0)
    }

    fun toggleDragLock() {
        if (!requireConnectedWithToast()) return
        val next = !_dragLockEnabled.value
        _dragLockEnabled.value = next
        dispatchMouseReport(effectiveButtonsMask(), 0, 0)
    }

    fun setDragMode(active: Boolean) {
        if (!requireConnectedSilently()) return
        _dragLockEnabled.value = active
        dispatchMouseReport(effectiveButtonsMask(), 0, 0)
    }

    fun executeThreeFingerGesture(direction: String) {
        if (!requireConnectedWithToast()) return
        when (direction) {
            "up" -> dispatchKeyPressAndRelease(HidKeyCodes.MOD_LEFT_GUI, HidKeyCodes.KEY_TAB)
            "down" -> dispatchKeyPressAndRelease(HidKeyCodes.MOD_LEFT_GUI, HidKeyCodes.KEY_D)
            "left" -> dispatchKeyPressAndRelease(HidKeyCodes.MOD_LEFT_ALT, HidKeyCodes.KEY_TAB)
            "right" -> dispatchKeyPressAndRelease(
                HidKeyCodes.MOD_LEFT_ALT or HidKeyCodes.MOD_LEFT_SHIFT,
                HidKeyCodes.KEY_TAB
            )
            "tap" -> clickMouseButton(HidKeyCodes.MOUSE_MIDDLE)
        }
    }

    fun executeFourFingerGesture(direction: String) {
        if (!requireConnectedWithToast()) return
        val prefs = preferences.value
        val action = when (direction) {
            "up" -> prefs.fourFingerUp
            "down" -> prefs.fourFingerDown
            "left" -> prefs.fourFingerLeft
            "right" -> prefs.fourFingerRight
            "tap" -> prefs.fourFingerTap
            else -> FourFingerAction.DISABLED
        }
        when (action) {
            FourFingerAction.SWITCH_DESKTOP_NEXT -> dispatchKeyPressAndRelease(
                HidKeyCodes.MOD_LEFT_CTRL or HidKeyCodes.MOD_LEFT_GUI,
                HidKeyCodes.KEY_RIGHT
            )
            FourFingerAction.SWITCH_DESKTOP_PREV -> dispatchKeyPressAndRelease(
                HidKeyCodes.MOD_LEFT_CTRL or HidKeyCodes.MOD_LEFT_GUI,
                HidKeyCodes.KEY_LEFT
            )
            FourFingerAction.TASK_VIEW -> dispatchKeyPressAndRelease(
                HidKeyCodes.MOD_LEFT_GUI,
                HidKeyCodes.KEY_TAB
            )
            FourFingerAction.SHOW_DESKTOP -> dispatchKeyPressAndRelease(
                HidKeyCodes.MOD_LEFT_GUI,
                HidKeyCodes.KEY_D
            )
            FourFingerAction.SNIP_SCREEN -> dispatchKeyPressAndRelease(
                HidKeyCodes.MOD_LEFT_GUI or HidKeyCodes.MOD_LEFT_SHIFT,
                HidKeyCodes.KEY_S
            )
            FourFingerAction.TASK_MANAGER -> dispatchKeyPressAndRelease(
                HidKeyCodes.MOD_LEFT_CTRL or HidKeyCodes.MOD_LEFT_SHIFT,
                HidKeyCodes.KEY_ESC
            )
            FourFingerAction.PLAY_PAUSE -> dispatchConsumerKey(HidKeyCodes.CONSUMER_PLAY_PAUSE)
            FourFingerAction.LOCK_PC -> dispatchKeyPressAndRelease(
                HidKeyCodes.MOD_LEFT_GUI,
                HidKeyCodes.KEY_L
            )
            FourFingerAction.DISABLED -> Unit
        }
    }

    private fun effectiveButtonsMask(): Int {
        var mask = activeMouseButtonsMask
        if (_dragLockEnabled.value || _laserPointerActive.value) {
            mask = mask or HidKeyCodes.MOUSE_LEFT
        }
        return mask
    }

    // =====================================================
    // Keyboard & Text Input Actions
    // =====================================================

    fun toggleModifierLatch(modBit: Int) {
        val isLocked = (_lockedModifiers.value and modBit) != 0
        val isLatched = (_latchedModifiers.value and modBit) != 0
        when {
            isLocked -> {
                _lockedModifiers.value = _lockedModifiers.value and modBit.inv()
                _latchedModifiers.value = _latchedModifiers.value and modBit.inv()
            }
            isLatched -> {
                // Second tap locks modifier
                _latchedModifiers.value = _latchedModifiers.value and modBit.inv()
                _lockedModifiers.value = _lockedModifiers.value or modBit
            }
            else -> {
                _latchedModifiers.value = _latchedModifiers.value or modBit
            }
        }
    }

    fun clearModifiers() {
        _latchedModifiers.value = 0
        _lockedModifiers.value = 0
    }

    fun sendKeyboardKey(keyCode: Int, extraModifiers: Int = 0) {
        if (!requireConnectedWithToast()) return
        val combinedMod = effectiveModifierMask.value or extraModifiers
        dispatchKeyPressAndRelease(combinedMod, keyCode)
        // Clear latched (single-shot) modifiers, keep locked ones
        _latchedModifiers.value = 0
    }

    fun sendCharFromTextBar(ch: Char) {
        if (!requireConnectedWithToast()) return
        val stroke = HidKeyCodes.charToHidKey(ch) ?: return
        dispatchKeyPressAndRelease(
            stroke.modifierMask or effectiveModifierMask.value,
            stroke.keyCode
        )
        _latchedModifiers.value = 0
    }

    fun sendTextString(text: String) {
        if (text.isEmpty()) return
        if (!requireConnectedWithToast()) return
        viewModelScope.launch {
            dispatchTypeText(text)
        }
    }

    // =====================================================
    // Media & Presentation Actions
    // =====================================================

    fun sendMediaConsumerKey(usageCode: Int) {
        if (!requireConnectedWithToast()) return
        dispatchConsumerKey(usageCode)
    }

    fun sendPresentationAction(action: String) {
        when (action) {
            "next" -> _presentationTimer.update { it.copy(slideNumber = it.slideNumber + 1) }
            "prev" -> _presentationTimer.update { it.copy(slideNumber = (it.slideNumber - 1).coerceAtLeast(1)) }
            "start_beginning" -> _presentationTimer.update { it.copy(slideNumber = 1) }
        }
        if (!requireConnectedWithToast()) return
        when (action) {
            "next" -> dispatchKeyPressAndRelease(0, HidKeyCodes.KEY_RIGHT)
            "prev" -> dispatchKeyPressAndRelease(0, HidKeyCodes.KEY_LEFT)
            "start_beginning" -> dispatchKeyPressAndRelease(0, HidKeyCodes.KEY_F5)
            "start_current" -> dispatchKeyPressAndRelease(HidKeyCodes.MOD_LEFT_SHIFT, HidKeyCodes.KEY_F5)
            "exit" -> dispatchKeyPressAndRelease(0, HidKeyCodes.KEY_ESC)
            "black_screen" -> dispatchKeyPressAndRelease(0, HidKeyCodes.KEY_B)
            "fullscreen_f11" -> dispatchKeyPressAndRelease(0, HidKeyCodes.KEY_F11)
            "fullscreen_f" -> dispatchKeyPressAndRelease(0, HidKeyCodes.KEY_F)
            "seek_back" -> dispatchKeyPressAndRelease(0, HidKeyCodes.KEY_J)
            "seek_forward" -> dispatchKeyPressAndRelease(0, HidKeyCodes.KEY_L)
            "captions" -> dispatchKeyPressAndRelease(0, HidKeyCodes.KEY_C)
        }
    }

    fun setPresentationTimerMinutes(minutes: Int) {
        val sec = (minutes.coerceIn(1, 120)) * 60
        presentationTimerJob?.cancel()
        _presentationTimer.update {
            it.copy(
                targetDurationSec = sec,
                elapsedSec = 0,
                isRunning = false,
                lastMilestoneAlert = null
            )
        }
    }

    fun togglePresentationTimer() {
        val currentlyRunning = _presentationTimer.value.isRunning
        if (currentlyRunning) {
            presentationTimerJob?.cancel()
            _presentationTimer.update { it.copy(isRunning = false) }
        } else {
            _presentationTimer.update { it.copy(isRunning = true) }
            presentationTimerJob?.cancel()
            presentationTimerJob = viewModelScope.launch {
                while (isActive) {
                    delay(1000L)
                    var milestone: String? = null
                    _presentationTimer.update { state ->
                        val nextElapsed = state.elapsedSec + 1
                        val rem = state.targetDurationSec - nextElapsed
                        milestone = when {
                            rem == 300 && state.targetDurationSec > 300 -> "5 minutes remaining"
                            rem == 60 && state.targetDurationSec > 60 -> "1 minute remaining"
                            rem == 0 -> "Time's up!"
                            else -> null
                        }
                        state.copy(
                            elapsedSec = nextElapsed,
                            lastMilestoneAlert = milestone ?: state.lastMilestoneAlert
                        )
                    }
                    milestone?.let { msg ->
                        _timerAlertPulse.tryEmit(msg)
                        _feedback.tryEmit("Presentation Timer: $msg")
                    }
                }
            }
        }
    }

    fun resetPresentationTimer() {
        presentationTimerJob?.cancel()
        _presentationTimer.update {
            it.copy(elapsedSec = 0, isRunning = false, slideNumber = 1, lastMilestoneAlert = null)
        }
    }

    fun toggleLaserPointer() {
        if (!requireConnectedWithToast()) return
        val next = !_laserPointerActive.value
        _laserPointerActive.value = next
        if (next) {
            dispatchKeyState(HidKeyCodes.MOD_LEFT_CTRL, emptyList())
            dispatchMouseReport(effectiveButtonsMask(), 0, 0)
        } else {
            dispatchMouseReport(HidKeyCodes.MOUSE_NONE, 0, 0)
            dispatchKeyState(0, emptyList())
        }
    }

    // =====================================================
    // Windows Shortcuts & Custom Macros
    // =====================================================

    fun triggerWindowsShortcut(shortcut: WindowsShortcut) {
        if (!requireConnectedWithToast()) return
        if (shortcut.requiresSafetyConfirm && preferences.value.confirmDangerousShortcuts) {
            _pendingDangerousShortcut.value = shortcut
            return
        }
        dispatchKeyPressAndRelease(shortcut.modifierMask, shortcut.keyCode)
    }

    fun confirmPendingShortcut() {
        val pending = _pendingDangerousShortcut.value ?: return
        _pendingDangerousShortcut.value = null
        if (!requireConnectedWithToast()) return
        dispatchKeyPressAndRelease(pending.modifierMask, pending.keyCode)
    }

    fun dismissPendingShortcut() {
        _pendingDangerousShortcut.value = null
    }

    fun triggerMultiStepMacro(macro: MultiStepMacro) {
        if (!requireConnectedWithToast()) return
        viewModelScope.launch {
            for (step in macro.steps) {
                when (step) {
                    is MacroActionStep.KeyCombo -> dispatchKeyPressAndRelease(step.modifierMask, step.keyCode)
                    is MacroActionStep.TypeString -> dispatchTypeText(step.text)
                    is MacroActionStep.WaitMs -> delay(step.durationMs)
                }
            }
            _feedback.tryEmit("Ran macro: ${macro.title}")
        }
    }

    fun triggerCustomShortcut(shortcut: CustomShortcut) {
        if (!requireConnectedWithToast()) return
        val followUpText = shortcut.macroText
        if (followUpText.isNullOrBlank()) {
            dispatchKeyPressAndRelease(shortcut.modifierMask, shortcut.keyCode)
        } else {
            viewModelScope.launch {
                dispatchKeyPressAndRelease(shortcut.modifierMask, shortcut.keyCode)
                delay(300L)
                dispatchTypeText(followUpText)
                delay(80L)
                dispatchKeyPressAndRelease(0, HidKeyCodes.KEY_ENTER)
            }
        }
    }

    fun addCustomShortcut(
        title: String,
        modifierMask: Int,
        keyCode: Int,
        comboLabel: String,
        macroText: String? = null
    ) {
        val cleanTitle = title.trim().ifBlank { comboLabel }
        val cleanMacro = macroText?.trim()?.takeIf { it.isNotBlank() }
        val item = CustomShortcut(
            id = "custom_${System.currentTimeMillis()}",
            title = cleanTitle,
            comboLabel = if (cleanMacro != null) "$comboLabel → $cleanMacro" else comboLabel,
            modifierMask = modifierMask,
            keyCode = keyCode,
            macroText = cleanMacro
        )
        viewModelScope.launch {
            prefsRepo.addCustomShortcut(item)
            _feedback.tryEmit("Saved shortcut \"$cleanTitle\"")
        }
    }

    fun deleteCustomShortcut(id: String) {
        viewModelScope.launch {
            prefsRepo.deleteCustomShortcut(id)
        }
    }

    // =====================================================
    // Preferences Updates
    // =====================================================

    fun setPointerSpeed(speed: Float) = viewModelScope.launch { prefsRepo.updatePointerSpeed(speed) }
    fun setMouseSensitivity(sens: Float) = viewModelScope.launch { prefsRepo.updateMouseSensitivity(sens) }
    fun setAirMouseSensitivity(sens: Float) = viewModelScope.launch { prefsRepo.updateAirMouseSensitivity(sens) }
    fun setVolumeButtonsControl(enabled: Boolean) = viewModelScope.launch { prefsRepo.updateVolumeButtonsControl(enabled) }
    fun setNaturalScrolling(enabled: Boolean) = viewModelScope.launch { prefsRepo.updateNaturalScrolling(enabled) }
    fun setHapticFeedback(enabled: Boolean) = viewModelScope.launch { prefsRepo.updateHapticFeedback(enabled) }
    fun setAutoReconnect(enabled: Boolean) = viewModelScope.launch { prefsRepo.updateAutoReconnect(enabled) }
    fun setAutoConnectConnectedDevices(enabled: Boolean) = viewModelScope.launch {
        prefsRepo.updateAutoConnectConnectedDevices(enabled)
    }
    fun setDeviceAutoConnect(address: String, enabled: Boolean) = viewModelScope.launch {
        prefsRepo.setDeviceAutoConnect(address, enabled)
        if (enabled && preferences.value.autoConnectConnectedDevices) {
            hidController.triggerAutoConnectIfIdle()
        }
    }
    fun setConnectionTimeout(sec: Int) = viewModelScope.launch { prefsRepo.updateConnectionTimeout(sec) }
    fun setForegroundServiceEnabled(enabled: Boolean) = viewModelScope.launch { prefsRepo.updateForegroundServiceEnabled(enabled) }
    fun setConfirmDangerousShortcuts(enabled: Boolean) = viewModelScope.launch { prefsRepo.updateConfirmDangerousShortcuts(enabled) }
    fun setFourFingerGesture(dir: String, action: FourFingerAction) = viewModelScope.launch {
        prefsRepo.updateFourFingerGesture(dir, action)
    }

    private fun activeTransport(): RemoteTransportMode? {
        val btConnected = hidStatus.value.phase == HidConnectionPhase.CONNECTED
        val wifiConnected = wifiRemoteStatus.value.phase == WifiConnectionPhase.CONNECTED
        return when {
            btConnected && wifiConnected -> preferences.value.preferredTransport
            btConnected -> RemoteTransportMode.BLUETOOTH_HID
            wifiConnected -> RemoteTransportMode.WIFI_POWERSHELL
            else -> null
        }
    }

    private fun dispatchMouseReport(
        buttonsMask: Int,
        dx: Int,
        dy: Int,
        wheel: Int = 0,
        pan: Int = 0
    ): Boolean {
        return when (activeTransport()) {
            RemoteTransportMode.BLUETOOTH_HID -> hidController.sendMouseReport(buttonsMask, dx, dy, wheel, pan)
            RemoteTransportMode.WIFI_POWERSHELL -> wifiRemoteClient.sendMouseReport(buttonsMask, dx, dy, wheel, pan)
            null -> false
        }
    }

    private fun dispatchKeyPressAndRelease(modifierMask: Int, keyCode: Int): Boolean {
        return when (activeTransport()) {
            RemoteTransportMode.BLUETOOTH_HID -> hidController.sendKeyPressAndRelease(modifierMask, keyCode)
            RemoteTransportMode.WIFI_POWERSHELL -> wifiRemoteClient.sendKeyPressAndRelease(modifierMask, keyCode)
            null -> false
        }
    }

    private fun dispatchKeyState(modifierMask: Int, keyCodes: List<Int>): Boolean {
        return when (activeTransport()) {
            RemoteTransportMode.BLUETOOTH_HID -> hidController.sendKeyState(modifierMask, keyCodes)
            RemoteTransportMode.WIFI_POWERSHELL -> wifiRemoteClient.sendKeyState(modifierMask, keyCodes)
            null -> false
        }
    }

    private suspend fun dispatchTypeText(text: String): Boolean {
        return when (activeTransport()) {
            RemoteTransportMode.BLUETOOTH_HID -> hidController.typeText(text)
            RemoteTransportMode.WIFI_POWERSHELL -> wifiRemoteClient.typeText(text)
            null -> false
        }
    }

    private fun dispatchConsumerKey(usageCode: Int): Boolean {
        return when (activeTransport()) {
            RemoteTransportMode.BLUETOOTH_HID -> hidController.sendConsumerKey(usageCode)
            RemoteTransportMode.WIFI_POWERSHELL -> wifiRemoteClient.sendConsumerKey(usageCode)
            null -> false
        }
    }

    private fun requireConnectedSilently(): Boolean {
        return activeTransport() != null
    }

    private fun requireConnectedWithToast(): Boolean {
        if (activeTransport() != null) return true
        val msg = if (preferences.value.preferredTransport == RemoteTransportMode.WIFI_POWERSHELL) {
            "Connect to your Windows PC via Local Wi-Fi Receiver or Bluetooth HID first."
        } else {
            "Connect to your Windows PC first via Bluetooth HID or Local Wi-Fi."
        }
        _feedback.tryEmit(msg)
        return false
    }
}
