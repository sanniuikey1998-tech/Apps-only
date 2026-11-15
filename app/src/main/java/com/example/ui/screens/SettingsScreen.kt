package com.example.ui.screens

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.hid.FourFingerAction
import com.example.domain.model.AppPreferences
import com.example.domain.model.DiscoveredBtDevice
import com.example.domain.model.DiscoveredWifiPc
import com.example.domain.model.HidConnectionPhase
import com.example.domain.model.HidStatusState
import com.example.domain.model.PairedPcDevice
import com.example.domain.model.PermissionSummary
import com.example.domain.model.WifiStatusInfo
import com.example.ui.components.ConnectionStatusBadge
import com.example.ui.components.GlassPanel
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanIce
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.EmeraldConnected
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.LuminousViolet
import com.example.ui.theme.LuminousVioletBright
import com.example.ui.theme.RoseAlert
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    hidStatus: HidStatusState,
    isHidApiSupported: Boolean,
    permissions: PermissionSummary,
    pairedDevices: List<PairedPcDevice>,
    discoveredBtDevices: List<DiscoveredBtDevice>,
    isBtDiscovering: Boolean,
    wifiStatus: WifiStatusInfo,
    discoveredWifiPcs: List<DiscoveredWifiPc>,
    isWifiScanning: Boolean,
    preferences: AppPreferences,
    onRequestPermissions: () -> Unit,
    onConnectDevice: (String) -> Unit,
    onDisconnect: () -> Unit,
    onStartBtDiscovery: () -> Unit,
    onStopBtDiscovery: () -> Unit,
    onPairBtDevice: (String) -> Unit,
    onScanWifiLan: () -> Unit,
    onSendWakeOnLan: (String) -> Unit,
    onPointerSpeedChange: (Float) -> Unit,
    onSensitivityChange: (Float) -> Unit,
    onNaturalScrollChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onAutoReconnectChange: (Boolean) -> Unit,
    onAutoConnectConnectedChange: (Boolean) -> Unit,
    onDeviceAutoConnectChange: (String, Boolean) -> Unit,
    onTimeoutChange: (Int) -> Unit,
    onForegroundServiceChange: (Boolean) -> Unit,
    onConfirmDangerousChange: (Boolean) -> Unit,
    onFourFingerGestureChange: (String, FourFingerAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var wolMacInput by remember { mutableStateOf("") }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .testTag("settings_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Connection & Setup",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Bluetooth HID, Wi-Fi LAN Diagnostics & Tuning",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    ConnectionStatusBadge(phase = hidStatus.phase)
                }
            }

            // 1. Device HID Capability & Permission Status
            item {
                GlassPanel(
                    accentGlow = if (isHidApiSupported && permissions.allBluetoothGranted) EmeraldConnected else AmberWarning,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    SectionHeader(
                        title = "Bluetooth HID Profile Status",
                        subtitle = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    StatusRow("HID Device Profile API", if (isHidApiSupported) "Supported (Composite)" else "Unsupported (< API 28)", isHidApiSupported)
                    StatusRow("HID SDP Registration", if (hidStatus.isHidAppRegistered) "Registered" else hidStatus.statusMessage, hidStatus.isHidAppRegistered)
                    StatusRow("Bluetooth Permissions", if (permissions.allBluetoothGranted) "All Granted" else "Missing Runtime Access", permissions.allBluetoothGranted)

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!permissions.allBluetoothGranted) {
                            Button(
                                onClick = onRequestPermissions,
                                colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                                modifier = Modifier.weight(1f).testTag("settings_grant_permissions")
                            ) {
                                Text("Grant Permissions", color = MaterialTheme.colorScheme.background)
                            }
                        }
                        FilledTonalButton(
                            onClick = {
                                runCatching {
                                    val discoverable = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                                        putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 180)
                                    }
                                    context.startActivity(discoverable)
                                }.onFailure {
                                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("settings_make_discoverable")
                        ) {
                            Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Make Discoverable", maxLines = 1)
                        }
                    }
                }
            }

            // 2. Paired PCs & Bluetooth Discovery
            item {
                GlassPanel(contentPadding = PaddingValues(16.dp)) {
                    SectionHeader(
                        title = "Bluetooth PCs",
                        subtitle = "Zero-install native Windows HID connection",
                        trailing = {
                            if (hidStatus.phase == HidConnectionPhase.CONNECTED) {
                                TextButton(onClick = onDisconnect, modifier = Modifier.testTag("settings_disconnect_btn")) {
                                    Text("Disconnect", color = RoseAlert)
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (pairedDevices.isEmpty()) {
                        Text(
                            text = "No paired Bluetooth devices found yet. Scan below or pair from Windows Settings → Bluetooth & devices.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    } else {
                        pairedDevices.forEachIndexed { idx, device ->
                            if (idx > 0) HorizontalDivider(color = GlassBorderSubtle, modifier = Modifier.padding(vertical = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = when {
                                            device.isConnected -> Icons.Rounded.BluetoothConnected
                                            device.isLikelyComputer -> Icons.Rounded.Computer
                                            else -> Icons.Rounded.Bluetooth
                                        },
                                        contentDescription = null,
                                        tint = if (device.isConnected) EmeraldConnected else ElectricBlueBright,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = device.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = buildString {
                                                append(device.address)
                                                if (device.isConnected) append(" · HID Active")
                                                else if (device.isBluetoothAclConnected) append(" · BT Connected")
                                                device.lastRssiDbm?.let { append(" · $it dBm") }
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (device.isBluetoothAclConnected) CyanIce else TextMuted
                                        )
                                    }
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilterChip(
                                        selected = device.autoConnectEnabled,
                                        onClick = { onDeviceAutoConnectChange(device.address, !device.autoConnectEnabled) },
                                        label = { Text("Auto", maxLines = 1) },
                                        modifier = Modifier.testTag("auto_connect_chip_${device.address}")
                                    )
                                    if (device.isConnected) {
                                        OutlinedButton(onClick = onDisconnect) {
                                            Text("Disconnect", color = RoseAlert)
                                        }
                                    } else {
                                        FilledTonalButton(
                                            onClick = { onConnectDevice(device.address) },
                                            modifier = Modifier.testTag("settings_connect_${device.address}")
                                        ) {
                                            Text("Connect")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { if (isBtDiscovering) onStopBtDiscovery() else onStartBtDiscovery() },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        modifier = Modifier.fillMaxWidth().testTag("settings_bt_scan_btn")
                    ) {
                        if (isBtDiscovering) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stop Bluetooth Scan")
                        } else {
                            Icon(Icons.AutoMirrored.Rounded.BluetoothSearching, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Discover Nearby Bluetooth PCs")
                        }
                    }

                    if (discoveredBtDevices.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        discoveredBtDevices.forEach { dev ->
                            HorizontalDivider(color = GlassBorderSubtle, modifier = Modifier.padding(vertical = 6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(dev.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                    Text(
                                        text = "${dev.address} · ${dev.rssiDbm} dBm" +
                                            if (dev.isLikelyComputer) " · PC" else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                                FilledTonalButton(onClick = { onPairBtDevice(dev.address) }) {
                                    Text(if (dev.bondState == BluetoothDevice.BOND_BONDED) "Connect" else "Pair")
                                }
                            }
                        }
                    }
                }
            }

            // 3. Connection Resilience Settings
            item {
                GlassPanel(contentPadding = PaddingValues(16.dp)) {
                    SectionHeader(title = "Connection Behavior")
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingSwitchRow(
                        title = "Auto-Connect Connected BT Devices",
                        subtitle = "Automatically attach HID when an already-paired PC is connected via Bluetooth",
                        checked = preferences.autoConnectConnectedDevices,
                        onCheckedChange = onAutoConnectConnectedChange,
                        switchTestTag = "settings_auto_connect_switch"
                    )
                    SettingSwitchRow(
                        title = "Auto-Reconnect on Drop",
                        subtitle = "Automatically retry HID link if Bluetooth briefly drops",
                        checked = preferences.autoReconnect,
                        onCheckedChange = onAutoReconnectChange
                    )
                    SettingSwitchRow(
                        title = "Background Keep-Alive Service",
                        subtitle = "Prevents ColorOS / OPPO A3 5G battery sleep during use",
                        checked = preferences.foregroundServiceEnabled,
                        onCheckedChange = onForegroundServiceChange
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Connection Timeout: ${preferences.connectionTimeoutSec}s",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(10, 15, 30).forEach { sec ->
                            FilterChip(
                                selected = preferences.connectionTimeoutSec == sec,
                                onClick = { onTimeoutChange(sec) },
                                label = { Text("${sec}s") }
                            )
                        }
                    }
                }
            }

            // 4. Wi-Fi Zero-Install Discovery & Wake-on-LAN
            item {
                GlassPanel(
                    accentGlow = CyanIce,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    SectionHeader(
                        title = "Wi-Fi LAN Discovery & Wake-on-LAN",
                        subtitle = if (wifiStatus.isWifiConnected) {
                            "${wifiStatus.ssid ?: "Wi-Fi"} · ${wifiStatus.localIpv4 ?: "Checking IP..."}"
                        } else {
                            "Not connected to Wi-Fi"
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Honest Technical Explanation (per Section 2 & 15.6)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ElectricBlue.copy(alpha = 0.12f))
                            .padding(12.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Rounded.Info, contentDescription = null, tint = CyanIce, modifier = Modifier.size(20.dp))
                            Text(
                                text = "Zero-Install Technical Note: Stock Windows 11 does not accept arbitrary mouse or keyboard input over Wi-Fi without installing a Windows companion server. DeckLink uses Bluetooth HID for zero-install PC input, and uses Wi-Fi strictly for native LAN PC discovery (mDNS, WS-Discovery, SMB/RDP detection) and Wake-on-LAN.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onScanWifiLan,
                        enabled = !isWifiScanning,
                        colors = ButtonDefaults.buttonColors(containerColor = LuminousViolet),
                        modifier = Modifier.fillMaxWidth().testTag("settings_wifi_scan_btn")
                    ) {
                        if (isWifiScanning) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scanning Local Subnet...")
                        } else {
                            Icon(Icons.Rounded.Lan, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan Wi-Fi LAN for Windows PCs")
                        }
                    }

                    if (discoveredWifiPcs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        discoveredWifiPcs.forEach { pc ->
                            HorizontalDivider(color = GlassBorderSubtle, modifier = Modifier.padding(vertical = 6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Rounded.Wifi, contentDescription = null, tint = CyanIce, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(pc.hostname, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                        val portsText = if (pc.openWindowsPorts.isEmpty()) pc.discoverySource else "Ports: ${pc.openWindowsPorts.joinToString(", ")}"
                                        Text(
                                            text = "${pc.ipAddress} · $portsText",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Hardware Wake-on-LAN (Magic Packet)",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = wolMacInput,
                            onValueChange = { wolMacInput = it },
                            placeholder = { Text("PC NIC MAC (AA:BB:CC:DD:EE:FF)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("wol_mac_input")
                        )
                        FilledTonalButton(
                            onClick = { onSendWakeOnLan(wolMacInput) },
                            modifier = Modifier.height(52.dp).testTag("wol_send_btn")
                        ) {
                            Icon(Icons.Rounded.PowerSettingsNew, contentDescription = "Send Wake-on-LAN")
                        }
                    }
                }
            }

            // 5. Touchpad, Haptics & 4-Finger Gesture Customizer
            item {
                GlassPanel(contentPadding = PaddingValues(16.dp)) {
                    SectionHeader(title = "Touchpad & 4-Finger Gestures")
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Pointer Speed: ${"%.2f".format(preferences.pointerSpeed)}x", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                    Slider(value = preferences.pointerSpeed, onValueChange = onPointerSpeedChange, valueRange = 0.5f..3.0f)

                    Text("Mouse Sensitivity: ${"%.2f".format(preferences.mouseSensitivity)}x", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                    Slider(value = preferences.mouseSensitivity, onValueChange = onSensitivityChange, valueRange = 0.5f..2.5f)

                    SettingSwitchRow("Natural Scrolling", "Content follows finger direction", preferences.naturalScrolling, onNaturalScrollChange)
                    SettingSwitchRow("Haptic Feedback", "Tactile vibration on clicks & gestures", preferences.hapticFeedback, onHapticsChange)
                    SettingSwitchRow("Confirm Win+L / Alt+F4", "Ask before locking PC or closing apps", preferences.confirmDangerousShortcuts, onConfirmDangerousChange)

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("4-Finger Customizable Gestures", style = MaterialTheme.typography.labelMedium, color = LuminousVioletBright)
                    Spacer(modifier = Modifier.height(6.dp))
                    FourFingerPickerRow("4-Finger Swipe Up", preferences.fourFingerUp) { onFourFingerGestureChange("up", it) }
                    FourFingerPickerRow("4-Finger Swipe Down", preferences.fourFingerDown) { onFourFingerGestureChange("down", it) }
                    FourFingerPickerRow("4-Finger Swipe Left", preferences.fourFingerLeft) { onFourFingerGestureChange("left", it) }
                    FourFingerPickerRow("4-Finger Swipe Right", preferences.fourFingerRight) { onFourFingerGestureChange("right", it) }
                    FourFingerPickerRow("4-Finger Tap", preferences.fourFingerTap) { onFourFingerGestureChange("tap", it) }
                }
            }

            // 6. Security & Target Device Compatibility Guide
            item {
                GlassPanel(
                    accentGlow = EmeraldConnected,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Shield, contentDescription = null, tint = EmeraldConnected)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Security & Compatibility Report", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Zero Cloud & Zero External Servers: No keystrokes ever leave Bluetooth or your local LAN.\n" +
                            "• OPPO A3 5G (ColorOS / Android 14): Enable \"Allow background activity\" in App Info → Battery usage for uninterrupted HID latency.\n" +
                            "• HP ProBook 445R G6 (Windows 11): Native Microsoft HID drivers (kbdhid.sys / mouhid.sys) handle all input automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String, ok: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.Security,
                contentDescription = null,
                tint = if (ok) EmeraldConnected else AmberWarning,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = if (ok) EmeraldConnected else AmberWarning,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    switchTestTag: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = if (switchTestTag != null) Modifier.testTag(switchTestTag) else Modifier
        )
    }
}

@Composable
private fun FourFingerPickerRow(
    label: String,
    selected: FourFingerAction,
    onSelect: (FourFingerAction) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.weight(1f))
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(selected.label, color = ElectricBlueBright, maxLines = 1)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                FourFingerAction.entries.forEach { action ->
                    DropdownMenuItem(
                        text = { Text("${action.label} (${action.comboHint})") },
                        onClick = {
                            onSelect(action)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
