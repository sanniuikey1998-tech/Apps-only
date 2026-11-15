package com.example.domain.hid

enum class ShortcutCategory {
    SYSTEM,
    NAVIGATION,
    EDITING
}

data class WindowsShortcut(
    val id: String,
    val title: String,
    val comboLabel: String,
    val description: String,
    val modifierMask: Int,
    val keyCode: Int,
    val category: ShortcutCategory,
    val requiresSafetyConfirm: Boolean = false
)

data class CustomShortcut(
    val id: String,
    val title: String,
    val comboLabel: String,
    val modifierMask: Int,
    val keyCode: Int
)

enum class FourFingerAction(val label: String, val comboHint: String) {
    SWITCH_DESKTOP_NEXT("Next Desktop", "Ctrl + Win + Right"),
    SWITCH_DESKTOP_PREV("Prev Desktop", "Ctrl + Win + Left"),
    TASK_VIEW("Task View", "Win + Tab"),
    SHOW_DESKTOP("Show Desktop", "Win + D"),
    SNIP_SCREEN("Screenshot Snip", "Win + Shift + S"),
    TASK_MANAGER("Task Manager", "Ctrl + Shift + Esc"),
    PLAY_PAUSE("Play / Pause", "Media Play/Pause"),
    LOCK_PC("Lock PC", "Win + L"),
    DISABLED("Disabled", "No action")
}

object WindowsShortcutsCatalog {
    val builtInShortcuts: List<WindowsShortcut> = listOf(
        WindowsShortcut(
            id = "win_d",
            title = "Show Desktop",
            comboLabel = "Win + D",
            description = "Minimize or restore all open windows",
            modifierMask = HidKeyCodes.MOD_LEFT_GUI,
            keyCode = HidKeyCodes.KEY_D,
            category = ShortcutCategory.SYSTEM
        ),
        WindowsShortcut(
            id = "alt_tab",
            title = "Switch Apps",
            comboLabel = "Alt + Tab",
            description = "Cycle to the previous active window",
            modifierMask = HidKeyCodes.MOD_LEFT_ALT,
            keyCode = HidKeyCodes.KEY_TAB,
            category = ShortcutCategory.NAVIGATION
        ),
        WindowsShortcut(
            id = "win_tab",
            title = "Task View",
            comboLabel = "Win + Tab",
            description = "Open Windows 11 virtual desktops & timeline",
            modifierMask = HidKeyCodes.MOD_LEFT_GUI,
            keyCode = HidKeyCodes.KEY_TAB,
            category = ShortcutCategory.NAVIGATION
        ),
        WindowsShortcut(
            id = "win_e",
            title = "File Explorer",
            comboLabel = "Win + E",
            description = "Launch Windows File Explorer",
            modifierMask = HidKeyCodes.MOD_LEFT_GUI,
            keyCode = HidKeyCodes.KEY_E,
            category = ShortcutCategory.SYSTEM
        ),
        WindowsShortcut(
            id = "win_r",
            title = "Run Dialog",
            comboLabel = "Win + R",
            description = "Open Windows Run command prompt",
            modifierMask = HidKeyCodes.MOD_LEFT_GUI,
            keyCode = HidKeyCodes.KEY_R,
            category = ShortcutCategory.SYSTEM
        ),
        WindowsShortcut(
            id = "ctrl_shift_esc",
            title = "Task Manager",
            comboLabel = "Ctrl + Shift + Esc",
            description = "Open Windows Task Manager directly",
            modifierMask = HidKeyCodes.MOD_LEFT_CTRL or HidKeyCodes.MOD_LEFT_SHIFT,
            keyCode = HidKeyCodes.KEY_ESC,
            category = ShortcutCategory.SYSTEM
        ),
        WindowsShortcut(
            id = "win_l",
            title = "Lock PC",
            comboLabel = "Win + L",
            description = "Lock your Windows 11 session immediately",
            modifierMask = HidKeyCodes.MOD_LEFT_GUI,
            keyCode = HidKeyCodes.KEY_L,
            category = ShortcutCategory.SYSTEM,
            requiresSafetyConfirm = true
        ),
        WindowsShortcut(
            id = "alt_f4",
            title = "Close Window",
            comboLabel = "Alt + F4",
            description = "Close the active application window",
            modifierMask = HidKeyCodes.MOD_LEFT_ALT,
            keyCode = HidKeyCodes.KEY_F4,
            category = ShortcutCategory.NAVIGATION,
            requiresSafetyConfirm = true
        ),
        WindowsShortcut(
            id = "ctrl_c",
            title = "Copy",
            comboLabel = "Ctrl + C",
            description = "Copy selected item to clipboard",
            modifierMask = HidKeyCodes.MOD_LEFT_CTRL,
            keyCode = HidKeyCodes.KEY_C,
            category = ShortcutCategory.EDITING
        ),
        WindowsShortcut(
            id = "ctrl_v",
            title = "Paste",
            comboLabel = "Ctrl + V",
            description = "Paste from clipboard",
            modifierMask = HidKeyCodes.MOD_LEFT_CTRL,
            keyCode = HidKeyCodes.KEY_V,
            category = ShortcutCategory.EDITING
        ),
        WindowsShortcut(
            id = "ctrl_x",
            title = "Cut",
            comboLabel = "Ctrl + X",
            description = "Cut selected content",
            modifierMask = HidKeyCodes.MOD_LEFT_CTRL,
            keyCode = HidKeyCodes.KEY_X,
            category = ShortcutCategory.EDITING
        ),
        WindowsShortcut(
            id = "ctrl_z",
            title = "Undo",
            comboLabel = "Ctrl + Z",
            description = "Undo last action",
            modifierMask = HidKeyCodes.MOD_LEFT_CTRL,
            keyCode = HidKeyCodes.KEY_Z,
            category = ShortcutCategory.EDITING
        ),
        WindowsShortcut(
            id = "ctrl_a",
            title = "Select All",
            comboLabel = "Ctrl + A",
            description = "Select all items in active focus",
            modifierMask = HidKeyCodes.MOD_LEFT_CTRL,
            keyCode = HidKeyCodes.KEY_A,
            category = ShortcutCategory.EDITING
        )
    )

    fun formatModifierCombo(modifierMask: Int, keyLabel: String): String {
        val parts = mutableListOf<String>()
        if ((modifierMask and HidKeyCodes.MOD_LEFT_CTRL) != 0) parts.add("Ctrl")
        if ((modifierMask and HidKeyCodes.MOD_LEFT_SHIFT) != 0) parts.add("Shift")
        if ((modifierMask and HidKeyCodes.MOD_LEFT_ALT) != 0) parts.add("Alt")
        if ((modifierMask and HidKeyCodes.MOD_LEFT_GUI) != 0) parts.add("Win")
        parts.add(keyLabel)
        return parts.joinToString(" + ")
    }
}
