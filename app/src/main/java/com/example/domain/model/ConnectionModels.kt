package com.example.domain.model

import com.example.domain.hid.CustomShortcut
import com.example.domain.hid.FourFingerAction

import com.example.domain.hid.WindowsVirtualKeyMapper

enum class RemoteTransportMode {
    BLUETOOTH_HID,
    WIFI_POWERSHELL
}

enum class WifiConnectionDirection {
    PHONE_TO_PC,
    PC_TO_PHONE_HOTSPOT
}

enum class WifiConnectionPhase {
    DISCONNECTED,
    LISTENING,
    CONNECTING,
    AUTHENTICATING,
    CONNECTED,
    ERROR
}

data class WifiRemoteStatus(
    val phase: WifiConnectionPhase = WifiConnectionPhase.DISCONNECTED,
    val direction: WifiConnectionDirection = WifiConnectionDirection.PHONE_TO_PC,
    val hostIp: String? = null,
    val port: Int = WindowsVirtualKeyMapper.DEFAULT_WIFI_PORT,
    val remoteHostname: String? = null,
    val latencyMs: Long? = null,
    val statusMessage: String = "Local Wi-Fi PowerShell receiver idle"
)

data class ScriptServerStatus(
    val isRunning: Boolean = false,
    val port: Int = WindowsVirtualKeyMapper.DEFAULT_WIFI_PORT + 1,
    val oneLinerCommand: String = "",
    val remainingSeconds: Int = 0,
    val serveCount: Int = 0,
    val statusNote: String = ""
)

data class FileTransferState(
    val isTransferring: Boolean = false,
    val fileName: String = "",
    val progressFraction: Float = 0f,
    val lastResultMessage: String? = null
)

data class VoiceDictationState(
    val isListening: Boolean = false,
    val isAvailable: Boolean = true,
    val partialTranscript: String = "",
    val errorNote: String? = null
)

data class PresentationTimerState(
    val targetDurationSec: Int = 600,
    val elapsedSec: Int = 0,
    val isRunning: Boolean = false,
    val slideNumber: Int = 1,
    val lastMilestoneAlert: String? = null
) {
    val remainingSec: Int
        get() = targetDurationSec - elapsedSec
}

enum class HidConnectionPhase {
    HID_UNSUPPORTED,
    MISSING_PERMISSIONS,
    BLUETOOTH_OFF,
    REGISTERING_HID,
    READY_TO_PAIR,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class HidStatusState(
    val phase: HidConnectionPhase = HidConnectionPhase.REGISTERING_HID,
    val connectedDeviceName: String? = null,
    val connectedDeviceAddress: String? = null,
    val isHidAppRegistered: Boolean = false,
    val lastSignalRssiDbm: Int? = null,
    val statusMessage: String = "Initializing Bluetooth HID profile...",
    val hostCapsLock: Boolean = false,
    val hostNumLock: Boolean = false,
    val hostScrollLock: Boolean = false
)

data class PairedPcDevice(
    val name: String,
    val address: String,
    val majorDeviceClass: Int,
    val isLikelyComputer: Boolean,
    val isConnected: Boolean,
    val lastRssiDbm: Int? = null,
    val isBluetoothAclConnected: Boolean = false,
    val autoConnectEnabled: Boolean = false
)

data class DiscoveredBtDevice(
    val name: String,
    val address: String,
    val rssiDbm: Int,
    val bondState: Int,
    val isLikelyComputer: Boolean
)

data class DiscoveredWifiPc(
    val ipAddress: String,
    val hostname: String,
    val discoverySource: String,
    val openWindowsPorts: List<Int>,
    val macAddress: String? = null
)

data class WifiStatusInfo(
    val isWifiConnected: Boolean = false,
    val ssid: String? = null,
    val localIpv4: String? = null,
    val subnetPrefix: String? = null,
    val linkSpeedMbps: Int? = null,
    val isHotspotActive: Boolean = false,
    val hotspotIpv4: String? = null,
    val hotspotSubnetPrefix: String? = null
) {
    val hasLocalNetwork: Boolean
        get() = isWifiConnected || isHotspotActive

    val effectiveLocalIpv4: String?
        get() = localIpv4 ?: hotspotIpv4

    val effectiveSubnetPrefix: String?
        get() = subnetPrefix ?: hotspotSubnetPrefix
}

data class BatteryStatusInfo(
    val levelPercent: Int = 100,
    val isCharging: Boolean = false
)

data class PermissionSummary(
    val hasBluetoothConnect: Boolean = false,
    val hasBluetoothScan: Boolean = false,
    val hasBluetoothAdvertise: Boolean = false,
    val hasLegacyLocation: Boolean = true,
    val hasNotifications: Boolean = true
) {
    val allBluetoothGranted: Boolean
        get() = hasBluetoothConnect && hasBluetoothScan && hasBluetoothAdvertise && hasLegacyLocation
}

data class AppPreferences(
    val pointerSpeed: Float = 1.35f,
    val mouseSensitivity: Float = 1.15f,
    val naturalScrolling: Boolean = true,
    val hapticFeedback: Boolean = true,
    val autoReconnect: Boolean = true,
    val autoConnectConnectedDevices: Boolean = true,
    val connectionTimeoutSec: Int = 15,
    val foregroundServiceEnabled: Boolean = true,
    val confirmDangerousShortcuts: Boolean = true,
    val fourFingerUp: FourFingerAction = FourFingerAction.TASK_VIEW,
    val fourFingerDown: FourFingerAction = FourFingerAction.SHOW_DESKTOP,
    val fourFingerLeft: FourFingerAction = FourFingerAction.SWITCH_DESKTOP_NEXT,
    val fourFingerRight: FourFingerAction = FourFingerAction.SWITCH_DESKTOP_PREV,
    val fourFingerTap: FourFingerAction = FourFingerAction.SNIP_SCREEN,
    val lastConnectedAddress: String? = null,
    val lastConnectedName: String? = null,
    val autoConnectAddresses: Set<String> = emptySet(),
    val disabledAutoConnectAddresses: Set<String> = emptySet(),
    val customShortcuts: List<CustomShortcut> = emptyList(),
    val preferredTransport: RemoteTransportMode = RemoteTransportMode.BLUETOOTH_HID,
    val wifiDirection: WifiConnectionDirection = WifiConnectionDirection.PC_TO_PHONE_HOTSPOT,
    val wifiTargetIp: String = "",
    val wifiPort: Int = WindowsVirtualKeyMapper.DEFAULT_WIFI_PORT,
    val wifiPin: String = WindowsVirtualKeyMapper.DEFAULT_WIFI_PIN,
    val airMouseSensitivity: Float = 1.25f,
    val volumeButtonsControl: Boolean = true
)
