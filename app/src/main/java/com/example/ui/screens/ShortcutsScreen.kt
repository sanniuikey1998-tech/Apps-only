package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.hid.CustomShortcut
import com.example.domain.hid.HidKeyCodes
import com.example.domain.hid.ShortcutCategory
import com.example.domain.hid.WindowsShortcut
import com.example.domain.hid.WindowsShortcutsCatalog
import com.example.domain.model.HidStatusState
import com.example.ui.components.ConnectionStatusBadge
import com.example.ui.components.GlassActionTile
import com.example.ui.components.GlassPanel
import com.example.ui.components.SectionHeader
import com.example.ui.theme.CyanIce
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.EmeraldConnected
import com.example.ui.theme.GlassBorderHighlight
import com.example.ui.theme.LuminousViolet
import com.example.ui.theme.LuminousVioletBright
import com.example.ui.theme.RoseAlert
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun ShortcutsScreen(
    hidStatus: HidStatusState,
    customShortcuts: List<CustomShortcut>,
    hapticsEnabled: Boolean,
    onTriggerShortcut: (WindowsShortcut) -> Unit,
    onTriggerCustomShortcut: (CustomShortcut) -> Unit,
    onAddCustomShortcut: (String, Int, Int, String) -> Unit,
    onDeleteCustomShortcut: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("ALL") }
    var showCreatorPanel by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val builtIn = remember(selectedCategory) {
        when (selectedCategory) {
            "SYSTEM" -> WindowsShortcutsCatalog.builtInShortcuts.filter { it.category == ShortcutCategory.SYSTEM }
            "NAV" -> WindowsShortcutsCatalog.builtInShortcuts.filter { it.category == ShortcutCategory.NAVIGATION }
            "EDIT" -> WindowsShortcutsCatalog.builtInShortcuts.filter { it.category == ShortcutCategory.EDITING }
            "CUSTOM" -> emptyList()
            else -> WindowsShortcutsCatalog.builtInShortcuts
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .testTag("shortcuts_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Windows Shortcuts",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Explicitly triggered HID key combinations",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    ConnectionStatusBadge(phase = hidStatus.phase)
                }
            }

            item {
                AnimatedVisibility(visible = showCreatorPanel) {
                    CustomShortcutCreatorPanel(
                        onDismiss = { showCreatorPanel = false },
                        onSave = { title, modMask, keyCode, combo ->
                            onAddCustomShortcut(title, modMask, keyCode, combo)
                            showCreatorPanel = false
                        }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        "ALL" to "All",
                        "SYSTEM" to "System",
                        "NAV" to "Windows Nav",
                        "EDIT" to "Clipboard & Edit",
                        "CUSTOM" to "Custom (${customShortcuts.size})"
                    )
                    filters.forEach { (key, label) ->
                        FilterChip(
                            selected = selectedCategory == key,
                            onClick = { selectedCategory = key },
                            label = { Text(label, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricBlue.copy(alpha = 0.25f),
                                selectedLabelColor = ElectricBlueBright
                            ),
                            modifier = Modifier.testTag("filter_chip_$key")
                        )
                    }
                }
            }

            if (builtIn.isNotEmpty()) {
                val chunked = builtIn.chunked(2)
                items(chunked) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { shortcut ->
                            val accent = when {
                                shortcut.requiresSafetyConfirm -> RoseAlert
                                shortcut.category == ShortcutCategory.SYSTEM -> ElectricBlue
                                shortcut.category == ShortcutCategory.NAVIGATION -> LuminousViolet
                                else -> CyanIce
                            }
                            GlassActionTile(
                                onClick = { onTriggerShortcut(shortcut) },
                                hapticsEnabled = hapticsEnabled,
                                accentColor = accent,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("shortcut_tile_${shortcut.id}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(accent.copy(alpha = 0.18f))
                                            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = shortcut.comboLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = accent,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                    }
                                    if (shortcut.requiresSafetyConfirm) {
                                        Icon(
                                            imageVector = Icons.Rounded.Lock,
                                            contentDescription = "Protected action",
                                            tint = RoseAlert,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = shortcut.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = shortcut.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            if (selectedCategory == "ALL" || selectedCategory == "CUSTOM") {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    SectionHeader(
                        title = "Custom Shortcuts",
                        subtitle = "Create your own multi-modifier Windows macros"
                    )
                }

                if (customShortcuts.isEmpty()) {
                    item {
                        GlassPanel(contentPadding = PaddingValues(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Terminal,
                                    contentDescription = null,
                                    tint = LuminousVioletBright,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "No custom shortcuts yet. Tap + to create a custom combination like Win + Shift + S or Ctrl + Alt + T.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(customShortcuts, key = { it.id }) { custom ->
                        GlassActionTile(
                            onClick = { onTriggerCustomShortcut(custom) },
                            hapticsEnabled = hapticsEnabled,
                            accentColor = EmeraldConnected,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_shortcut_${custom.id}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Bolt,
                                        contentDescription = null,
                                        tint = EmeraldConnected
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = custom.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary
                                        )
                                        if (custom.title != custom.comboLabel) {
                                            Text(
                                                text = custom.comboLabel,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = EmeraldConnected
                                            )
                                        }
                                    }
                                }
                                IconButton(
                                    onClick = { onDeleteCustomShortcut(custom.id) },
                                    modifier = Modifier.testTag("delete_custom_${custom.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Delete custom shortcut",
                                        tint = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                showCreatorPanel = !showCreatorPanel
                if (showCreatorPanel) {
                    scope.launch { listState.animateScrollToItem(0) }
                }
            },
            containerColor = ElectricBlue,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp)
                .testTag("add_custom_shortcut_fab")
        ) {
            Icon(
                imageVector = if (showCreatorPanel) Icons.Rounded.Close else Icons.Rounded.Add,
                contentDescription = "Create custom shortcut"
            )
        }
    }
}

@Composable
private fun CustomShortcutCreatorPanel(
    onDismiss: () -> Unit,
    onSave: (String, Int, Int, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var useCtrl by remember { mutableStateOf(true) }
    var useShift by remember { mutableStateOf(false) }
    var useAlt by remember { mutableStateOf(false) }
    var useWin by remember { mutableStateOf(false) }
    var selectedKeyIdx by remember { mutableIntStateOf(18) } // 'S'

    val keys = HidKeyCodes.selectableKeysForBuilder
    val chosenKey = keys[selectedKeyIdx]

    val modMask = (if (useCtrl) HidKeyCodes.MOD_LEFT_CTRL else 0) or
        (if (useShift) HidKeyCodes.MOD_LEFT_SHIFT else 0) or
        (if (useAlt) HidKeyCodes.MOD_LEFT_ALT else 0) or
        (if (useWin) HidKeyCodes.MOD_LEFT_GUI else 0)
    val comboPreview = WindowsShortcutsCatalog.formatModifierCombo(modMask, chosenKey.first)

    GlassPanel(
        elevated = true,
        accentGlow = ElectricBlue,
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "New Custom Shortcut",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = "Close creator", tint = TextSecondary)
            }
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Shortcut Name") },
            placeholder = { Text("e.g. Snipping Tool") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("custom_shortcut_name_input")
        )

        Spacer(modifier = Modifier.height(10.dp))
        Text("Modifiers", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = useCtrl,
                onClick = { useCtrl = !useCtrl },
                label = { Text("Ctrl") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = useShift,
                onClick = { useShift = !useShift },
                label = { Text("Shift") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = useAlt,
                onClick = { useAlt = !useAlt },
                label = { Text("Alt") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = useWin,
                onClick = { useWin = !useWin },
                label = { Text("Win") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Primary Key (${chosenKey.first})", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            itemsIndexed(keys) { idx, pair ->
                FilterChip(
                    selected = selectedKeyIdx == idx,
                    onClick = { selectedKeyIdx = idx },
                    label = { Text(pair.first) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(ElectricBlue.copy(alpha = 0.14f))
                    .border(1.dp, GlassBorderHighlight, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Preview: $comboPreview",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricBlueBright
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextSecondary)
                }
                Button(
                    onClick = { onSave(title, modMask, chosenKey.second, comboPreview) },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    modifier = Modifier.testTag("save_custom_shortcut_button")
                ) {
                    Text("Save")
                }
            }
        }
    }
}
