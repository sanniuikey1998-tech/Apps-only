package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BluetoothHidController
import com.example.data.PreferencesRepository
import com.example.data.SystemTelemetryRepository
import com.example.data.WifiDiscoveryRepository
import com.example.domain.hid.CustomShortcut
import com.example.domain.hid.FourFingerAction
import com.example.domain.hid.HidKeyCodes
import com.example.domain.hid.TouchpadMath
import com.example.domain.hid.WindowsShortcut
import com.example.domain.model.AppPreferences
import com.example.domain.model.BatteryStatusInfo
import com.example.domain.model.DiscoveredBtDevice
import com.example.domain.model.DiscoveredWifiPc
import com.example.domain.model.HidConnectionPhase
import com.example.domain.model.HidStatusState
import com.example.domain.model.PairedPcDevice
import com.example.domain.model.PermissionSummary
import com.example.domain.model.WifiStatusInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DeckLinkViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = PreferencesRepository(application)
    private val telemetryRepo = SystemTelemetryRepository(application)
    private val wifiRepo = WifiDiscoveryRepository(application)
    private val hidController = BluetoothHidController(application, prefsRepo)
    private val touchpadMath = TouchpadMath()

    val hidStatus: StateFlow<HidStatusState> = hidController.hidStatus
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

    private val _feedback = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val feedback: SharedFlow<String> = _feedback.asSharedFlow()

    val effectiveModifierMask: StateFlow<Int> = combine(_latchedModifiers, _lockedModifiers) { l, lock ->
        l or lock
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isHidApiSupported: Boolean = telemetryRepo.isBluetoothHidApiSupported()

    private var activeMouseButtonsMask: Int = 0

    init {
        hidController.start()
        viewModelScope.launch {
            preferences.collect { prefs ->
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
    }

    override fun onCleared() {
        super.onCleared()
        hidController.stop()
    }

    fun requiredRuntimePermissions(): Array<String> = telemetryRepo.requiredRuntimePermissions()

    fun refreshPermissionsAndHid() {
        _permissions.value = telemetryRepo.checkPermissions()
        hidController.initializeHidProfile()
        hidController.refreshPairedDevices()
    }

    // =====================================================
    // Connection Controls
    // =====================================================

    fun connectOrQuickConnect() {
        val status = hidStatus.value
        if (status.phase == HidConnectionPhase.CONNECTED) {
            hidController.disconnect()
            return
        }
        val lastAddr = preferences.value.lastConnectedAddress
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
    // Wi-Fi Zero-Install Discovery & Wake-on-LAN
    // =====================================================

    fun scanWifiLan() {
        val ip = wifiStatus.value.localIpv4
        if (!wifiStatus.value.isWifiConnected || ip.isNullOrBlank()) {
            _feedback.tryEmit("Connect to a Wi-Fi network first to scan local Windows PCs.")
            return
        }
        viewModelScope.launch {
            val found = wifiRepo.scanLocalNetworkForWindowsPcs(ip)
            _feedback.tryEmit(
                if (found.isEmpty()) "No Windows LAN services (SMB/RDP/WS-Discovery) answered on $ip/24"
                else "Discovered ${found.size} Windows host(s) on local Wi-Fi"
            )
        }
    }

    fun sendWakeOnLan(macAddress: String) {
        viewModelScope.launch {
            val result = wifiRepo.sendWakeOnLan(macAddress, wifiStatus.value.subnetPrefix)
            _feedback.tryEmit(
                result.getOrElse { it.message ?: "Failed to send Wake-on-LAN packet" }
            )
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
            hidController.sendMouseReport(buttons, delta.dx, delta.dy)
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
            hidController.sendMouseReport(
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
            hidController.sendMouseReport(HidKeyCodes.MOUSE_NONE, 0, 0)
            return
        }
        hidController.sendMouseReport(buttonMask, 0, 0)
        hidController.sendMouseReport(effectiveButtonsMask(), 0, 0)
    }

    fun doubleClickLeft() {
        if (!requireConnectedWithToast()) return
        viewModelScope.launch {
            hidController.sendMouseReport(HidKeyCodes.MOUSE_LEFT, 0, 0)
            hidController.sendMouseReport(HidKeyCodes.MOUSE_NONE, 0, 0)
            kotlinx.coroutines.delay(55)
            hidController.sendMouseReport(HidKeyCodes.MOUSE_LEFT, 0, 0)
            hidController.sendMouseReport(HidKeyCodes.MOUSE_NONE, 0, 0)
        }
    }

    fun setMouseButtonHeld(buttonMask: Int, held: Boolean) {
        if (!requireConnectedSilently()) return
        activeMouseButtonsMask = if (held) {
            activeMouseButtonsMask or buttonMask
        } else {
            activeMouseButtonsMask and buttonMask.inv()
        }
        hidController.sendMouseReport(effectiveButtonsMask(), 0, 0)
    }

    fun toggleDragLock() {
        if (!requireConnectedWithToast()) return
        val next = !_dragLockEnabled.value
        _dragLockEnabled.value = next
        hidController.sendMouseReport(effectiveButtonsMask(), 0, 0)
    }

    fun setDragMode(active: Boolean) {
        if (!requireConnectedSilently()) return
        _dragLockEnabled.value = active
        hidController.sendMouseReport(effectiveButtonsMask(), 0, 0)
    }

    fun executeThreeFingerGesture(direction: String) {
        if (!requireConnectedWithToast()) return
        when (direction) {
            "up" -> hidController.sendKeyPressAndRelease(HidKeyCodes.MOD_LEFT_GUI, HidKeyCodes.KEY_TAB)
            "down" -> hidController.sendKeyPressAndRelease(HidKeyCodes.MOD_LEFT_GUI, HidKeyCodes.KEY_D)
            "left" -> hidController.sendKeyPressAndRelease(HidKeyCodes.MOD_LEFT_ALT, HidKeyCodes.KEY_TAB)
            "right" -> hidController.sendKeyPressAndRelease(
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
            FourFingerAction.SWITCH_DESKTOP_NEXT -> hidController.sendKeyPressAndRelease(
                HidKeyCodes.MOD_LEFT_CTRL or HidKeyCodes.MOD_LEFT_GUI,
                HidKeyCodes.KEY_RIGHT
            )
            FourFingerAction.SWITCH_DESKTOP_PREV -> hidController.sendKeyPressAndRelease(
                HidKeyCodes.MOD_LEFT_CTRL or HidKeyCodes.MOD_LEFT_GUI,
                HidKeyCodes.KEY_LEFT
            )
            FourFingerAction.TASK_VIEW -> hidController.sendKeyPressAndRelease(
                HidKeyCodes.MOD_LEFT_GUI,
                HidKeyCodes.KEY_TAB
            )
            FourFingerAction.SHOW_DESKTOP -> hidController.sendKeyPressAndRelease(
                HidKeyCodes.MOD_LEFT_GUI,
                HidKeyCodes.KEY_D
            )
            FourFingerAction.SNIP_SCREEN -> hidController.sendKeyPressAndRelease(
                HidKeyCodes.MOD_LEFT_GUI or HidKeyCodes.MOD_LEFT_SHIFT,
                HidKeyCodes.KEY_S
            )
            FourFingerAction.TASK_MANAGER -> hidController.sendKeyPressAndRelease(
                HidKeyCodes.MOD_LEFT_CTRL or HidKeyCodes.MOD_LEFT_SHIFT,
                HidKeyCodes.KEY_ESC
            )
            FourFingerAction.PLAY_PAUSE -> hidController.sendConsumerKey(HidKeyCodes.CONSUMER_PLAY_PAUSE)
            FourFingerAction.LOCK_PC -> hidController.sendKeyPressAndRelease(
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
        hidController.sendKeyPressAndRelease(combinedMod, keyCode)
        // Clear latched (single-shot) modifiers, keep locked ones
        _latchedModifiers.value = 0
    }

    fun sendCharFromTextBar(ch: Char) {
        if (!requireConnectedWithToast()) return
        val stroke = HidKeyCodes.charToHidKey(ch) ?: return
        hidController.sendKeyPressAndRelease(
            stroke.modifierMask or effectiveModifierMask.value,
            stroke.keyCode
        )
        _latchedModifiers.value = 0
    }

    fun sendTextString(text: String) {
        if (text.isEmpty()) return
        if (!requireConnectedWithToast()) return
        viewModelScope.launch {
            hidController.typeText(text)
        }
    }

    // =====================================================
    // Media & Presentation Actions
    // =====================================================

    fun sendMediaConsumerKey(usageCode: Int) {
        if (!requireConnectedWithToast()) return
        hidController.sendConsumerKey(usageCode)
    }

    fun sendPresentationAction(action: String) {
        if (!requireConnectedWithToast()) return
        when (action) {
            "next" -> hidController.sendKeyPressAndRelease(0, HidKeyCodes.KEY_RIGHT)
            "prev" -> hidController.sendKeyPressAndRelease(0, HidKeyCodes.KEY_LEFT)
            "start_beginning" -> hidController.sendKeyPressAndRelease(0, HidKeyCodes.KEY_F5)
            "start_current" -> hidController.sendKeyPressAndRelease(HidKeyCodes.MOD_LEFT_SHIFT, HidKeyCodes.KEY_F5)
            "exit" -> hidController.sendKeyPressAndRelease(0, HidKeyCodes.KEY_ESC)
            "black_screen" -> hidController.sendKeyPressAndRelease(0, HidKeyCodes.KEY_B)
            "fullscreen_f11" -> hidController.sendKeyPressAndRelease(0, HidKeyCodes.KEY_F11)
            "fullscreen_f" -> hidController.sendKeyPressAndRelease(0, HidKeyCodes.KEY_F)
            "seek_back" -> hidController.sendKeyPressAndRelease(0, HidKeyCodes.KEY_J)
            "seek_forward" -> hidController.sendKeyPressAndRelease(0, HidKeyCodes.KEY_L)
            "captions" -> hidController.sendKeyPressAndRelease(0, HidKeyCodes.KEY_C)
        }
    }

    fun toggleLaserPointer() {
        if (!requireConnectedWithToast()) return
        val next = !_laserPointerActive.value
        _laserPointerActive.value = next
        if (next) {
            hidController.sendKeyState(HidKeyCodes.MOD_LEFT_CTRL, emptyList())
            hidController.sendMouseReport(effectiveButtonsMask(), 0, 0)
        } else {
            hidController.sendMouseReport(HidKeyCodes.MOUSE_NONE, 0, 0)
            hidController.sendKeyState(0, emptyList())
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
        hidController.sendKeyPressAndRelease(shortcut.modifierMask, shortcut.keyCode)
    }

    fun confirmPendingShortcut() {
        val pending = _pendingDangerousShortcut.value ?: return
        _pendingDangerousShortcut.value = null
        if (!requireConnectedWithToast()) return
        hidController.sendKeyPressAndRelease(pending.modifierMask, pending.keyCode)
    }

    fun dismissPendingShortcut() {
        _pendingDangerousShortcut.value = null
    }

    fun triggerCustomShortcut(shortcut: CustomShortcut) {
        if (!requireConnectedWithToast()) return
        hidController.sendKeyPressAndRelease(shortcut.modifierMask, shortcut.keyCode)
    }

    fun addCustomShortcut(title: String, modifierMask: Int, keyCode: Int, comboLabel: String) {
        val cleanTitle = title.trim().ifBlank { comboLabel }
        val item = CustomShortcut(
            id = "custom_${System.currentTimeMillis()}",
            title = cleanTitle,
            comboLabel = comboLabel,
            modifierMask = modifierMask,
            keyCode = keyCode
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

    private fun requireConnectedSilently(): Boolean {
        return hidStatus.value.phase == HidConnectionPhase.CONNECTED
    }

    private fun requireConnectedWithToast(): Boolean {
        if (hidStatus.value.phase == HidConnectionPhase.CONNECTED) return true
        _feedback.tryEmit("Connect to your Windows PC first via Bluetooth HID.")
        return false
    }
}
