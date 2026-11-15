package com.example.domain.hid

/**
 * Composite Bluetooth HID Report Descriptor for Windows 11 / macOS / Linux zero-driver compatibility.
 *
 * Exposes 3 distinct HID Reports over a single Bluetooth Classic L2CAP HID registration:
 * - Report ID 1: Standard 6-Key Rollover Keyboard (8 bytes IN, 1 byte LEDs OUT)
 * - Report ID 2: Precision 5-Button Mouse + X/Y + Vertical Wheel + Horizontal AC Pan (5 bytes IN)
 * - Report ID 3: Consumer Control Media & Volume Keys (2 bytes IN, 16-bit usage)
 */
object HidReportDescriptor {
    const val REPORT_ID_KEYBOARD: Int = 1
    const val REPORT_ID_MOUSE: Int = 2
    const val REPORT_ID_CONSUMER: Int = 3

    val DESCRIPTOR: ByteArray = byteArrayOf(
        // ========================
        // Report ID 1: Keyboard
        // ========================
        0x05.toByte(), 0x01.toByte(),       // Usage Page (Generic Desktop)
        0x09.toByte(), 0x06.toByte(),       // Usage (Keyboard)
        0xA1.toByte(), 0x01.toByte(),       // Collection (Application)
        0x85.toByte(), REPORT_ID_KEYBOARD.toByte(), // Report ID (1)
        0x05.toByte(), 0x07.toByte(),       //   Usage Page (Keyboard/Keypad)
        0x19.toByte(), 0xE0.toByte(),       //   Usage Minimum (Left Control)
        0x29.toByte(), 0xE7.toByte(),       //   Usage Maximum (Right GUI)
        0x15.toByte(), 0x00.toByte(),       //   Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(),       //   Logical Maximum (1)
        0x75.toByte(), 0x01.toByte(),       //   Report Size (1)
        0x95.toByte(), 0x08.toByte(),       //   Report Count (8)
        0x81.toByte(), 0x02.toByte(),       //   Input (Data, Var, Abs) -> Byte 0: Modifiers
        0x95.toByte(), 0x01.toByte(),       //   Report Count (1)
        0x75.toByte(), 0x08.toByte(),       //   Report Size (8)
        0x81.toByte(), 0x03.toByte(),       //   Input (Const, Var, Abs) -> Byte 1: Reserved
        0x95.toByte(), 0x05.toByte(),       //   Report Count (5)
        0x75.toByte(), 0x01.toByte(),       //   Report Size (1)
        0x05.toByte(), 0x08.toByte(),       //   Usage Page (LEDs)
        0x19.toByte(), 0x01.toByte(),       //   Usage Minimum (Num Lock)
        0x29.toByte(), 0x05.toByte(),       //   Usage Maximum (Kana)
        0x91.toByte(), 0x02.toByte(),       //   Output (Data, Var, Abs) -> LED state
        0x95.toByte(), 0x01.toByte(),       //   Report Count (1)
        0x75.toByte(), 0x03.toByte(),       //   Report Size (3)
        0x91.toByte(), 0x03.toByte(),       //   Output (Const, Var, Abs) -> Padding
        0x95.toByte(), 0x06.toByte(),       //   Report Count (6)
        0x75.toByte(), 0x08.toByte(),       //   Report Size (8)
        0x15.toByte(), 0x00.toByte(),       //   Logical Minimum (0)
        0x26.toByte(), 0xFF.toByte(), 0x00.toByte(), // Logical Maximum (255)
        0x05.toByte(), 0x07.toByte(),       //   Usage Page (Keyboard/Keypad)
        0x19.toByte(), 0x00.toByte(),       //   Usage Minimum (0)
        0x2A.toByte(), 0xFF.toByte(), 0x00.toByte(), // Usage Maximum (255)
        0x81.toByte(), 0x00.toByte(),       //   Input (Data, Ary, Abs) -> Bytes 2..7: Keys
        0xC0.toByte(),                      // End Collection

        // ========================
        // Report ID 2: Mouse + V-Wheel + H-Pan
        // ========================
        0x05.toByte(), 0x01.toByte(),       // Usage Page (Generic Desktop)
        0x09.toByte(), 0x02.toByte(),       // Usage (Mouse)
        0xA1.toByte(), 0x01.toByte(),       // Collection (Application)
        0x85.toByte(), REPORT_ID_MOUSE.toByte(), // Report ID (2)
        0x09.toByte(), 0x01.toByte(),       //   Usage (Pointer)
        0xA1.toByte(), 0x00.toByte(),       //   Collection (Physical)
        0x05.toByte(), 0x09.toByte(),       //     Usage Page (Button)
        0x19.toByte(), 0x01.toByte(),       //     Usage Minimum (Button 1 - Left)
        0x29.toByte(), 0x05.toByte(),       //     Usage Maximum (Button 5 - Forward)
        0x15.toByte(), 0x00.toByte(),       //     Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(),       //     Logical Maximum (1)
        0x95.toByte(), 0x05.toByte(),       //     Report Count (5)
        0x75.toByte(), 0x01.toByte(),       //     Report Size (1)
        0x81.toByte(), 0x02.toByte(),       //     Input (Data, Var, Abs)
        0x95.toByte(), 0x01.toByte(),       //     Report Count (1)
        0x75.toByte(), 0x03.toByte(),       //     Report Size (3)
        0x81.toByte(), 0x03.toByte(),       //     Input (Const, Var, Abs) -> Byte 0
        0x05.toByte(), 0x01.toByte(),       //     Usage Page (Generic Desktop)
        0x09.toByte(), 0x30.toByte(),       //     Usage (X)
        0x09.toByte(), 0x31.toByte(),       //     Usage (Y)
        0x09.toByte(), 0x38.toByte(),       //     Usage (Wheel)
        0x15.toByte(), 0x81.toByte(),       //     Logical Minimum (-127)
        0x25.toByte(), 0x7F.toByte(),       //     Logical Maximum (127)
        0x75.toByte(), 0x08.toByte(),       //     Report Size (8)
        0x95.toByte(), 0x03.toByte(),       //     Report Count (3)
        0x81.toByte(), 0x06.toByte(),       //     Input (Data, Var, Rel) -> Bytes 1..3
        0x05.toByte(), 0x0C.toByte(),       //     Usage Page (Consumer)
        0x0A.toByte(), 0x38.toByte(), 0x02.toByte(), // Usage (AC Pan)
        0x15.toByte(), 0x81.toByte(),       //     Logical Minimum (-127)
        0x25.toByte(), 0x7F.toByte(),       //     Logical Maximum (127)
        0x75.toByte(), 0x08.toByte(),       //     Report Size (8)
        0x95.toByte(), 0x01.toByte(),       //     Report Count (1)
        0x81.toByte(), 0x06.toByte(),       //     Input (Data, Var, Rel) -> Byte 4
        0xC0.toByte(),                      //   End Collection
        0xC0.toByte(),                      // End Collection

        // ========================
        // Report ID 3: Consumer Control (Media & Volume)
        // ========================
        0x05.toByte(), 0x0C.toByte(),       // Usage Page (Consumer)
        0x09.toByte(), 0x01.toByte(),       // Usage (Consumer Control)
        0xA1.toByte(), 0x01.toByte(),       // Collection (Application)
        0x85.toByte(), REPORT_ID_CONSUMER.toByte(), // Report ID (3)
        0x15.toByte(), 0x00.toByte(),       //   Logical Minimum (0)
        0x26.toByte(), 0x9C.toByte(), 0x02.toByte(), // Logical Maximum (0x029C)
        0x19.toByte(), 0x00.toByte(),       //   Usage Minimum (0)
        0x2A.toByte(), 0x9C.toByte(), 0x02.toByte(), // Usage Maximum (0x029C)
        0x75.toByte(), 0x10.toByte(),       //   Report Size (16)
        0x95.toByte(), 0x01.toByte(),       //   Report Count (1)
        0x81.toByte(), 0x00.toByte(),       //   Input (Data, Ary, Abs) -> Bytes 0..1
        0xC0.toByte()                       // End Collection
    )

    fun buildKeyboardReport(
        modifierMask: Int,
        keyCodes: List<Int> = emptyList()
    ): ByteArray {
        val report = ByteArray(8)
        report[0] = (modifierMask and 0xFF).toByte()
        report[1] = 0x00
        keyCodes.take(6).forEachIndexed { index, code ->
            report[2 + index] = (code and 0xFF).toByte()
        }
        return report
    }

    fun buildMouseReport(
        buttonsMask: Int,
        deltaX: Int,
        deltaY: Int,
        verticalWheel: Int = 0,
        horizontalPan: Int = 0
    ): ByteArray {
        return byteArrayOf(
            (buttonsMask and 0x1F).toByte(),
            deltaX.coerceIn(-127, 127).toByte(),
            deltaY.coerceIn(-127, 127).toByte(),
            verticalWheel.coerceIn(-127, 127).toByte(),
            horizontalPan.coerceIn(-127, 127).toByte()
        )
    }

    fun buildConsumerReport(usageCode: Int): ByteArray {
        return byteArrayOf(
            (usageCode and 0xFF).toByte(),
            ((usageCode shr 8) and 0xFF).toByte()
        )
    }
}
