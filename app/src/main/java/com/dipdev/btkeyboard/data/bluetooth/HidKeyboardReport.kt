package com.dipdev.btkeyboard.data.bluetooth

/**
 * Represents an 8-byte HID keyboard input report.
 * Layout: [modifier, 0x00 (reserved), key1..key6]
 */
data class HidKeyboardReport(
    val modifier: Byte = 0,
    val key1: Byte = 0,
    val key2: Byte = 0,
    val key3: Byte = 0,
    val key4: Byte = 0,
    val key5: Byte = 0,
    val key6: Byte = 0
) {
    fun toByteArray(): ByteArray = byteArrayOf(modifier, 0x00, key1, key2, key3, key4, key5, key6)

    companion object {
        val EMPTY = HidKeyboardReport()
    }
}
