package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.hid.WindowsShortcut
import com.example.domain.model.HidConnectionPhase
import com.example.domain.model.WifiConnectionPhase
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanIce
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.EmeraldConnected
import com.example.ui.theme.GlassBorderHighlight
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.LuminousViolet
import com.example.ui.theme.RoseAlert
import com.example.ui.theme.SpaceDeep
import com.example.ui.theme.SpaceObsidian
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SpatialBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SpaceObsidian, SpaceDeep, SpaceObsidian)
                )
            )
            .drawBehind {
                // Subtle electric blue aura top-left
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(ElectricBlue.copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(size.width * 0.18f, size.height * 0.08f),
                        radius = size.maxDimension * 0.45f
                    ),
                    center = Offset(size.width * 0.18f, size.height * 0.08f),
                    radius = size.maxDimension * 0.45f
                )
                // Subtle luminous violet aura bottom-right
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(LuminousViolet.copy(alpha = 0.13f), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.78f),
                        radius = size.maxDimension * 0.45f
                    ),
                    center = Offset(size.width * 0.85f, size.height * 0.78f),
                    radius = size.maxDimension * 0.45f
                )
            },
        content = content
    )
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    elevated: Boolean = false,
    accentGlow: Color? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val bgColor = if (elevated) GlassSurfaceElevated else GlassSurface
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            accentGlow?.copy(alpha = 0.45f) ?: GlassBorderHighlight,
            GlassBorderSubtle
        )
    )
    Column(
        modifier = modifier
            .clip(shape)
            .background(bgColor)
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun GlassActionTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hapticsEnabled: Boolean = true,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(20.dp),
    accentColor: Color = ElectricBlue,
    isActive: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val bgColor by animateColorAsState(
        targetValue = when {
            isActive -> accentColor.copy(alpha = 0.24f)
            else -> GlassSurface
        },
        label = "tile_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isActive -> accentColor.copy(alpha = 0.75f)
            else -> GlassBorderHighlight.copy(alpha = 0.22f)
        },
        label = "tile_border"
    )

    Column(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(shape)
            .background(bgColor)
            .border(1.dp, borderColor, shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(color = accentColor),
                role = Role.Button,
                onClick = {
                    if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun ConnectionStatusBadge(
    phase: HidConnectionPhase,
    modifier: Modifier = Modifier,
    wifiPhase: WifiConnectionPhase = WifiConnectionPhase.DISCONNECTED
) {
    val (label, color) = when {
        phase == HidConnectionPhase.CONNECTED -> "BT HID" to EmeraldConnected
        wifiPhase == WifiConnectionPhase.CONNECTED -> "WIFI LIVE" to CyanIce
        wifiPhase == WifiConnectionPhase.LISTENING -> "HOTSPOT WAIT" to CyanIce
        wifiPhase == WifiConnectionPhase.CONNECTING || wifiPhase == WifiConnectionPhase.AUTHENTICATING ->
            "WIFI AUTH" to AmberWarning
        phase == HidConnectionPhase.CONNECTING -> "CONNECTING" to AmberWarning
        phase == HidConnectionPhase.REGISTERING_HID -> "INIT HID" to CyanIce
        phase == HidConnectionPhase.READY_TO_PAIR -> "HID READY" to ElectricBlueBright
        phase == HidConnectionPhase.BLUETOOTH_OFF -> "BT OFF" to RoseAlert
        phase == HidConnectionPhase.MISSING_PERMISSIONS -> "PERMISSIONS" to AmberWarning
        phase == HidConnectionPhase.HID_UNSUPPORTED -> "UNSUPPORTED" to RoseAlert
        else -> "ERROR" to RoseAlert
    }

    val shouldPulse = phase == HidConnectionPhase.CONNECTED ||
        phase == HidConnectionPhase.CONNECTING ||
        wifiPhase == WifiConnectionPhase.CONNECTED ||
        wifiPhase == WifiConnectionPhase.LISTENING ||
        wifiPhase == WifiConnectionPhase.CONNECTING ||
        wifiPhase == WifiConnectionPhase.AUTHENTICATING
    val pulseAlpha = if (shouldPulse) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val animated by infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(900),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )
        animated
    } else {
        1f
    }

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = pulseAlpha))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun DangerousShortcutDialog(
    shortcut: WindowsShortcut,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpaceDeep,
        iconContentColor = AmberWarning,
        icon = {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = "Safety confirmation"
            )
        },
        title = {
            Text(
                text = "Send ${shortcut.title}?",
                color = TextPrimary
            )
        },
        text = {
            Text(
                text = "This will immediately send ${shortcut.comboLabel} (${shortcut.description.lowercase()}) to your connected Windows PC.",
                color = TextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = RoseAlert),
                modifier = Modifier.testTag("confirm_dangerous_shortcut")
            ) {
                Text("Send ${shortcut.comboLabel}")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_dangerous_shortcut")
            ) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

fun performOptionalHaptic(
    hapticFeedback: HapticFeedback,
    enabled: Boolean,
    strong: Boolean = false
) {
    if (!enabled) return
    hapticFeedback.performHapticFeedback(
        if (strong) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
    )
}
