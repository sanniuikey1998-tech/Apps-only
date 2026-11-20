package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ControlCamera
import androidx.compose.material.icons.rounded.CoPresent
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.hid.HidKeyCodes
import com.example.domain.model.AppPreferences
import com.example.domain.model.HidStatusState
import com.example.domain.model.WifiConnectionPhase
import com.example.ui.components.ConnectionStatusBadge
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot

@Composable
fun MouseTouchpadScreen(
    hidStatus: HidStatusState,
    preferences: AppPreferences,
    dragLockEnabled: Boolean,
    onMovePointer: (Float, Float) -> Unit,
    onResetMath: () -> Unit,
    onScrollTwoFinger: (Float, Float) -> Unit,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
    onMiddleClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onButtonHoldChanged: (Int, Boolean) -> Unit,
    onToggleDragLock: () -> Unit,
    onSetDragMode: (Boolean) -> Unit,
    onThreeFingerGesture: (String) -> Unit,
    onFourFingerGesture: (String) -> Unit,
    onPointerSpeedChange: (Float) -> Unit,
    onSensitivityChange: (Float) -> Unit,
    onNaturalScrollChange: (Boolean) -> Unit,
    onOpenKeyboard: () -> Unit,
    onOpenPresentation: () -> Unit,
    modifier: Modifier = Modifier,
    wifiRemotePhase: WifiConnectionPhase = WifiConnectionPhase.DISCONNECTED,
    airMouseActive: Boolean = false,
    isAirMouseSupported: Boolean = true,
    onToggleAirMouse: () -> Unit = {},
    onSetAirMouseHeld: (Boolean) -> Unit = {},
    onAirMouseSensitivityChange: (Float) -> Unit = {},
    onPinchZoom: (Int) -> Unit = {}
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var showTuningPanel by remember { mutableStateOf(false) }
    var gestureBanner by remember { mutableStateOf("Touchpad Ready · 1–4 Finger Gestures") }
    var lastTapTimeMs by remember { mutableLongStateOf(0L) }
    val activeTouches = remember { mutableStateListOf<Offset>() }
    var momentumJob by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(Unit) {
        onDispose { momentumJob?.cancel() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Status & Quick Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    ConnectionStatusBadge(phase = hidStatus.phase, wifiPhase = wifiRemotePhase)
                    Text(
                        text = hidStatus.connectedDeviceName ?: "Touchpad",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isAirMouseSupported) {
                        FilterChip(
                            selected = airMouseActive,
                            onClick = {
                                performOptionalHaptic(haptics, preferences.hapticFeedback, strong = true)
                                onToggleAirMouse()
                            },
                            label = { Text("Air", maxLines = 1) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.ControlCamera,
                                    contentDescription = "Toggle Air Mouse",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LuminousViolet.copy(alpha = 0.28f),
                                selectedLabelColor = LuminousVioletBright,
                                selectedLeadingIconColor = LuminousVioletBright
                            ),
                            modifier = Modifier.testTag("mouse_air_mouse_chip")
                        )
                    }

                    FilterChip(
                        selected = dragLockEnabled,
                        onClick = {
                            performOptionalHaptic(haptics, preferences.hapticFeedback)
                            onToggleDragLock()
                        },
                        label = { Text("Drag", maxLines = 1) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.DragIndicator,
                                contentDescription = "Toggle Drag Lock",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricBlue.copy(alpha = 0.25f),
                            selectedLabelColor = ElectricBlueBright,
                            selectedLeadingIconColor = ElectricBlueBright
                        ),
                        modifier = Modifier.testTag("mouse_drag_lock_chip")
                    )

                    IconButton(
                        onClick = { showTuningPanel = !showTuningPanel },
                        modifier = Modifier.testTag("mouse_tuning_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "Pointer speed and sensitivity",
                            tint = if (showTuningPanel) ElectricBlueBright else TextSecondary
                        )
                    }

                    IconButton(
                        onClick = onOpenPresentation,
                        modifier = Modifier.testTag("mouse_presentation_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CoPresent,
                            contentDescription = "Presentation mode",
                            tint = TextSecondary
                        )
                    }

                    IconButton(
                        onClick = onOpenKeyboard,
                        modifier = Modifier.testTag("mouse_keyboard_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Keyboard,
                            contentDescription = "Open keyboard",
                            tint = TextSecondary
                        )
                    }
                }
            }

            // Collapsible Inline Speed & Sensitivity Tuning Panel
            AnimatedVisibility(visible = showTuningPanel) {
                GlassPanel(
                    elevated = true,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Pointer Speed: ${"%.2f".format(preferences.pointerSpeed)}x",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Sensitivity: ${"%.2f".format(preferences.mouseSensitivity)}x",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricBlueBright
                        )
                    }
                    Slider(
                        value = preferences.pointerSpeed,
                        onValueChange = onPointerSpeedChange,
                        valueRange = 0.5f..3.0f
                    )
                    Slider(
                        value = preferences.mouseSensitivity,
                        onValueChange = onSensitivityChange,
                        valueRange = 0.5f..2.5f
                    )
                    if (isAirMouseSupported) {
                        Text(
                            text = "Air Mouse Sensitivity: ${"%.2f".format(preferences.airMouseSensitivity)}x",
                            style = MaterialTheme.typography.labelMedium,
                            color = LuminousVioletBright
                        )
                        Slider(
                            value = preferences.airMouseSensitivity,
                            onValueChange = onAirMouseSensitivityChange,
                            valueRange = 0.5f..2.5f
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Natural Two-Finger Scroll",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Switch(
                            checked = preferences.naturalScrolling,
                            onCheckedChange = onNaturalScrollChange
                        )
                    }
                }
            }

            // Precision Multi-Touch Spatial Touchpad Surface
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(GlassSurfaceElevated, GlassSurface)
                        )
                    )
                    .border(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                if (dragLockEnabled) EmeraldConnected.copy(alpha = 0.7f) else GlassBorderHighlight,
                                LuminousViolet.copy(alpha = 0.25f)
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .testTag("touchpad_surface")
                    .pointerInput(
                        preferences.pointerSpeed,
                        preferences.mouseSensitivity,
                        preferences.naturalScrolling,
                        preferences.hapticFeedback
                    ) {
                        awaitPointerEventScope {
                            while (true) {
                                // Wait for first pointer down
                                val firstDownEvent = awaitPointerEvent()
                                val firstDown = firstDownEvent.changes.firstOrNull { it.pressed } ?: continue
                                firstDown.consume()
                                momentumJob?.cancel()
                                onResetMath()

                                val downTime = System.currentTimeMillis()
                                val isDoubleTapCandidate = (downTime - lastTapTimeMs) < 270L
                                var maxPointers = 1
                                var totalMovement = 0f
                                var isDoubleTapDragging = false
                                var longPressTriggered = false
                                var multiFingerNetDx = 0f
                                var multiFingerNetDy = 0f
                                var multiGestureFired = false
                                var pinchAccumulator = 0f
                                var lastTwoFingerSpan = 0f
                                var scrollVelX = 0f
                                var scrollVelY = 0f
                                var didTwoFingerScroll = false
                                var didPinch = false
                                var primaryId: PointerId = firstDown.id
                                var lastPrimaryPos: Offset = firstDown.position

                                activeTouches.clear()
                                activeTouches.add(firstDown.position)

                                val longPressJob: Job = scope.launch {
                                    delay(450L)
                                    if (maxPointers == 1 && totalMovement < 16f && !isDoubleTapCandidate) {
                                        longPressTriggered = true
                                        gestureBanner = "Long-Press · Right Click"
                                        performOptionalHaptic(haptics, preferences.hapticFeedback, strong = true)
                                        onRightClick()
                                    }
                                }

                                // Track gesture until all fingers lift
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val pressedChanges = event.changes.filter { it.pressed }
                                    activeTouches.clear()
                                    pressedChanges.forEach { activeTouches.add(it.position) }

                                    if (pressedChanges.isEmpty()) {
                                        longPressJob.cancel()
                                        activeTouches.clear()
                                        break
                                    }

                                    if (pressedChanges.size > maxPointers) {
                                        maxPointers = pressedChanges.size
                                        longPressJob.cancel()
                                    }

                                    when (pressedChanges.size) {
                                        1 -> {
                                            val pointer = pressedChanges.first()
                                            val delta = if (pointer.id == primaryId) {
                                                pointer.position - lastPrimaryPos
                                            } else {
                                                primaryId = pointer.id
                                                Offset.Zero
                                            }
                                            lastPrimaryPos = pointer.position
                                            val dist = hypot(delta.x, delta.y)
                                            totalMovement += dist

                                            if (totalMovement > 16f) {
                                                longPressJob.cancel()
                                                if (isDoubleTapCandidate && !isDoubleTapDragging && maxPointers == 1) {
                                                    isDoubleTapDragging = true
                                                    onSetDragMode(true)
                                                    gestureBanner = "Drag & Drop Active"
                                                    performOptionalHaptic(haptics, preferences.hapticFeedback)
                                                }
                                            }

                                            if (maxPointers == 1 && !longPressTriggered && dist > 0.25f) {
                                                if (!isDoubleTapDragging) {
                                                    gestureBanner = if (dragLockEnabled) "Dragging Item" else "Cursor Moving"
                                                }
                                                onMovePointer(delta.x, delta.y)
                                            }
                                        }

                                        2 -> {
                                            longPressJob.cancel()
                                            val p0 = pressedChanges[0].position
                                            val p1 = pressedChanges[1].position
                                            val prev0 = pressedChanges[0].previousPosition
                                            val prev1 = pressedChanges[1].previousPosition
                                            val currentSpan = hypot(p1.x - p0.x, p1.y - p0.y)
                                            val prevSpan = hypot(prev1.x - prev0.x, prev1.y - prev0.y)
                                            val spanDelta = if (lastTwoFingerSpan > 0f) currentSpan - prevSpan else 0f
                                            lastTwoFingerSpan = currentSpan

                                            val avgDx = ((p0.x - prev0.x) + (p1.x - prev1.x)) * 0.5f
                                            val avgDy = ((p0.y - prev0.y) + (p1.y - prev1.y)) * 0.5f
                                            val panDist = hypot(avgDx, avgDy)
                                            totalMovement += panDist + abs(spanDelta) * 0.5f

                                            if (abs(spanDelta) > panDist * 1.15f && abs(spanDelta) > 1.5f) {
                                                didPinch = true
                                                pinchAccumulator += spanDelta
                                                while (abs(pinchAccumulator) >= 28f) {
                                                    val step = if (pinchAccumulator > 0f) 1 else -1
                                                    pinchAccumulator -= step * 28f
                                                    gestureBanner = if (step > 0) "Pinch · Zoom In" else "Pinch · Zoom Out"
                                                    onPinchZoom(step)
                                                }
                                            } else if (panDist > 0.4f) {
                                                didTwoFingerScroll = true
                                                scrollVelX = scrollVelX * 0.45f + avgDx * 0.55f
                                                scrollVelY = scrollVelY * 0.45f + avgDy * 0.55f
                                                gestureBanner = "2-Finger Scroll (V / H)"
                                                onScrollTwoFinger(avgDx, avgDy)
                                            }
                                        }

                                        3, 4 -> {
                                            longPressJob.cancel()
                                            val avgDx = pressedChanges.map { it.position.x - it.previousPosition.x }.average().toFloat()
                                            val avgDy = pressedChanges.map { it.position.y - it.previousPosition.y }.average().toFloat()
                                            multiFingerNetDx += avgDx
                                            multiFingerNetDy += avgDy
                                            totalMovement += hypot(avgDx, avgDy)

                                            if (!multiGestureFired && hypot(multiFingerNetDx, multiFingerNetDy) > 72f) {
                                                multiGestureFired = true
                                                val dir = when {
                                                    abs(multiFingerNetDx) > abs(multiFingerNetDy) ->
                                                        if (multiFingerNetDx > 0) "right" else "left"
                                                    else ->
                                                        if (multiFingerNetDy > 0) "down" else "up"
                                                }
                                                performOptionalHaptic(haptics, preferences.hapticFeedback, strong = true)
                                                if (maxPointers == 3) {
                                                    gestureBanner = "3-Finger Swipe $dir"
                                                    onThreeFingerGesture(dir)
                                                } else {
                                                    gestureBanner = "4-Finger Swipe $dir"
                                                    onFourFingerGesture(dir)
                                                }
                                            }
                                        }
                                    }

                                    event.changes.forEach { it.consume() }
                                }

                                // All fingers released — evaluate tap actions or inertial scroll fling
                                val durationMs = System.currentTimeMillis() - downTime
                                if (isDoubleTapDragging) {
                                    onSetDragMode(false)
                                    gestureBanner = "Dropped"
                                    lastTapTimeMs = 0L
                                } else if (didTwoFingerScroll && !didPinch && hypot(scrollVelX, scrollVelY) > 6f) {
                                    val launchVx = scrollVelX.coerceIn(-48f, 48f)
                                    val launchVy = scrollVelY.coerceIn(-48f, 48f)
                                    momentumJob = scope.launch {
                                        var vx = launchVx
                                        var vy = launchVy
                                        repeat(28) {
                                            delay(16L)
                                            vx *= 0.86f
                                            vy *= 0.86f
                                            if (hypot(vx, vy) < 1.2f) return@launch
                                            onScrollTwoFinger(vx, vy)
                                        }
                                    }
                                } else if (!longPressTriggered && !multiGestureFired && !didPinch && totalMovement < 20f && durationMs < 320L) {
                                    when (maxPointers) {
                                        1 -> {
                                            val now = System.currentTimeMillis()
                                            if (now - lastTapTimeMs < 270L) {
                                                gestureBanner = "Double-Click"
                                                performOptionalHaptic(haptics, preferences.hapticFeedback)
                                                onDoubleClick()
                                                lastTapTimeMs = 0L
                                            } else {
                                                gestureBanner = "Left Click"
                                                performOptionalHaptic(haptics, preferences.hapticFeedback)
                                                onLeftClick()
                                                lastTapTimeMs = now
                                            }
                                        }
                                        2 -> {
                                            gestureBanner = "2-Finger Tap · Right Click"
                                            performOptionalHaptic(haptics, preferences.hapticFeedback)
                                            onRightClick()
                                        }
                                        3 -> {
                                            gestureBanner = "3-Finger Tap · Middle Click"
                                            performOptionalHaptic(haptics, preferences.hapticFeedback)
                                            onThreeFingerGesture("tap")
                                        }
                                        4 -> {
                                            gestureBanner = "4-Finger Tap"
                                            performOptionalHaptic(haptics, preferences.hapticFeedback, strong = true)
                                            onFourFingerGesture("tap")
                                        }
                                    }
                                }
                            }
                        }
                    }
            ) {
                // Spatial Dot Grid & Finger Touch Ripples
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val step = 28.dp.toPx()
                    var x = step
                    while (x < size.width) {
                        var y = step
                        while (y < size.height) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.06f),
                                radius = 1.2.dp.toPx(),
                                center = Offset(x, y)
                            )
                            y += step
                        }
                        x += step
                    }

                    activeTouches.forEach { pos ->
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(ElectricBlueBright.copy(alpha = 0.45f), Color.Transparent),
                                center = pos,
                                radius = 58.dp.toPx()
                            ),
                            radius = 58.dp.toPx(),
                            center = pos
                        )
                        drawCircle(
                            color = CyanIce,
                            radius = 6.dp.toPx(),
                            center = pos
                        )
                    }
                }

                // Subtle Top & Bottom Touchpad Hints
                Surface(
                    shape = CircleShape,
                    color = SpaceObsidianSurfaceColor(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 14.dp)
                ) {
                    Text(
                        text = gestureBanner,
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricBlueBright,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Text(
                    text = if (airMouseActive) {
                        "Air Mouse Active · Tilt or sweep phone to aim cursor"
                    } else {
                        "Tap: Left · 2-Tap: Right · 2-Drag: Momentum Scroll · Pinch: Zoom · 3/4-Finger Swipes"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (airMouseActive) LuminousVioletBright else TextMuted,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            // Hold-to-Aim Air Mouse Strip
            if (isAirMouseSupported) {
                HoldableMouseButton(
                    label = if (airMouseActive) "Air Mouse Active (Tap chip to stop)" else "Hold to Aim · Gyro Air Mouse",
                    accentColor = LuminousViolet,
                    hapticsEnabled = preferences.hapticFeedback,
                    onHoldChanged = { held ->
                        if (!airMouseActive || !held) {
                            onSetAirMouseHeld(held)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("mouse_air_hold_strip")
                )
            }

            // Physical Left / Middle / Right Click-and-Hold Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HoldableMouseButton(
                    label = "Left Click",
                    accentColor = ElectricBlue,
                    hapticsEnabled = preferences.hapticFeedback,
                    onHoldChanged = { held -> onButtonHoldChanged(HidKeyCodes.MOUSE_LEFT, held) },
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("mouse_left_button")
                )
                HoldableMouseButton(
                    label = "Mid",
                    accentColor = CyanIce,
                    hapticsEnabled = preferences.hapticFeedback,
                    onHoldChanged = { held -> onButtonHoldChanged(HidKeyCodes.MOUSE_MIDDLE, held) },
                    modifier = Modifier
                        .weight(0.75f)
                        .testTag("mouse_middle_button")
                )
                HoldableMouseButton(
                    label = "Right Click",
                    accentColor = LuminousViolet,
                    hapticsEnabled = preferences.hapticFeedback,
                    onHoldChanged = { held -> onButtonHoldChanged(HidKeyCodes.MOUSE_RIGHT, held) },
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("mouse_right_button")
                )
            }
        }
    }
}

@Composable
private fun HoldableMouseButton(
    label: String,
    accentColor: Color,
    hapticsEnabled: Boolean,
    onHoldChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (pressed) accentColor.copy(alpha = 0.30f) else GlassSurface
            )
            .border(
                width = 1.dp,
                color = if (pressed) accentColor else GlassBorderHighlight.copy(alpha = 0.25f),
                shape = RoundedCornerShape(18.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        performOptionalHaptic(haptics, hapticsEnabled)
                        onHoldChanged(true)
                        tryAwaitRelease()
                        pressed = false
                        onHoldChanged(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (pressed) Color.White else TextPrimary
        )
    }
}

@Composable
private fun SpaceObsidianSurfaceColor(): Color = Color(0xAA060913)
