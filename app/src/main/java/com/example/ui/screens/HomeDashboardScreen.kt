package com.example.ui.screens

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.CoPresent
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.LaptopWindows
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Mouse
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.BatteryStatusInfo
import com.example.domain.model.HidConnectionPhase
import com.example.domain.model.HidStatusState
import com.example.domain.model.PairedPcDevice
import com.example.domain.model.PermissionSummary
import com.example.domain.model.WifiStatusInfo
import com.example.ui.components.ConnectionStatusBadge
import com.example.ui.components.GlassActionTile
import com.example.ui.components.GlassPanel
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanIce
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.EmeraldConnected
import com.example.ui.theme.LuminousViolet
import com.example.ui.theme.LuminousVioletBright
import com.example.ui.theme.RoseAlert
import com.example.ui.theme.SpaceObsidian
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HomeDashboardScreen(
    hidStatus: HidStatusState,
    pairedDevices: List<PairedPcDevice>,
    wifiStatus: WifiStatusInfo,
    batteryStatus: BatteryStatusInfo,
    permissions: PermissionSummary,
    hapticsEnabled: Boolean,
    onQuickConnectClick: () -> Unit,
    onConnectDevice: (String) -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenMode: (String) -> Unit,
    onOpenConnectionSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isConnected = hidStatus.phase == HidConnectionPhase.CONNECTED

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DeckLink",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Zero-Install Windows HID Controller",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    ConnectionStatusBadge(
                        phase = hidStatus.phase,
                        modifier = Modifier.testTag("dashboard_status_badge")
                    )
                }
            }

            // 2. Permission / Bluetooth Off Action Banner if needed
            if (!permissions.allBluetoothGranted || hidStatus.phase == HidConnectionPhase.BLUETOOTH_OFF) {
                item {
                    GlassPanel(
                        accentGlow = AmberWarning,
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (!permissions.allBluetoothGranted) {
                                    Icons.Rounded.Security
                                } else {
                                    Icons.Rounded.Bluetooth
                                },
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(28.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (!permissions.allBluetoothGranted) {
                                        "Bluetooth Permissions Needed"
                                    } else {
                                        "Bluetooth Adapter Is Off"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (!permissions.allBluetoothGranted) {
                                        "Grant Nearby Devices access so DeckLink can register as a wireless mouse & keyboard."
                                    } else {
                                        "Enable Bluetooth to establish a native HID connection with your Windows 11 PC."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (!permissions.allBluetoothGranted) {
                                    onRequestPermissions()
                                } else {
                                    runCatching {
                                        context.startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                                    }.onFailure {
                                        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dashboard_fix_bt_button")
                        ) {
                            Text(
                                text = if (!permissions.allBluetoothGranted) "Grant Permissions" else "Enable Bluetooth",
                                color = SpaceObsidian,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 3. Spatial Hero Connection Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_spatial_pc_hero),
                        contentDescription = stringResource(R.string.cd_hero_banner),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        SpaceObsidian.copy(alpha = 0.52f),
                                        SpaceObsidian.copy(alpha = 0.90f)
                                    )
                                )
                            )
                    )

                    GlassPanel(
                        shape = RoundedCornerShape(28.dp),
                        accentGlow = if (isConnected) EmeraldConnected else ElectricBlue,
                        contentPadding = PaddingValues(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isConnected) EmeraldConnected.copy(alpha = 0.22f)
                                            else ElectricBlue.copy(alpha = 0.22f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isConnected) {
                                            Icons.Rounded.BluetoothConnected
                                        } else {
                                            Icons.Rounded.LaptopWindows
                                        },
                                        contentDescription = "PC link icon",
                                        tint = if (isConnected) EmeraldConnected else ElectricBlueBright,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = hidStatus.connectedDeviceName ?: "No PC Connected",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isConnected) {
                                            "Bluetooth HID Composite · ${hidStatus.connectedDeviceAddress.orEmpty()}"
                                        } else {
                                            hidStatus.statusMessage
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onQuickConnectClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isConnected) RoseAlert else ElectricBlue
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("quick_connect_button")
                            ) {
                                Icon(
                                    imageVector = if (isConnected) Icons.Rounded.LinkOff else Icons.AutoMirrored.Rounded.BluetoothSearching,
                                    contentDescription = stringResource(R.string.cd_connect_button),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        isConnected -> "Disconnect"
                                        hidStatus.phase == HidConnectionPhase.CONNECTING -> "Connecting..."
                                        pairedDevices.isNotEmpty() -> "Quick Connect"
                                        else -> "Pair a PC"
                                    },
                                    maxLines = 1
                                )
                            }

                            OutlinedButton(
                                onClick = onOpenConnectionSettings,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("open_connection_manager_button")
                            ) {
                                Text("Devices", color = TextPrimary)
                            }
                        }
                    }
                }
            }

            // 4. Live Telemetry Strip (Bluetooth Signal, Wi-Fi LAN, Phone Battery)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TelemetryMiniCard(
                        icon = Icons.Rounded.Bluetooth,
                        label = "Bluetooth",
                        value = when {
                            hidStatus.lastSignalRssiDbm != null -> "${hidStatus.lastSignalRssiDbm} dBm"
                            hidStatus.phase == HidConnectionPhase.CONNECTED -> "Active HID"
                            hidStatus.phase == HidConnectionPhase.BLUETOOTH_OFF -> "Off"
                            hidStatus.isHidAppRegistered -> "HID Ready"
                            else -> "Idle"
                        },
                        tint = if (isConnected) EmeraldConnected else ElectricBlueBright,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryMiniCard(
                        icon = if (wifiStatus.isWifiConnected) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                        label = "Wi-Fi LAN",
                        value = if (wifiStatus.isWifiConnected) {
                            wifiStatus.localIpv4 ?: "Connected"
                        } else {
                            "Offline"
                        },
                        tint = if (wifiStatus.isWifiConnected) CyanIce else TextMuted,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryMiniCard(
                        icon = if (batteryStatus.isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd,
                        label = "Phone Bat",
                        value = "${batteryStatus.levelPercent}%",
                        tint = when {
                            batteryStatus.levelPercent <= 15 -> RoseAlert
                            batteryStatus.isCharging -> EmeraldConnected
                            else -> LuminousVioletBright
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 5. Previously Paired PCs Quick Chips
            if (pairedDevices.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Paired Devices",
                        subtitle = "Tap a Windows PC to connect via Bluetooth HID"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pairedDevices, key = { it.address }) { device ->
                            val chipLabel = when {
                                device.isConnected -> "${device.name} · HID"
                                device.isBluetoothAclConnected -> "${device.name} · BT Active"
                                device.autoConnectEnabled -> "${device.name} · Auto"
                                else -> device.name
                            }
                            FilterChip(
                                selected = device.isConnected,
                                onClick = { onConnectDevice(device.address) },
                                label = {
                                    Text(
                                        text = chipLabel,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (device.isLikelyComputer) {
                                            Icons.Rounded.Computer
                                        } else {
                                            Icons.Rounded.Bluetooth
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldConnected.copy(alpha = 0.22f),
                                    selectedLabelColor = EmeraldConnected,
                                    selectedLeadingIconColor = EmeraldConnected
                                ),
                                modifier = Modifier.testTag("paired_chip_${device.address}")
                            )
                        }
                    }
                }
            }

            // 6. Control Modes Grid (Mouse, Keyboard, Media, Presentation, Desktop Control)
            item {
                SectionHeader(
                    title = "Control Modes",
                    subtitle = "Choose a wireless remote surface"
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ModeLauncherTile(
                            title = "Mouse",
                            subtitle = "Touchpad & gestures",
                            icon = Icons.Rounded.Mouse,
                            accent = ElectricBlue,
                            hapticsEnabled = hapticsEnabled,
                            onClick = { onOpenMode("mouse") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mode_card_mouse")
                        )
                        ModeLauncherTile(
                            title = "Keyboard",
                            subtitle = "QWERTY, Fn & text",
                            icon = Icons.Rounded.Keyboard,
                            accent = LuminousViolet,
                            hapticsEnabled = hapticsEnabled,
                            onClick = { onOpenMode("keyboard") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mode_card_keyboard")
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ModeLauncherTile(
                            title = "Media",
                            subtitle = "Playback & volume",
                            icon = Icons.Rounded.PlayCircleOutline,
                            accent = CyanIce,
                            hapticsEnabled = hapticsEnabled,
                            onClick = { onOpenMode("media") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mode_card_media")
                        )
                        ModeLauncherTile(
                            title = "Presentation",
                            subtitle = "Slides, laser & pointer",
                            icon = Icons.Rounded.CoPresent,
                            accent = EmeraldConnected,
                            hapticsEnabled = hapticsEnabled,
                            onClick = { onOpenMode("presentation") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mode_card_presentation")
                        )
                    }
                    GlassActionTile(
                        onClick = { onOpenMode("shortcuts") },
                        hapticsEnabled = hapticsEnabled,
                        accentColor = LuminousViolet,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("mode_card_desktop_control")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.DashboardCustomize,
                                    contentDescription = null,
                                    tint = LuminousVioletBright,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Desktop Control & Shortcuts",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Win+D, Alt+Tab, Task Manager, Lock PC & custom macros",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }
                    }
                }
            }

            // 7. Zero-Install Pairing Instructions
            item {
                GlassPanel(
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text(
                        text = "Windows 11 Zero-Install Pairing",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1. Open Settings → Bluetooth & devices → Add device on your HP ProBook.\n" +
                            "2. Tap \"Make Discoverable\" in DeckLink Devices tab.\n" +
                            "3. Pair with this phone — Windows auto-loads native Microsoft HID Mouse & Keyboard drivers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FilledTonalButton(
                        onClick = onOpenConnectionSettings,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Connection Manager & Wi-Fi Scanner")
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryMiniCard(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    GlassPanel(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ModeLauncherTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    hapticsEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassActionTile(
        onClick = onClick,
        hapticsEnabled = hapticsEnabled,
        accentColor = accent,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            maxLines = 1
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
