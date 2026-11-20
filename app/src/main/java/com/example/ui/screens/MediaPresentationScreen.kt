package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.ControlCamera
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircleFilled
import androidx.compose.material.icons.rounded.PresentToAll
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.hid.HidKeyCodes
import com.example.domain.model.HidStatusState
import com.example.domain.model.PresentationTimerState
import com.example.domain.model.WifiConnectionPhase
import com.example.ui.components.ConnectionStatusBadge
import com.example.ui.components.GlassActionTile
import com.example.ui.components.GlassPanel
import com.example.ui.components.performOptionalHaptic
import com.example.ui.theme.CyanIce
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.EmeraldConnected
import com.example.ui.theme.GlassBorderHighlight
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.LuminousViolet
import com.example.ui.theme.LuminousVioletBright
import com.example.ui.theme.RoseAlert
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.abs
import kotlin.math.hypot

@Composable
fun MediaPresentationScreen(
    hidStatus: HidStatusState,
    initialTab: Int,
    laserPointerActive: Boolean,
    hapticsEnabled: Boolean,
    onSendConsumerKey: (Int) -> Unit,
    onPresentationAction: (String) -> Unit,
    onToggleLaserPointer: () -> Unit,
    onMovePointer: (Float, Float) -> Unit,
    onLeftClick: () -> Unit,
    modifier: Modifier = Modifier,
    wifiRemotePhase: WifiConnectionPhase = WifiConnectionPhase.DISCONNECTED,
    presentationTimer: PresentationTimerState = PresentationTimerState(),
    airMouseActive: Boolean = false,
    isAirMouseSupported: Boolean = true,
    onToggleAirMouse: () -> Unit = {},
    onToggleTimer: () -> Unit = {},
    onResetTimer: () -> Unit = {},
    onSetTimerMinutes: (Int) -> Unit = {},
    onSelectedTabChanged: (Int) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    LaunchedEffect(initialTab) {
        selectedTab = initialTab.coerceIn(0, 1)
    }
    LaunchedEffect(selectedTab) {
        onSelectedTabChanged(selectedTab)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedTab == 0) "Media Remote" else "Presentation",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    ConnectionStatusBadge(phase = hidStatus.phase, wifiPhase = wifiRemotePhase)
                }
            }

            item {
                val tabs = listOf("Media Remote", "Presentation")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    tabs.forEachIndexed { idx, label ->
                        SegmentedButton(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = tabs.size),
                            modifier = Modifier.testTag("media_seg_tab_$idx")
                        ) {
                            Text(label, maxLines = 1)
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                item {
                    MediaRemoteContent(
                        hapticsEnabled = hapticsEnabled,
                        onSendConsumerKey = onSendConsumerKey,
                        onPresentationAction = onPresentationAction
                    )
                }
            } else {
                item {
                    PresentationRemoteContent(
                        laserPointerActive = laserPointerActive,
                        airMouseActive = airMouseActive,
                        isAirMouseSupported = isAirMouseSupported,
                        presentationTimer = presentationTimer,
                        hapticsEnabled = hapticsEnabled,
                        onPresentationAction = onPresentationAction,
                        onToggleLaserPointer = onToggleLaserPointer,
                        onToggleAirMouse = onToggleAirMouse,
                        onToggleTimer = onToggleTimer,
                        onResetTimer = onResetTimer,
                        onSetTimerMinutes = onSetTimerMinutes,
                        onMovePointer = onMovePointer,
                        onLeftClick = onLeftClick
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaRemoteContent(
    hapticsEnabled: Boolean,
    onSendConsumerKey: (Int) -> Unit,
    onPresentationAction: (String) -> Unit
) {
    val haptics = LocalHapticFeedback.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Primary Playback Deck
        GlassPanel(
            elevated = true,
            accentGlow = ElectricBlue,
            contentPadding = PaddingValues(20.dp)
        ) {
            Text(
                text = "Playback Transport (HID Consumer)",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundMediaButton(
                    icon = Icons.Rounded.SkipPrevious,
                    label = "Previous",
                    size = 62.dp,
                    accent = LuminousViolet,
                    hapticsEnabled = hapticsEnabled,
                    onClick = { onSendConsumerKey(HidKeyCodes.CONSUMER_PREV_TRACK) },
                    modifier = Modifier.testTag("media_prev_button")
                )

                // Hero Play/Pause Orb
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(ElectricBlue, LuminousViolet))
                        )
                        .clickable {
                            performOptionalHaptic(haptics, hapticsEnabled, strong = true)
                            onSendConsumerKey(HidKeyCodes.CONSUMER_PLAY_PAUSE)
                        }
                        .testTag("media_play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play or Pause",
                        tint = Color.White,
                        modifier = Modifier.size(42.dp)
                    )
                }

                RoundMediaButton(
                    icon = Icons.Rounded.SkipNext,
                    label = "Next",
                    size = 62.dp,
                    accent = LuminousViolet,
                    hapticsEnabled = hapticsEnabled,
                    onClick = { onSendConsumerKey(HidKeyCodes.CONSUMER_NEXT_TRACK) },
                    modifier = Modifier.testTag("media_next_button")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassActionTile(
                    onClick = { onPresentationAction("seek_back") },
                    hapticsEnabled = hapticsEnabled,
                    accentColor = CyanIce,
                    modifier = Modifier.weight(1f).testTag("media_seek_back")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.FastRewind, contentDescription = "Seek back", tint = CyanIce)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("-10s", color = TextPrimary, style = MaterialTheme.typography.labelLarge)
                    }
                }

                GlassActionTile(
                    onClick = { onSendConsumerKey(HidKeyCodes.CONSUMER_STOP) },
                    hapticsEnabled = hapticsEnabled,
                    accentColor = RoseAlert,
                    modifier = Modifier.weight(1f).testTag("media_stop_button")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Stop, contentDescription = "Stop playback", tint = RoseAlert)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Stop", color = TextPrimary, style = MaterialTheme.typography.labelLarge)
                    }
                }

                GlassActionTile(
                    onClick = { onPresentationAction("seek_forward") },
                    hapticsEnabled = hapticsEnabled,
                    accentColor = CyanIce,
                    modifier = Modifier.weight(1f).testTag("media_seek_forward")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("+10s", color = TextPrimary, style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Rounded.FastForward, contentDescription = "Seek forward", tint = CyanIce)
                    }
                }
            }
        }

        // Volume Control Deck
        GlassPanel(contentPadding = PaddingValues(16.dp)) {
            Text(
                text = "Windows System Volume",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassActionTile(
                    onClick = { onSendConsumerKey(HidKeyCodes.CONSUMER_VOL_DOWN) },
                    hapticsEnabled = hapticsEnabled,
                    accentColor = ElectricBlue,
                    modifier = Modifier.weight(1f).testTag("media_vol_down")
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeDown, contentDescription = "Volume down", tint = ElectricBlueBright)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Vol -", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                    }
                }

                GlassActionTile(
                    onClick = { onSendConsumerKey(HidKeyCodes.CONSUMER_MUTE) },
                    hapticsEnabled = hapticsEnabled,
                    accentColor = RoseAlert,
                    modifier = Modifier.weight(1f).testTag("media_mute")
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeOff, contentDescription = "Mute audio", tint = RoseAlert)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Mute", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                    }
                }

                GlassActionTile(
                    onClick = { onSendConsumerKey(HidKeyCodes.CONSUMER_VOL_UP) },
                    hapticsEnabled = hapticsEnabled,
                    accentColor = ElectricBlue,
                    modifier = Modifier.weight(1f).testTag("media_vol_up")
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Volume up", tint = ElectricBlueBright)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Vol +", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                    }
                }
            }
        }

        // Player Window & Fullscreen Controls
        GlassPanel(contentPadding = PaddingValues(16.dp)) {
            Text(
                text = "Media Navigation & Screen",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassActionTile(
                    onClick = { onPresentationAction("fullscreen_f") },
                    hapticsEnabled = hapticsEnabled,
                    accentColor = LuminousViolet,
                    modifier = Modifier.weight(1f).testTag("media_fullscreen_f")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Fullscreen, contentDescription = "Player fullscreen", tint = LuminousVioletBright)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Player Full (F)", color = TextPrimary, style = MaterialTheme.typography.labelMedium)
                    }
                }

                GlassActionTile(
                    onClick = { onPresentationAction("fullscreen_f11") },
                    hapticsEnabled = hapticsEnabled,
                    accentColor = ElectricBlue,
                    modifier = Modifier.weight(1f).testTag("media_fullscreen_f11")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Fullscreen, contentDescription = "Browser fullscreen", tint = ElectricBlueBright)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Browser (F11)", color = TextPrimary, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            GlassActionTile(
                onClick = { onPresentationAction("captions") },
                hapticsEnabled = hapticsEnabled,
                accentColor = CyanIce,
                modifier = Modifier.fillMaxWidth().testTag("media_captions")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.ClosedCaption, contentDescription = "Toggle captions", tint = CyanIce)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Toggle Subtitles / Captions (C)", color = TextPrimary, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun PresentationRemoteContent(
    laserPointerActive: Boolean,
    airMouseActive: Boolean,
    isAirMouseSupported: Boolean,
    presentationTimer: PresentationTimerState,
    hapticsEnabled: Boolean,
    onPresentationAction: (String) -> Unit,
    onToggleLaserPointer: () -> Unit,
    onToggleAirMouse: () -> Unit,
    onToggleTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onSetTimerMinutes: (Int) -> Unit,
    onMovePointer: (Float, Float) -> Unit,
    onLeftClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val remaining = presentationTimer.remainingSec
    val isOvertime = remaining < 0
    val absSec = abs(remaining)
    val timerText = "${if (isOvertime) "+" else ""}%02d:%02d".format(absSec / 60, absSec % 60)
    val progress = if (presentationTimer.targetDurationSec > 0) {
        (presentationTimer.elapsedSec.toFloat() / presentationTimer.targetDurationSec.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Presentation Timer & Slide Counter Card
        GlassPanel(
            elevated = true,
            accentGlow = if (isOvertime) RoseAlert else ElectricBlue,
            contentPadding = PaddingValues(14.dp),
            modifier = Modifier.testTag("pres_timer_card")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = if (isOvertime) RoseAlert else ElectricBlueBright,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = timerText,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isOvertime) RoseAlert else TextPrimary
                        )
                        Text(
                            text = presentationTimer.lastMilestoneAlert
                                ?: "Use phone Vol+/Vol- buttons for Next/Prev slide",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOvertime) RoseAlert else TextSecondary
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(LuminousViolet.copy(alpha = 0.22f))
                        .border(1.dp, LuminousViolet.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Slide #${presentationTimer.slideNumber}",
                        style = MaterialTheme.typography.labelLarge,
                        color = LuminousVioletBright
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                color = if (isOvertime) RoseAlert else ElectricBlue,
                trackColor = GlassSurfaceElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(5, 10, 15, 20).forEach { mins ->
                    val selected = presentationTimer.targetDurationSec == mins * 60
                    FilterChip(
                        selected = selected,
                        onClick = {
                            performOptionalHaptic(haptics, hapticsEnabled)
                            onSetTimerMinutes(mins)
                        },
                        label = { Text("${mins}m", maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricBlue.copy(alpha = 0.24f),
                            selectedLabelColor = ElectricBlueBright
                        ),
                        modifier = Modifier.testTag("pres_timer_preset_$mins")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        performOptionalHaptic(haptics, hapticsEnabled)
                        onToggleTimer()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (presentationTimer.isRunning) LuminousViolet else ElectricBlue
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("pres_timer_toggle")
                ) {
                    Icon(
                        imageVector = if (presentationTimer.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (presentationTimer.isRunning) "Pause Timer" else "Start Timer")
                }
                OutlinedButton(
                    onClick = {
                        performOptionalHaptic(haptics, hapticsEnabled)
                        onResetTimer()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("pres_timer_reset")
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = "Reset timer", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset")
                }
            }
        }

        // Large Ergonomic Prev / Next Slide Paddles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassActionTile(
                onClick = { onPresentationAction("prev") },
                hapticsEnabled = hapticsEnabled,
                accentColor = LuminousViolet,
                contentPadding = PaddingValues(vertical = 28.dp, horizontal = 12.dp),
                modifier = Modifier.weight(1f).testTag("pres_prev_slide")
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Previous slide",
                        tint = LuminousVioletBright,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Prev Slide", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }
            }

            GlassActionTile(
                onClick = { onPresentationAction("next") },
                hapticsEnabled = hapticsEnabled,
                accentColor = ElectricBlue,
                isActive = true,
                contentPadding = PaddingValues(vertical = 28.dp, horizontal = 12.dp),
                modifier = Modifier.weight(1.3f).testTag("pres_next_slide")
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "Next slide",
                        tint = ElectricBlueBright,
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Next Slide", style = MaterialTheme.typography.titleLarge, color = Color.White)
                }
            }
        }

        // Start, Current Slide, Black Screen, Exit
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    performOptionalHaptic(haptics, hapticsEnabled)
                    onPresentationAction("start_beginning")
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldConnected),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f).height(48.dp).testTag("pres_start_f5")
            ) {
                Icon(Icons.Rounded.PlayCircleFilled, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Start (F5)", maxLines = 1)
            }

            Button(
                onClick = {
                    performOptionalHaptic(haptics, hapticsEnabled)
                    onPresentationAction("start_current")
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f).height(48.dp).testTag("pres_start_current")
            ) {
                Icon(Icons.Rounded.PresentToAll, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Current", maxLines = 1)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassActionTile(
                onClick = { onPresentationAction("black_screen") },
                hapticsEnabled = hapticsEnabled,
                accentColor = CyanIce,
                modifier = Modifier.weight(1f).testTag("pres_black_screen")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Contrast, contentDescription = "Black screen", tint = CyanIce)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Black Screen (B)", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                }
            }

            GlassActionTile(
                onClick = { onPresentationAction("exit") },
                hapticsEnabled = hapticsEnabled,
                accentColor = RoseAlert,
                modifier = Modifier.weight(1f).testTag("pres_exit")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Exit slideshow", tint = RoseAlert)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exit (Esc)", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                }
            }
        }

        // Integrated Pointer & Laser Touchpad
        GlassPanel(contentPadding = PaddingValues(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Slide Pointer Surface",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Drag to move cursor · Tap to advance/click",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isAirMouseSupported) {
                        FilterChip(
                            selected = airMouseActive,
                            onClick = {
                                performOptionalHaptic(haptics, hapticsEnabled, strong = true)
                                onToggleAirMouse()
                            },
                            label = { Text("Air", maxLines = 1) },
                            leadingIcon = {
                                Icon(Icons.Rounded.ControlCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LuminousViolet.copy(alpha = 0.25f),
                                selectedLabelColor = LuminousVioletBright,
                                selectedLeadingIconColor = LuminousVioletBright
                            ),
                            modifier = Modifier.testTag("pres_air_mouse_toggle")
                        )
                    }
                    FilterChip(
                        selected = laserPointerActive,
                        onClick = {
                            performOptionalHaptic(haptics, hapticsEnabled, strong = true)
                            onToggleLaserPointer()
                        },
                        label = { Text("Laser", maxLines = 1) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Flare, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RoseAlert.copy(alpha = 0.25f),
                            selectedLabelColor = RoseAlert,
                            selectedLeadingIconColor = RoseAlert
                        ),
                        modifier = Modifier.testTag("pres_laser_toggle")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(GlassSurfaceElevated)
                    .border(
                        1.dp,
                        if (laserPointerActive) RoseAlert.copy(alpha = 0.65f) else GlassBorderHighlight,
                        RoundedCornerShape(20.dp)
                    )
                    .testTag("pres_pointer_pad")
                    .pointerInput(hapticsEnabled) {
                        awaitPointerEventScope {
                            while (true) {
                                val downEvent = awaitPointerEvent()
                                val down = downEvent.changes.firstOrNull { it.pressed } ?: continue
                                down.consume()
                                var moved = 0f
                                var lastPos = down.position
                                while (true) {
                                    val ev = awaitPointerEvent()
                                    val active = ev.changes.firstOrNull { it.pressed }
                                    if (active == null) break
                                    val delta = active.position - lastPos
                                    lastPos = active.position
                                    moved += hypot(delta.x, delta.y)
                                    onMovePointer(delta.x, delta.y)
                                    active.consume()
                                }
                                if (moved < 16f) {
                                    performOptionalHaptic(haptics, hapticsEnabled)
                                    onLeftClick()
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                (if (laserPointerActive) RoseAlert else ElectricBlue).copy(alpha = 0.14f),
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.minDimension * 0.45f
                        )
                    )
                }
                Text(
                    text = if (laserPointerActive) "Laser Pointer Active (Ctrl + Drag)" else "Pointer Touchpad Area",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (laserPointerActive) RoseAlert else TextMuted
                )
            }
        }
    }
}

@Composable
private fun RoundMediaButton(
    icon: ImageVector,
    label: String,
    size: androidx.compose.ui.unit.Dp,
    accent: Color,
    hapticsEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(GlassSurface)
            .border(1.dp, accent.copy(alpha = 0.45f), CircleShape)
            .clickable {
                performOptionalHaptic(haptics, hapticsEnabled)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = TextPrimary,
            modifier = Modifier.size(28.dp)
        )
    }
}
