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

enum class MacroDeckProfile(val label: String) {
    WINDOWS("Windows"),
    BROWSER("Browser"),
    DEV_IDE("VS Code / Dev"),
    STREAMING("YouTube / Media"),
    POWERPOINT("PowerPoint")
}

sealed interface MacroActionStep {
    data class KeyCombo(val modifierMask: Int, val keyCode: Int) : MacroActionStep
    data class TypeString(val text: String) : MacroActionStep
    data class WaitMs(val durationMs: Long) : MacroActionStep
}

data class MultiStepMacro(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val steps: List<MacroActionStep>
)

data class CustomShortcut(
    val id: String,
    val title: String,
    val comboLabel: String,
    val modifierMask: Int,
    val keyCode: Int,
    val macroText: String? = null
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

    val appProfileDecks: Map<MacroDeckProfile, List<WindowsShortcut>> = mapOf(
        MacroDeckProfile.BROWSER to listOf(
            WindowsShortcut("br_new_tab", "New Tab", "Ctrl + T", "Open a fresh browser tab", HidKeyCodes.MOD_LEFT_CTRL, HidKeyCodes.KEY_T, ShortcutCategory.NAVIGATION),
            WindowsShortcut("br_close_tab", "Close Tab", "Ctrl + W", "Close current browser tab", HidKeyCodes.MOD_LEFT_CTRL, HidKeyCodes.KEY_W, ShortcutCategory.NAVIGATION),
            WindowsShortcut("br_reopen_tab", "Reopen Tab", "Ctrl + Shift + T", "Restore last closed tab", HidKeyCodes.MOD_LEFT_CTRL or HidKeyCodes.MOD_LEFT_SHIFT, HidKeyCodes.KEY_T, ShortcutCategory.NAVIGATION),
            WindowsShortcut("br_address", "Address Bar", "Ctrl + L", "Focus URL search bar", HidKeyCodes.MOD_LEFT_CTRL, HidKeyCodes.KEY_L, ShortcutCategory.NAVIGATION),
            WindowsShortcut("br_next_tab", "Next Tab", "Ctrl + Tab", "Jump to next open tab", HidKeyCodes.MOD_LEFT_CTRL, HidKeyCodes.KEY_TAB, ShortcutCategory.NAVIGATION),
            WindowsShortcut("br_refresh", "Hard Reload", "Ctrl + Shift + R", "Reload page bypassing cache", HidKeyCodes.MOD_LEFT_CTRL or HidKeyCodes.MOD_LEFT_SHIFT, HidKeyCodes.KEY_R, ShortcutCategory.SYSTEM)
        ),
        MacroDeckProfile.DEV_IDE to listOf(
            WindowsShortcut("ide_palette", "Command Palette", "Ctrl + Shift + P", "Open VS Code command palette", HidKeyCodes.MOD_LEFT_CTRL or HidKeyCodes.MOD_LEFT_SHIFT, HidKeyCodes.KEY_P, ShortcutCategory.SYSTEM),
            WindowsShortcut("ide_quick_open", "Quick Open File", "Ctrl + P", "Jump to any project file", HidKeyCodes.MOD_LEFT_CTRL, HidKeyCodes.KEY_P, ShortcutCategory.NAVIGATION),
            WindowsShortcut("ide_terminal", "Toggle Terminal", "Ctrl + `", "Show or hide integrated terminal", HidKeyCodes.MOD_LEFT_CTRL, HidKeyCodes.KEY_GRAVE, ShortcutCategory.SYSTEM),
            WindowsShortcut("ide_format", "Format Code", "Shift + Alt + F", "Auto-format active document", HidKeyCodes.MOD_LEFT_SHIFT or HidKeyCodes.MOD_LEFT_ALT, HidKeyCodes.KEY_F, ShortcutCategory.EDITING),
            WindowsShortcut("ide_comment", "Toggle Comment", "Ctrl + /", "Comment or uncomment lines", HidKeyCodes.MOD_LEFT_CTRL, HidKeyCodes.KEY_SLASH, ShortcutCategory.EDITING),
            WindowsShortcut("ide_save", "Save File", "Ctrl + S", "Save current editor buffer", HidKeyCodes.MOD_LEFT_CTRL, HidKeyCodes.KEY_S, ShortcutCategory.EDITING)
        ),
        MacroDeckProfile.STREAMING to listOf(
            WindowsShortcut("st_play", "Play / Pause", "K", "Pause or resume video playback", 0, HidKeyCodes.KEY_K, ShortcutCategory.NAVIGATION),
            WindowsShortcut("st_fullscreen", "Fullscreen", "F", "Toggle fullscreen video player", 0, HidKeyCodes.KEY_F, ShortcutCategory.NAVIGATION),
            WindowsShortcut("st_mute", "Mute Video", "M", "Mute or unmute player audio", 0, HidKeyCodes.KEY_M, ShortcutCategory.SYSTEM),
            WindowsShortcut("st_theater", "Theater Mode", "T", "Toggle wide theater view", 0, HidKeyCodes.KEY_T, ShortcutCategory.NAVIGATION),
            WindowsShortcut("st_back10", "Rewind 10s", "J", "Jump backward 10 seconds", 0, HidKeyCodes.KEY_J, ShortcutCategory.NAVIGATION),
            WindowsShortcut("st_fwd10", "Forward 10s", "L", "Jump forward 10 seconds", 0, HidKeyCodes.KEY_L, ShortcutCategory.NAVIGATION)
        ),
        MacroDeckProfile.POWERPOINT to listOf(
            WindowsShortcut("ppt_start", "Start Deck", "F5", "Start presentation from slide 1", 0, HidKeyCodes.KEY_F5, ShortcutCategory.SYSTEM),
            WindowsShortcut("ppt_current", "From Current", "Shift + F5", "Start from active slide", HidKeyCodes.MOD_LEFT_SHIFT, HidKeyCodes.KEY_F5, ShortcutCategory.SYSTEM),
            WindowsShortcut("ppt_black", "Black Screen", "B", "Toggle blank black display", 0, HidKeyCodes.KEY_B, ShortcutCategory.NAVIGATION),
            WindowsShortcut("ppt_white", "White Screen", "W", "Toggle blank white display", 0, HidKeyCodes.KEY_W, ShortcutCategory.NAVIGATION),
            WindowsShortcut("ppt_laser", "Laser Pointer", "Ctrl + L", "Switch cursor to laser pointer", HidKeyCodes.MOD_LEFT_CTRL, HidKeyCodes.KEY_L, ShortcutCategory.EDITING),
            WindowsShortcut("ppt_exit", "End Show", "Esc", "Exit active slide show", 0, HidKeyCodes.KEY_ESC, ShortcutCategory.SYSTEM)
        )
    )

    val builtInMultiStepMacros: List<MultiStepMacro> = listOf(
        MultiStepMacro(
            id = "macro_launch_terminal",
            title = "Open Windows Terminal",
            subtitle = "Win+R → wt → Enter",
            badge = "3 Steps",
            steps = listOf(
                MacroActionStep.KeyCombo(HidKeyCodes.MOD_LEFT_GUI, HidKeyCodes.KEY_R),
                MacroActionStep.WaitMs(320L),
                MacroActionStep.TypeString("wt"),
                MacroActionStep.WaitMs(80L),
                MacroActionStep.KeyCombo(0, HidKeyCodes.KEY_ENTER)
            )
        ),
        MultiStepMacro(
            id = "macro_launch_notepad",
            title = "Launch Notepad",
            subtitle = "Win+R → notepad → Enter",
            badge = "3 Steps",
            steps = listOf(
                MacroActionStep.KeyCombo(HidKeyCodes.MOD_LEFT_GUI, HidKeyCodes.KEY_R),
                MacroActionStep.WaitMs(320L),
                MacroActionStep.TypeString("notepad"),
                MacroActionStep.WaitMs(80L),
                MacroActionStep.KeyCombo(0, HidKeyCodes.KEY_ENTER)
            )
        ),
        MultiStepMacro(
            id = "macro_launch_calc",
            title = "Launch Calculator",
            subtitle = "Win+R → calc → Enter",
            badge = "3 Steps",
            steps = listOf(
                MacroActionStep.KeyCombo(HidKeyCodes.MOD_LEFT_GUI, HidKeyCodes.KEY_R),
                MacroActionStep.WaitMs(320L),
                MacroActionStep.TypeString("calc"),
                MacroActionStep.WaitMs(80L),
                MacroActionStep.KeyCombo(0, HidKeyCodes.KEY_ENTER)
            )
        ),
        MultiStepMacro(
            id = "macro_format_save",
            title = "Format & Save Code",
            subtitle = "Shift+Alt+F → Ctrl+S",
            badge = "2 Steps",
            steps = listOf(
                MacroActionStep.KeyCombo(HidKeyCodes.MOD_LEFT_SHIFT or HidKeyCodes.MOD_LEFT_ALT, HidKeyCodes.KEY_F),
                MacroActionStep.WaitMs(220L),
                MacroActionStep.KeyCombo(HidKeyCodes.MOD_LEFT_CTRL, HidKeyCodes.KEY_S)
            )
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
