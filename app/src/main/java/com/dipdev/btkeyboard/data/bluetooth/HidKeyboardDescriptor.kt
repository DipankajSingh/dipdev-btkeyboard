package com.dipdev.btkeyboard.data.bluetooth

/**
 * HID Report Descriptor for:
 *   Report ID 1 — Standard keyboard (modifier + 6 key codes = 8 bytes)
 *   Report ID 2 — Consumer Control (media keys, 2 bytes)
 *   Report ID 3 — Mouse/Trackpad (3 buttons + X, Y = 3 bytes)
 */
object HidKeyboardDescriptor {

    const val ID_KEYBOARD: Int = 1
    const val ID_CONSUMER: Int = 2
    const val ID_MOUSE: Int = 3

    val DESCRIPTOR: ByteArray = byteArrayOf(
        // ── Keyboard Report (ID 1) ──────────────────────────────────────
        0x05, 0x01,                    // Usage Page (Generic Desktop Ctrls)
        0x09, 0x06,                    // Usage (Keyboard)
        0xA1.toByte(), 0x01,           // Collection (Application)
        0x85.toByte(), 0x01,           //   Report ID (1)

        // Modifier keys — 8 bits, one per modifier key
        0x05, 0x07,                    //   Usage Page (Keyboard/Keypad)
        0x19, 0xE0.toByte(),           //   Usage Min (0xE0 = Left Ctrl)
        0x29, 0xE7.toByte(),           //   Usage Max (0xE7 = Right GUI)
        0x15, 0x00,                    //   Logical Min (0)
        0x25, 0x01,                    //   Logical Max (1)
        0x75, 0x01,                    //   Report Size (1)
        0x95.toByte(), 0x08,           //   Report Count (8)
        0x81.toByte(), 0x02,           //   Input (Data, Var, Abs) — modifier byte

        // Reserved byte
        0x95.toByte(), 0x01,           //   Report Count (1)
        0x75, 0x08,                    //   Report Size (8)
        0x81.toByte(), 0x01,           //   Input (Const) — reserved

        // LED Output (Num Lock, Caps, Scroll, Compose, Kana)
        0x95.toByte(), 0x05,           //   Report Count (5)
        0x75, 0x01,                    //   Report Size (1)
        0x05, 0x08,                    //   Usage Page (LEDs)
        0x19, 0x01,                    //   Usage Min (Num Lock)
        0x29, 0x05,                    //   Usage Max (Kana)
        0x91.toByte(), 0x02,           //   Output (Data, Var, Abs)
        0x95.toByte(), 0x01,           //   Report Count (1)
        0x75, 0x03,                    //   Report Size (3) — LED padding
        0x91.toByte(), 0x01,           //   Output (Const)

        // Key array — 6 simultaneous key codes
        0x95.toByte(), 0x06,           //   Report Count (6)
        0x75, 0x08,                    //   Report Size (8)
        0x15, 0x00,                    //   Logical Min (0)
        0x25, 0x65,                    //   Logical Max (101)
        0x05, 0x07,                    //   Usage Page (Keyboard/Keypad)
        0x19, 0x00,                    //   Usage Min (No Event)
        0x29, 0x65,                    //   Usage Max (Keyboard Application)
        0x81.toByte(), 0x00,           //   Input (Data, Array, Abs) — key codes

        0xC0.toByte(),                 // End Collection

        // ── Consumer Control Report (ID 2) ────────────────────────────
        0x05, 0x0C,                    // Usage Page (Consumer)
        0x09, 0x01,                    // Usage (Consumer Control)
        0xA1.toByte(), 0x01,           // Collection (Application)
        0x85.toByte(), 0x02,           //   Report ID (2)
        0x19, 0x00,                    //   Usage Min (0)
        0x2A, 0x9C.toByte(), 0x02,    //   Usage Max (0x029C)
        0x15, 0x00,                    //   Logical Min (0)
        0x26, 0x9C.toByte(), 0x02,    //   Logical Max (0x029C)
        0x75, 0x10,                    //   Report Size (16)
        0x95.toByte(), 0x01,           //   Report Count (1)
        0xC0.toByte(),                 // End Collection

        // ── Mouse Report (ID 3) ───────────────────────────────────────
        0x05, 0x01,                    // Usage Page (Generic Desktop)
        0x09, 0x02,                    // Usage (Mouse)
        0xA1.toByte(), 0x01,           // Collection (Application)
        0x85.toByte(), 0x03,           //   Report ID (3)
        0x09, 0x01,                    //   Usage (Pointer)
        0xA1.toByte(), 0x00,           //   Collection (Physical)

        // Buttons (Left, Right, Middle) — 3 bits
        0x05, 0x09,                    //     Usage Page (Buttons)
        0x19, 0x01,                    //     Usage Min (1)
        0x29, 0x03,                    //     Usage Max (3)
        0x15, 0x00,                    //     Logical Min (0)
        0x25, 0x01,                    //     Logical Max (1)
        0x95.toByte(), 0x03,           //     Report Count (3)
        0x75, 0x01,                    //     Report Size (1)
        0x81.toByte(), 0x02,           //     Input (Data, Var, Abs)

        // 5 padding bits
        0x95.toByte(), 0x01,           //     Report Count (1)
        0x75, 0x05,                    //     Report Size (5)
        0x81.toByte(), 0x03,           //     Input (Const, Var, Abs)

        // X and Y coords
        0x05, 0x01,                    //     Usage Page (Generic Desktop)
        0x09, 0x30,                    //     Usage (X)
        0x09, 0x31,                    //     Usage (Y)
        0x15, 0x81.toByte(),           //     Logical Min (-127)
        0x25, 0x7F,                    //     Logical Max (127)
        0x75, 0x08,                    //     Report Size (8)
        0x95.toByte(), 0x02,           //     Report Count (2)
        0x81.toByte(), 0x06,           //     Input (Data, Var, Rel)

        0xC0.toByte(),                 //   End Collection
        0xC0.toByte()                  // End Collection
    )
}
