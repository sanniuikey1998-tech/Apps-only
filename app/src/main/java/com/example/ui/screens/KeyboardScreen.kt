package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.SpaceBar
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.hid.CustomShortcut
import com.example.domain.hid.HidKeyCodes
import com.example.domain.model.HidStatusState
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

@Composable
fun KeyboardScreen(
    hidStatus: HidStatusState,
    latchedModifiers: Int,
    lockedModifiers: Int,
    customShortcuts: List<CustomShortcut>,
    hapticsEnabled: Boolean,
    onToggleModifier: (Int) -> Unit,
    onClearModifiers: () -> Unit,
    onSendKey: (Int, Int) -> Unit,
    onSendChar: (Char) -> Unit,
    onSendText: (String) -> Unit,
    onTriggerCustomShortcut: (CustomShortcut) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    var textBuffer by remember { mutableStateOf("") }
    var selectedLayout by remember { mutableIntStateOf(0) } // 0 = QWERTY, 1 = 123 & Sym, 2 = Fn & Nav

    val isShifted = ((latchedModifiers or lockedModifiers) and HidKeyCodes.MOD_LEFT_SHIFT) != 0

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .testTag("keyboard_list"),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header & Host LEDs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ConnectionStatusBadge(phase = hidStatus.phase)
                        Text(
                            text = "Wireless Keyboard",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        LedBadge("CAPS", hidStatus.hostCapsLock)
                        LedBadge("NUM", hidStatus.hostNumLock)
                    }
                }
            }

            // Direct Text Input & Send Bar
            item {
                GlassPanel(contentPadding = PaddingValues(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = textBuffer,
                            onValueChange = { textBuffer = it },
                            placeholder = {
                                Text(
                                    "Type or paste text to send to Windows...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (textBuffer.isNotEmpty()) {
                                        onSendText(textBuffer)
                                        textBuffer = ""
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = GlassBorderHighlight.copy(alpha = 0.25f),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("keyboard_text_input")
                        )
                        FilledIconButton(
                            onClick = {
                                performOptionalHaptic(haptics, hapticsEnabled)
                                if (textBuffer.isNotEmpty()) {
                                    onSendText(textBuffer)
                                    textBuffer = ""
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = ElectricBlue),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("keyboard_send_text_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = "Send text to PC"
                            )
                        }
                    }
                }
            }

            // Sticky Modifiers + Quick Edit Bar
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ModifierKeyCap(
                            label = "Ctrl",
                            modBit = HidKeyCodes.MOD_LEFT_CTRL,
                            latched = latchedModifiers,
                            locked = lockedModifiers,
                            hapticsEnabled = hapticsEnabled,
                            onClick = { onToggleModifier(HidKeyCodes.MOD_LEFT_CTRL) },
                            modifier = Modifier.weight(1f).testTag("mod_ctrl")
                        )
                        ModifierKeyCap(
                            label = "Shift",
                            modBit = HidKeyCodes.MOD_LEFT_SHIFT,
                            latched = latchedModifiers,
                            locked = lockedModifiers,
                            hapticsEnabled = hapticsEnabled,
                            onClick = { onToggleModifier(HidKeyCodes.MOD_LEFT_SHIFT) },
                            modifier = Modifier.weight(1f).testTag("mod_shift")
                        )
                        ModifierKeyCap(
                            label = "Alt",
                            modBit = HidKeyCodes.MOD_LEFT_ALT,
                            latched = latchedModifiers,
                            locked = lockedModifiers,
                            hapticsEnabled = hapticsEnabled,
                            onClick = { onToggleModifier(HidKeyCodes.MOD_LEFT_ALT) },
                            modifier = Modifier.weight(1f).testTag("mod_alt")
                        )
                        ModifierKeyCap(
                            label = "Win",
                            modBit = HidKeyCodes.MOD_LEFT_GUI,
                            latched = latchedModifiers,
                            locked = lockedModifiers,
                            hapticsEnabled = hapticsEnabled,
                            onClick = { onToggleModifier(HidKeyCodes.MOD_LEFT_GUI) },
                            modifier = Modifier.weight(1f).testTag("mod_win")
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        KeyCapChip("Esc", hapticsEnabled) { onSendKey(HidKeyCodes.KEY_ESC, 0) }
                        KeyCapChip("Tab", hapticsEnabled) { onSendKey(HidKeyCodes.KEY_TAB, 0) }
                        KeyCapChip("Del", hapticsEnabled) { onSendKey(HidKeyCodes.KEY_DELETE, 0) }
                        KeyCapChip("Copy", hapticsEnabled, accent = CyanIce) {
                            onSendKey(HidKeyCodes.KEY_C, HidKeyCodes.MOD_LEFT_CTRL)
                        }
                        KeyCapChip("Paste", hapticsEnabled, accent = CyanIce) {
                            onSendKey(HidKeyCodes.KEY_V, HidKeyCodes.MOD_LEFT_CTRL)
                        }
                        KeyCapChip("Cut", hapticsEnabled, accent = CyanIce) {
                            onSendKey(HidKeyCodes.KEY_X, HidKeyCodes.MOD_LEFT_CTRL)
                        }
                        KeyCapChip("Select All", hapticsEnabled, accent = CyanIce) {
                            onSendKey(HidKeyCodes.KEY_A, HidKeyCodes.MOD_LEFT_CTRL)
                        }
                        if ((latchedModifiers or lockedModifiers) != 0) {
                            KeyCapChip("Clear Mod", hapticsEnabled, accent = LuminousViolet) {
                                onClearModifiers()
                            }
                        }
                    }
                }
            }

            // Layout Mode Segmented Selector
            item {
                val tabs = listOf("QWERTY", "123 & Sym", "Fn & Nav")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    tabs.forEachIndexed { idx, label ->
                        SegmentedButton(
                            selected = selectedLayout == idx,
                            onClick = { selectedLayout = idx },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = tabs.size),
                            modifier = Modifier.testTag("kb_tab_$idx")
                        ) {
                            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            // Active Keyboard Grid
            item {
                when (selectedLayout) {
                    0 -> QwertyKeyboardSection(
                        isShifted = isShifted,
                        latchedModifiers = latchedModifiers,
                        lockedModifiers = lockedModifiers,
                        hapticsEnabled = hapticsEnabled,
                        onToggleShift = { onToggleModifier(HidKeyCodes.MOD_LEFT_SHIFT) },
                        onSendKey = onSendKey
                    )

                    1 -> NumbersAndSymbolsSection(
                        hapticsEnabled = hapticsEnabled,
                        onSendChar = onSendChar,
                        onSendKey = onSendKey
                    )

                    else -> FunctionAndNavSection(
                        hapticsEnabled = hapticsEnabled,
                        customShortcuts = customShortcuts,
                        onSendKey = onSendKey,
                        onTriggerCustom = onTriggerCustomShortcut
                    )
                }
            }
        }
    }
}

@Composable
private fun QwertyKeyboardSection(
    isShifted: Boolean,
    latchedModifiers: Int,
    lockedModifiers: Int,
    hapticsEnabled: Boolean,
    onToggleShift: () -> Unit,
    onSendKey: (Int, Int) -> Unit
) {
    val row1 = listOf(
        "Q" to HidKeyCodes.KEY_Q, "W" to HidKeyCodes.KEY_W, "E" to HidKeyCodes.KEY_E,
        "R" to HidKeyCodes.KEY_R, "T" to HidKeyCodes.KEY_T, "Y" to HidKeyCodes.KEY_Y,
        "U" to HidKeyCodes.KEY_U, "I" to HidKeyCodes.KEY_I, "O" to HidKeyCodes.KEY_O,
        "P" to HidKeyCodes.KEY_P
    )
    val row2 = listOf(
        "A" to HidKeyCodes.KEY_A, "S" to HidKeyCodes.KEY_S, "D" to HidKeyCodes.KEY_D,
        "F" to HidKeyCodes.KEY_F, "G" to HidKeyCodes.KEY_G, "H" to HidKeyCodes.KEY_H,
        "J" to HidKeyCodes.KEY_J, "K" to HidKeyCodes.KEY_K, "L" to HidKeyCodes.KEY_L
    )
    val row3 = listOf(
        "Z" to HidKeyCodes.KEY_Z, "X" to HidKeyCodes.KEY_X, "C" to HidKeyCodes.KEY_C,
        "V" to HidKeyCodes.KEY_V, "B" to HidKeyCodes.KEY_B, "N" to HidKeyCodes.KEY_N,
        "M" to HidKeyCodes.KEY_M
    )

    GlassPanel(contentPadding = PaddingValues(8.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            // Number quick strip
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val nums = listOf(
                    "1" to HidKeyCodes.KEY_1, "2" to HidKeyCodes.KEY_2, "3" to HidKeyCodes.KEY_3,
                    "4" to HidKeyCodes.KEY_4, "5" to HidKeyCodes.KEY_5, "6" to HidKeyCodes.KEY_6,
                    "7" to HidKeyCodes.KEY_7, "8" to HidKeyCodes.KEY_8, "9" to HidKeyCodes.KEY_9,
                    "0" to HidKeyCodes.KEY_0
                )
                nums.forEach { (label, code) ->
                    KeyCapButton(
                        label = label,
                        hapticsEnabled = hapticsEnabled,
                        onClick = { onSendKey(code, 0) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // QWERTY Row 1
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row1.forEach { (label, code) ->
                    KeyCapButton(
                        label = if (isShifted) label else label.lowercase(),
                        hapticsEnabled = hapticsEnabled,
                        onClick = { onSendKey(code, 0) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // QWERTY Row 2
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row2.forEach { (label, code) ->
                    KeyCapButton(
                        label = if (isShifted) label else label.lowercase(),
                        hapticsEnabled = hapticsEnabled,
                        onClick = { onSendKey(code, 0) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // QWERTY Row 3 with Shift & Backspace
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ModifierKeyCap(
                    label = "⇧",
                    modBit = HidKeyCodes.MOD_LEFT_SHIFT,
                    latched = latchedModifiers,
                    locked = lockedModifiers,
                    hapticsEnabled = hapticsEnabled,
                    onClick = onToggleShift,
                    modifier = Modifier.weight(1.4f)
                )
                row3.forEach { (label, code) ->
                    KeyCapButton(
                        label = if (isShifted) label else label.lowercase(),
                        hapticsEnabled = hapticsEnabled,
                        onClick = { onSendKey(code, 0) },
                        modifier = Modifier.weight(1f)
                    )
                }
                IconKeyCapButton(
                    icon = Icons.AutoMirrored.Rounded.Backspace,
                    description = "Backspace",
                    hapticsEnabled = hapticsEnabled,
                    onClick = { onSendKey(HidKeyCodes.KEY_BACKSPACE, 0) },
                    modifier = Modifier.weight(1.4f).testTag("key_backspace")
                )
            }

            // Bottom Row: , Space . Enter
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                KeyCapButton(
                    label = ",",
                    hapticsEnabled = hapticsEnabled,
                    onClick = { onSendKey(HidKeyCodes.KEY_COMMA, 0) },
                    modifier = Modifier.weight(1f)
                )
                IconKeyCapButton(
                    icon = Icons.Rounded.SpaceBar,
                    description = "Space",
                    hapticsEnabled = hapticsEnabled,
                    onClick = { onSendKey(HidKeyCodes.KEY_SPACE, 0) },
                    modifier = Modifier.weight(4f).testTag("key_space")
                )
                KeyCapButton(
                    label = ".",
                    hapticsEnabled = hapticsEnabled,
                    onClick = { onSendKey(HidKeyCodes.KEY_PERIOD, 0) },
                    modifier = Modifier.weight(1f)
                )
                IconKeyCapButton(
                    icon = Icons.AutoMirrored.Rounded.KeyboardReturn,
                    description = "Enter",
                    accent = ElectricBlue,
                    hapticsEnabled = hapticsEnabled,
                    onClick = { onSendKey(HidKeyCodes.KEY_ENTER, 0) },
                    modifier = Modifier.weight(1.8f).testTag("key_enter")
                )
            }
        }
    }
}

@Composable
private fun NumbersAndSymbolsSection(
    hapticsEnabled: Boolean,
    onSendChar: (Char) -> Unit,
    onSendKey: (Int, Int) -> Unit
) {
    val rows = listOf(
        listOf('1', '2', '3', '4', '5', '6', '7', '8', '9', '0'),
        listOf('!', '@', '#', '$', '%', '^', '&', '*', '(', ')'),
        listOf('-', '_', '=', '+', '[', ']', '{', '}', '\\', '|'),
        listOf(';', ':', '\'', '"', ',', '.', '<', '>', '/', '?'),
        listOf('`', '~')
    )

    GlassPanel(contentPadding = PaddingValues(8.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            rows.take(4).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    row.forEach { ch ->
                        KeyCapButton(
                            label = ch.toString(),
                            hapticsEnabled = hapticsEnabled,
                            onClick = { onSendChar(ch) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rows[4].forEach { ch ->
                    KeyCapButton(
                        label = ch.toString(),
                        hapticsEnabled = hapticsEnabled,
                        onClick = { onSendChar(ch) },
                        modifier = Modifier.weight(1f)
                    )
                }
                IconKeyCapButton(
                    icon = Icons.Rounded.SpaceBar,
                    description = "Space",
                    hapticsEnabled = hapticsEnabled,
                    onClick = { onSendKey(HidKeyCodes.KEY_SPACE, 0) },
                    modifier = Modifier.weight(3f)
                )
                IconKeyCapButton(
                    icon = Icons.AutoMirrored.Rounded.Backspace,
                    description = "Backspace",
                    hapticsEnabled = hapticsEnabled,
                    onClick = { onSendKey(HidKeyCodes.KEY_BACKSPACE, 0) },
                    modifier = Modifier.weight(1.6f)
                )
                IconKeyCapButton(
                    icon = Icons.AutoMirrored.Rounded.KeyboardReturn,
                    description = "Enter",
                    accent = ElectricBlue,
                    hapticsEnabled = hapticsEnabled,
                    onClick = { onSendKey(HidKeyCodes.KEY_ENTER, 0) },
                    modifier = Modifier.weight(1.6f)
                )
            }
        }
    }
}

@Composable
private fun FunctionAndNavSection(
    hapticsEnabled: Boolean,
    customShortcuts: List<CustomShortcut>,
    onSendKey: (Int, Int) -> Unit,
    onTriggerCustom: (CustomShortcut) -> Unit
) {
    val fKeysRow1 = listOf(
        "F1" to HidKeyCodes.KEY_F1, "F2" to HidKeyCodes.KEY_F2, "F3" to HidKeyCodes.KEY_F3,
        "F4" to HidKeyCodes.KEY_F4, "F5" to HidKeyCodes.KEY_F5, "F6" to HidKeyCodes.KEY_F6
    )
    val fKeysRow2 = listOf(
        "F7" to HidKeyCodes.KEY_F7, "F8" to HidKeyCodes.KEY_F8, "F9" to HidKeyCodes.KEY_F9,
        "F10" to HidKeyCodes.KEY_F10, "F11" to HidKeyCodes.KEY_F11, "F12" to HidKeyCodes.KEY_F12
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassPanel(contentPadding = PaddingValues(10.dp)) {
            Text(
                text = "Function Keys (F1 – F12)",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                fKeysRow1.forEach { (label, code) ->
                    KeyCapButton(
                        label = label,
                        hapticsEnabled = hapticsEnabled,
                        onClick = { onSendKey(code, 0) },
                        modifier = Modifier.weight(1f).testTag("key_$label")
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                fKeysRow2.forEach { (label, code) ->
                    KeyCapButton(
                        label = label,
                        hapticsEnabled = hapticsEnabled,
                        onClick = { onSendKey(code, 0) },
                        modifier = Modifier.weight(1f).testTag("key_$label")
                    )
                }
            }
        }

        // Navigation Cluster + Arrow D-Pad
        GlassPanel(contentPadding = PaddingValues(10.dp)) {
            Text(
                text = "Navigation & Cursor Arrows",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                KeyCapButton("Home", hapticsEnabled, { onSendKey(HidKeyCodes.KEY_HOME, 0) }, Modifier.weight(1f))
                KeyCapButton("End", hapticsEnabled, { onSendKey(HidKeyCodes.KEY_END, 0) }, Modifier.weight(1f))
                KeyCapButton("PgUp", hapticsEnabled, { onSendKey(HidKeyCodes.KEY_PAGE_UP, 0) }, Modifier.weight(1f))
                KeyCapButton("PgDn", hapticsEnabled, { onSendKey(HidKeyCodes.KEY_PAGE_DOWN, 0) }, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                KeyCapButton("Ins", hapticsEnabled, { onSendKey(HidKeyCodes.KEY_INSERT, 0) }, Modifier.weight(1f))
                KeyCapButton("Del", hapticsEnabled, { onSendKey(HidKeyCodes.KEY_DELETE, 0) }, Modifier.weight(1f))
                KeyCapButton("PrtSc", hapticsEnabled, { onSendKey(HidKeyCodes.KEY_PRINT_SCREEN, 0) }, Modifier.weight(1f))
                KeyCapButton("Pause", hapticsEnabled, { onSendKey(HidKeyCodes.KEY_PAUSE, 0) }, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconKeyCapButton(
                    icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    description = "Left arrow",
                    hapticsEnabled = hapticsEnabled,
                    onClick = { onSendKey(HidKeyCodes.KEY_LEFT, 0) },
                    modifier = Modifier.weight(1f).testTag("key_arrow_left")
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconKeyCapButton(
                        icon = Icons.Rounded.KeyboardArrowUp,
                        description = "Up arrow",
                        hapticsEnabled = hapticsEnabled,
                        onClick = { onSendKey(HidKeyCodes.KEY_UP, 0) },
                        modifier = Modifier.fillMaxWidth().testTag("key_arrow_up")
                    )
                    IconKeyCapButton(
                        icon = Icons.Rounded.KeyboardArrowDown,
                        description = "Down arrow",
                        hapticsEnabled = hapticsEnabled,
                        onClick = { onSendKey(HidKeyCodes.KEY_DOWN, 0) },
                        modifier = Modifier.fillMaxWidth().testTag("key_arrow_down")
                    )
                }
                IconKeyCapButton(
                    icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    description = "Right arrow",
                    hapticsEnabled = hapticsEnabled,
                    onClick = { onSendKey(HidKeyCodes.KEY_RIGHT, 0) },
                    modifier = Modifier.weight(1f).testTag("key_arrow_right")
                )
            }
        }

        // Function Combos + Saved Custom Shortcuts
        GlassPanel(contentPadding = PaddingValues(10.dp)) {
            Text(
                text = "Function & Custom Combos",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                KeyCapChip("Alt+F4", hapticsEnabled, LuminousViolet) {
                    onSendKey(HidKeyCodes.KEY_F4, HidKeyCodes.MOD_LEFT_ALT)
                }
                KeyCapChip("Win+Tab", hapticsEnabled, ElectricBlue) {
                    onSendKey(HidKeyCodes.KEY_TAB, HidKeyCodes.MOD_LEFT_GUI)
                }
                KeyCapChip("Ctrl+Shift+Esc", hapticsEnabled, CyanIce) {
                    onSendKey(HidKeyCodes.KEY_ESC, HidKeyCodes.MOD_LEFT_CTRL or HidKeyCodes.MOD_LEFT_SHIFT)
                }
                customShortcuts.forEach { custom ->
                    KeyCapChip("${custom.title} (${custom.comboLabel})", hapticsEnabled, EmeraldConnected) {
                        onTriggerCustom(custom)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModifierKeyCap(
    label: String,
    modBit: Int,
    latched: Int,
    locked: Int,
    hapticsEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val isLocked = (locked and modBit) != 0
    val isLatched = (latched and modBit) != 0
    val bgColor = when {
        isLocked -> LuminousViolet.copy(alpha = 0.35f)
        isLatched -> ElectricBlue.copy(alpha = 0.28f)
        else -> GlassSurface
    }
    val borderColor = when {
        isLocked -> LuminousVioletBright
        isLatched -> ElectricBlueBright
        else -> GlassBorderHighlight.copy(alpha = 0.22f)
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable {
                performOptionalHaptic(haptics, hapticsEnabled)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isLocked) "$label •" else label,
            style = MaterialTheme.typography.labelLarge,
            fontSize = 13.sp,
            color = if (isLocked || isLatched) Color.White else TextPrimary
        )
    }
}

@Composable
private fun KeyCapButton(
    label: String,
    hapticsEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(GlassSurfaceElevated)
            .border(1.dp, GlassBorderHighlight.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
            .clickable {
                performOptionalHaptic(haptics, hapticsEnabled)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            maxLines = 1
        )
    }
}

@Composable
private fun IconKeyCapButton(
    icon: ImageVector,
    description: String,
    hapticsEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color? = null
) {
    val haptics = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(accent?.copy(alpha = 0.28f) ?: GlassSurfaceElevated)
            .border(
                1.dp,
                accent?.copy(alpha = 0.65f) ?: GlassBorderHighlight.copy(alpha = 0.18f),
                RoundedCornerShape(10.dp)
            )
            .clickable {
                performOptionalHaptic(haptics, hapticsEnabled)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = TextPrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun KeyCapChip(
    label: String,
    hapticsEnabled: Boolean,
    accent: Color = ElectricBlue,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.16f))
            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .clickable {
                performOptionalHaptic(haptics, hapticsEnabled)
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            maxLines = 1
        )
    }
}

@Composable
private fun LedBadge(label: String, active: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) EmeraldConnected.copy(alpha = 0.25f) else GlassSurface)
            .border(
                1.dp,
                if (active) EmeraldConnected else GlassBorderHighlight.copy(alpha = 0.2f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) EmeraldConnected else TextMuted
        )
    }
}
