package com.dipdev.btkeyboard.ui.screens.keyboard

/**
 * Maps characters and named keys to HID USB usage codes and modifier bytes.
 */
object KeyMap {

    // ── Modifier constants ────────────────────────────────────────────
    const val MOD_NONE: Byte = 0x00
    const val MOD_CTRL: Byte = 0x01
    const val MOD_SHIFT: Byte = 0x02
    const val MOD_ALT: Byte = 0x04
    const val MOD_GUI: Byte = 0x08  // Win / Cmd

    // ── Special key HID usage codes ──────────────────────────────────
    object Keys {
        const val ENTER: Byte = 0x28
        const val ESC: Byte = 0x29
        const val BACKSPACE: Byte = 0x2A
        const val TAB: Byte = 0x2B
        const val SPACE: Byte = 0x2C
        const val CAPS_LOCK: Byte = 0x39
        const val F1: Byte = 0x3A
        const val F2: Byte = 0x3B
        const val F3: Byte = 0x3C
        const val F4: Byte = 0x3D
        const val F5: Byte = 0x3E
        const val F6: Byte = 0x3F
        const val F7: Byte = 0x40
        const val F8: Byte = 0x41
        const val F9: Byte = 0x42
        const val F10: Byte = 0x43
        const val F11: Byte = 0x44
        const val F12: Byte = 0x45
        const val PRINT_SCREEN: Byte = 0x46
        const val SCROLL_LOCK: Byte = 0x47
        const val PAUSE: Byte = 0x48
        const val INSERT: Byte = 0x49
        const val HOME: Byte = 0x4A
        const val PAGE_UP: Byte = 0x4B
        const val DELETE: Byte = 0x4C
        const val END: Byte = 0x4D
        const val PAGE_DOWN: Byte = 0x4E
        const val RIGHT: Byte = 0x4F
        const val LEFT: Byte = 0x50
        const val DOWN: Byte = 0x51
        const val UP: Byte = 0x52
    }

    // ── Consumer Control usage IDs (media keys) ───────────────────────
    object Consumer {
        const val PLAY_PAUSE = 0x00CD
        const val STOP = 0x00B7
        const val NEXT_TRACK = 0x00B5
        const val PREV_TRACK = 0x00B6
        const val VOLUME_UP = 0x00E9
        const val VOLUME_DOWN = 0x00EA
        const val MUTE = 0x00E2
        const val BROWSER_HOME = 0x0223
    }

    // ── Character → (modifier, keyCode) map ──────────────────────────
    private val charMap: Map<Char, Pair<Byte, Byte>> = buildMap {
        // Lowercase letters a-z → HID codes 0x04-0x1D
        ('a'..'z').forEachIndexed { i, c -> put(c, MOD_NONE to (0x04 + i).toByte()) }
        // Uppercase letters A-Z → same codes but with SHIFT
        ('A'..'Z').forEachIndexed { i, c -> put(c, MOD_SHIFT to (0x04 + i).toByte()) }

        // Number row (unshifted)
        put('1', MOD_NONE to 0x1E); put('2', MOD_NONE to 0x1F); put('3', MOD_NONE to 0x20)
        put('4', MOD_NONE to 0x21); put('5', MOD_NONE to 0x22); put('6', MOD_NONE to 0x23)
        put('7', MOD_NONE to 0x24); put('8', MOD_NONE to 0x25); put('9', MOD_NONE to 0x26)
        put('0', MOD_NONE to 0x27)

        // Shift+number → symbols
        put('!', MOD_SHIFT to 0x1E); put('@', MOD_SHIFT to 0x1F); put('#', MOD_SHIFT to 0x20)
        put('$', MOD_SHIFT to 0x21); put('%', MOD_SHIFT to 0x22); put('^', MOD_SHIFT to 0x23)
        put('&', MOD_SHIFT to 0x24); put('*', MOD_SHIFT to 0x25); put('(', MOD_SHIFT to 0x26)
        put(')', MOD_SHIFT to 0x27)

        // Punctuation (unshifted)
        put('-', MOD_NONE to 0x2D); put('=', MOD_NONE to 0x2E)
        put('[', MOD_NONE to 0x2F); put(']', MOD_NONE to 0x30)
        put('\\', MOD_NONE to 0x31); put(';', MOD_NONE to 0x33)
        put('\'', MOD_NONE to 0x34); put('`', MOD_NONE to 0x35)
        put(',', MOD_NONE to 0x36); put('.', MOD_NONE to 0x37)
        put('/', MOD_NONE to 0x38)

        // Punctuation (shifted)
        put('_', MOD_SHIFT to 0x2D); put('+', MOD_SHIFT to 0x2E)
        put('{', MOD_SHIFT to 0x2F); put('}', MOD_SHIFT to 0x30)
        put('|', MOD_SHIFT to 0x31); put(':', MOD_SHIFT to 0x33)
        put('"', MOD_SHIFT to 0x34); put('~', MOD_SHIFT to 0x35)
        put('<', MOD_SHIFT to 0x36); put('>', MOD_SHIFT to 0x37)
        put('?', MOD_SHIFT to 0x38)

        // Whitespace
        put(' ', MOD_NONE to Keys.SPACE)
        put('\n', MOD_NONE to Keys.ENTER)
        put('\t', MOD_NONE to Keys.TAB)
    }

    /** Returns (modifier, keyCode) or null if the character cannot be mapped. */
    fun charToHid(char: Char): Pair<Byte, Byte>? = charMap[char]

    private infix fun Byte.to(that: Byte): Pair<Byte, Byte> = Pair(this, that)
    private infix fun Int.to(that: Byte): Pair<Byte, Byte> = Pair(this.toByte(), that)
}
