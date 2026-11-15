package com.example.domain.hid

data class HidKeyStroke(
    val modifierMask: Int,
    val keyCode: Int
)

/**
 * Standard USB HID Usage IDs (Page 0x07 Keyboard/Keypad & Page 0x0C Consumer Control).
 * Matches Windows 11 kbdhid.sys and hidclass.sys standard US-QWERTY scan mappings.
 */
object HidKeyCodes {
    // Modifier Bitmasks (Report Byte 0)
    const val MOD_NONE = 0x00
    const val MOD_LEFT_CTRL = 0x01
    const val MOD_LEFT_SHIFT = 0x02
    const val MOD_LEFT_ALT = 0x04
    const val MOD_LEFT_GUI = 0x08 // Windows key
    const val MOD_RIGHT_CTRL = 0x10
    const val MOD_RIGHT_SHIFT = 0x20
    const val MOD_RIGHT_ALT = 0x40
    const val MOD_RIGHT_GUI = 0x80

    // Mouse Button Bitmasks
    const val MOUSE_NONE = 0x00
    const val MOUSE_LEFT = 0x01
    const val MOUSE_RIGHT = 0x02
    const val MOUSE_MIDDLE = 0x04
    const val MOUSE_BACK = 0x08
    const val MOUSE_FORWARD = 0x10

    // Letters A-Z (0x04 - 0x1D)
    const val KEY_A = 0x04
    const val KEY_B = 0x05
    const val KEY_C = 0x06
    const val KEY_D = 0x07
    const val KEY_E = 0x08
    const val KEY_F = 0x09
    const val KEY_G = 0x0A
    const val KEY_H = 0x0B
    const val KEY_I = 0x0C
    const val KEY_J = 0x0D
    const val KEY_K = 0x0E
    const val KEY_L = 0x0F
    const val KEY_M = 0x10
    const val KEY_N = 0x11
    const val KEY_O = 0x12
    const val KEY_P = 0x13
    const val KEY_Q = 0x14
    const val KEY_R = 0x15
    const val KEY_S = 0x16
    const val KEY_T = 0x17
    const val KEY_U = 0x18
    const val KEY_V = 0x19
    const val KEY_W = 0x1A
    const val KEY_X = 0x1B
    const val KEY_Y = 0x1C
    const val KEY_Z = 0x1D

    // Number Row (0x1E - 0x27)
    const val KEY_1 = 0x1E
    const val KEY_2 = 0x1F
    const val KEY_3 = 0x20
    const val KEY_4 = 0x21
    const val KEY_5 = 0x22
    const val KEY_6 = 0x23
    const val KEY_7 = 0x24
    const val KEY_8 = 0x25
    const val KEY_9 = 0x26
    const val KEY_0 = 0x27

    // Control & Punctuation
    const val KEY_ENTER = 0x28
    const val KEY_ESC = 0x29
    const val KEY_BACKSPACE = 0x2A
    const val KEY_TAB = 0x2B
    const val KEY_SPACE = 0x2C
    const val KEY_MINUS = 0x2D       // - _
    const val KEY_EQUAL = 0x2E       // = +
    const val KEY_LEFT_BRACKET = 0x2F // [ {
    const val KEY_RIGHT_BRACKET = 0x30 // ] }
    const val KEY_BACKSLASH = 0x31   // \ |
    const val KEY_SEMICOLON = 0x33   // ; :
    const val KEY_APOSTROPHE = 0x34  // ' "
    const val KEY_GRAVE = 0x35       // ` ~
    const val KEY_COMMA = 0x36       // , <
    const val KEY_PERIOD = 0x37      // . >
    const val KEY_SLASH = 0x38       // / ?
    const val KEY_CAPS_LOCK = 0x39

    // Function Keys F1-F12
    const val KEY_F1 = 0x3A
    const val KEY_F2 = 0x3B
    const val KEY_F3 = 0x3C
    const val KEY_F4 = 0x3D
    const val KEY_F5 = 0x3E
    const val KEY_F6 = 0x3F
    const val KEY_F7 = 0x40
    const val KEY_F8 = 0x41
    const val KEY_F9 = 0x42
    const val KEY_F10 = 0x43
    const val KEY_F11 = 0x44
    const val KEY_F12 = 0x45

    // System & Navigation Keys
    const val KEY_PRINT_SCREEN = 0x46
    const val KEY_SCROLL_LOCK = 0x47
    const val KEY_PAUSE = 0x48
    const val KEY_INSERT = 0x49
    const val KEY_HOME = 0x4A
    const val KEY_PAGE_UP = 0x4B
    const val KEY_DELETE = 0x4C
    const val KEY_END = 0x4D
    const val KEY_PAGE_DOWN = 0x4E
    const val KEY_RIGHT = 0x4F
    const val KEY_LEFT = 0x50
    const val KEY_DOWN = 0x51
    const val KEY_UP = 0x52
    const val KEY_APP_MENU = 0x65

    // Consumer Control Usage IDs (Page 0x0C)
    const val CONSUMER_NONE = 0x0000
    const val CONSUMER_NEXT_TRACK = 0x00B5
    const val CONSUMER_PREV_TRACK = 0x00B6
    const val CONSUMER_STOP = 0x00B7
    const val CONSUMER_PLAY_PAUSE = 0x00CD
    const val CONSUMER_MUTE = 0x00E2
    const val CONSUMER_VOL_UP = 0x00E9
    const val CONSUMER_VOL_DOWN = 0x00EA

    private val asciiMap: Map<Char, HidKeyStroke> = buildMap {
        // Lowercase a-z
        for (i in 0 until 26) {
            val lower = ('a'.code + i).toChar()
            val upper = ('A'.code + i).toChar()
            val code = KEY_A + i
            put(lower, HidKeyStroke(MOD_NONE, code))
            put(upper, HidKeyStroke(MOD_LEFT_SHIFT, code))
        }
        // Digits 1-9, 0
        val digits = listOf(
            '1' to KEY_1, '2' to KEY_2, '3' to KEY_3, '4' to KEY_4, '5' to KEY_5,
            '6' to KEY_6, '7' to KEY_7, '8' to KEY_8, '9' to KEY_9, '0' to KEY_0
        )
        digits.forEach { (ch, code) -> put(ch, HidKeyStroke(MOD_NONE, code)) }

        // Shifted digit symbols
        val shiftedDigits = listOf(
            '!' to KEY_1, '@' to KEY_2, '#' to KEY_3, '$' to KEY_4, '%' to KEY_5,
            '^' to KEY_6, '&' to KEY_7, '*' to KEY_8, '(' to KEY_9, ')' to KEY_0
        )
        shiftedDigits.forEach { (ch, code) -> put(ch, HidKeyStroke(MOD_LEFT_SHIFT, code)) }

        // Whitespace & control
        put('\n', HidKeyStroke(MOD_NONE, KEY_ENTER))
        put('\r', HidKeyStroke(MOD_NONE, KEY_ENTER))
        put('\t', HidKeyStroke(MOD_NONE, KEY_TAB))
        put(' ', HidKeyStroke(MOD_NONE, KEY_SPACE))

        // Punctuation (unshifted & shifted)
        put('-', HidKeyStroke(MOD_NONE, KEY_MINUS))
        put('_', HidKeyStroke(MOD_LEFT_SHIFT, KEY_MINUS))
        put('=', HidKeyStroke(MOD_NONE, KEY_EQUAL))
        put('+', HidKeyStroke(MOD_LEFT_SHIFT, KEY_EQUAL))
        put('[', HidKeyStroke(MOD_NONE, KEY_LEFT_BRACKET))
        put('{', HidKeyStroke(MOD_LEFT_SHIFT, KEY_LEFT_BRACKET))
        put(']', HidKeyStroke(MOD_NONE, KEY_RIGHT_BRACKET))
        put('}', HidKeyStroke(MOD_LEFT_SHIFT, KEY_RIGHT_BRACKET))
        put('\\', HidKeyStroke(MOD_NONE, KEY_BACKSLASH))
        put('|', HidKeyStroke(MOD_LEFT_SHIFT, KEY_BACKSLASH))
        put(';', HidKeyStroke(MOD_NONE, KEY_SEMICOLON))
        put(':', HidKeyStroke(MOD_LEFT_SHIFT, KEY_SEMICOLON))
        put('\'', HidKeyStroke(MOD_NONE, KEY_APOSTROPHE))
        put('"', HidKeyStroke(MOD_LEFT_SHIFT, KEY_APOSTROPHE))
        put('`', HidKeyStroke(MOD_NONE, KEY_GRAVE))
        put('~', HidKeyStroke(MOD_LEFT_SHIFT, KEY_GRAVE))
        put(',', HidKeyStroke(MOD_NONE, KEY_COMMA))
        put('<', HidKeyStroke(MOD_LEFT_SHIFT, KEY_COMMA))
        put('.', HidKeyStroke(MOD_NONE, KEY_PERIOD))
        put('>', HidKeyStroke(MOD_LEFT_SHIFT, KEY_PERIOD))
        put('/', HidKeyStroke(MOD_NONE, KEY_SLASH))
        put('?', HidKeyStroke(MOD_LEFT_SHIFT, KEY_SLASH))
    }

    fun charToHidKey(ch: Char): HidKeyStroke? = asciiMap[ch]

    val selectableKeysForBuilder: List<Pair<String, Int>> = listOf(
        "A" to KEY_A, "B" to KEY_B, "C" to KEY_C, "D" to KEY_D, "E" to KEY_E,
        "F" to KEY_F, "G" to KEY_G, "H" to KEY_H, "I" to KEY_I, "J" to KEY_J,
        "K" to KEY_K, "L" to KEY_L, "M" to KEY_M, "N" to KEY_N, "O" to KEY_O,
        "P" to KEY_P, "Q" to KEY_Q, "R" to KEY_R, "S" to KEY_S, "T" to KEY_T,
        "U" to KEY_U, "V" to KEY_V, "W" to KEY_W, "X" to KEY_X, "Y" to KEY_Y,
        "Z" to KEY_Z,
        "0" to KEY_0, "1" to KEY_1, "2" to KEY_2, "3" to KEY_3, "4" to KEY_4,
        "5" to KEY_5, "6" to KEY_6, "7" to KEY_7, "8" to KEY_8, "9" to KEY_9,
        "Tab" to KEY_TAB, "Enter" to KEY_ENTER, "Esc" to KEY_ESC, "Space" to KEY_SPACE,
        "Delete" to KEY_DELETE, "Backspace" to KEY_BACKSPACE,
        "Left" to KEY_LEFT, "Right" to KEY_RIGHT, "Up" to KEY_UP, "Down" to KEY_DOWN,
        "Home" to KEY_HOME, "End" to KEY_END, "PgUp" to KEY_PAGE_UP, "PgDn" to KEY_PAGE_DOWN,
        "F1" to KEY_F1, "F2" to KEY_F2, "F3" to KEY_F3, "F4" to KEY_F4,
        "F5" to KEY_F5, "F6" to KEY_F6, "F7" to KEY_F7, "F8" to KEY_F8,
        "F9" to KEY_F9, "F10" to KEY_F10, "F11" to KEY_F11, "F12" to KEY_F12,
        "PrtSc" to KEY_PRINT_SCREEN
    )
}
