package com.dipdev.btkeyboard.data.bluetooth

/**
 * HID Mouse Report Data
 * ID 3: 3 bytes
 * Byte 0: Buttons (0x01 = Left, 0x02 = Right, 0x04 = Middle)
 * Byte 1: X displacement (-127 to +127)
 * Byte 2: Y displacement (-127 to +127)
 */
data class HidMouseReport(
    val buttons: Byte = 0,
    val dx: Byte = 0,
    val dy: Byte = 0
) {
    fun toByteArray(): ByteArray = byteArrayOf(buttons, dx, dy)

    companion object {
        val EMPTY = HidMouseReport()
    }
}
