package com.example

import com.example.data.BluetoothHidController
import com.example.data.PreferencesRepository
import com.example.data.WifiDiscoveryRepository
import com.example.domain.hid.CustomShortcut
import com.example.domain.hid.HidKeyCodes
import com.example.domain.hid.HidReportDescriptor
import com.example.domain.hid.TouchpadMath
import com.example.domain.hid.WindowsShortcutsCatalog
import com.example.domain.model.PairedPcDevice
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.Inet4Address
import java.net.InetAddress

class HidProtocolTest {

    @Test
    fun `keyboard report builds 8 bytes with modifier and 6-key rollover`() {
        val report = HidReportDescriptor.buildKeyboardReport(
            modifierMask = HidKeyCodes.MOD_LEFT_CTRL or HidKeyCodes.MOD_LEFT_SHIFT,
            keyCodes = listOf(HidKeyCodes.KEY_ESC, HidKeyCodes.KEY_A, 1, 2, 3, 4, 5)
        )
        assertEquals(8, report.size)
        assertEquals(0x03.toByte(), report[0])
        assertEquals(0x00.toByte(), report[1])
        assertEquals(HidKeyCodes.KEY_ESC.toByte(), report[2])
        assertEquals(HidKeyCodes.KEY_A.toByte(), report[3])
        // 7th key is capped by 6KRO
        assertEquals(4.toByte(), report[7])
    }

    @Test
    fun `mouse report clamps deltas and packs 5 buttons plus wheel and pan`() {
        val report = HidReportDescriptor.buildMouseReport(
            buttonsMask = HidKeyCodes.MOUSE_LEFT or HidKeyCodes.MOUSE_RIGHT,
            deltaX = 500,
            deltaY = -300,
            verticalWheel = 3,
            horizontalPan = -2
        )
        assertEquals(5, report.size)
        assertEquals(0x03.toByte(), report[0])
        assertEquals(127.toByte(), report[1])
        assertEquals((-127).toByte(), report[2])
        assertEquals(3.toByte(), report[3])
        assertEquals((-2).toByte(), report[4])
    }

    @Test
    fun `consumer report encodes 16-bit usage ID little endian`() {
        val playPause = HidReportDescriptor.buildConsumerReport(HidKeyCodes.CONSUMER_PLAY_PAUSE)
        assertArrayEquals(byteArrayOf(0xCD.toByte(), 0x00.toByte()), playPause)

        val volUp = HidReportDescriptor.buildConsumerReport(HidKeyCodes.CONSUMER_VOL_UP)
        assertArrayEquals(byteArrayOf(0xE9.toByte(), 0x00.toByte()), volUp)
    }

    @Test
    fun `charToHidKey maps lowercase, uppercase, symbols, and whitespace accurately`() {
        val lowerA = HidKeyCodes.charToHidKey('a')
        assertNotNull(lowerA)
        assertEquals(HidKeyCodes.MOD_NONE, lowerA!!.modifierMask)
        assertEquals(HidKeyCodes.KEY_A, lowerA.keyCode)

        val upperZ = HidKeyCodes.charToHidKey('Z')
        assertNotNull(upperZ)
        assertEquals(HidKeyCodes.MOD_LEFT_SHIFT, upperZ!!.modifierMask)
        assertEquals(HidKeyCodes.KEY_Z, upperZ.keyCode)

        val atSign = HidKeyCodes.charToHidKey('@')
        assertNotNull(atSign)
        assertEquals(HidKeyCodes.MOD_LEFT_SHIFT, atSign!!.modifierMask)
        assertEquals(HidKeyCodes.KEY_2, atSign.keyCode)

        val enter = HidKeyCodes.charToHidKey('\n')
        assertNotNull(enter)
        assertEquals(HidKeyCodes.KEY_ENTER, enter!!.keyCode)
    }

    @Test
    fun `touchpad sub-pixel accumulator preserves tiny finger movements`() {
        val math = TouchpadMath()
        // Tiny slow movements of 0.4f each at 1.0x speed
        var totalX = 0
        repeat(5) {
            val d = math.computePointerDelta(0.4f, 0f, pointerSpeed = 1.0f, sensitivity = 0.0f)
            totalX += d.dx
        }
        assertEquals(2, totalX)
    }

    @Test
    fun `natural scrolling toggle inverts scroll direction cleanly`() {
        val mathNatural = TouchpadMath()
        val sNatural = mathNatural.computeScrollDelta(0f, 60f, naturalScrolling = true, sensitivity = 1.0f)

        val mathClassic = TouchpadMath()
        val sClassic = mathClassic.computeScrollDelta(0f, 60f, naturalScrolling = false, sensitivity = 1.0f)

        assertTrue(sNatural.wheel != 0)
        assertEquals(-sNatural.wheel, sClassic.wheel)
    }

    @Test
    fun `all required windows shortcuts exist in catalog`() {
        val ids = WindowsShortcutsCatalog.builtInShortcuts.map { it.id }.toSet()
        val required = listOf(
            "win_d", "alt_tab", "win_e", "win_r", "ctrl_shift_esc",
            "win_l", "alt_f4", "ctrl_c", "ctrl_v", "ctrl_z", "ctrl_a"
        )
        required.forEach { assertTrue("Missing $it", it in ids) }
    }

    @Test
    fun `custom shortcut serialization round-trip preserves fields`() {
        val list = listOf(
            CustomShortcut("id1", "Snip", "Win + Shift + S", HidKeyCodes.MOD_LEFT_GUI or HidKeyCodes.MOD_LEFT_SHIFT, HidKeyCodes.KEY_S),
            CustomShortcut("id2", "Terminal", "Ctrl + Alt + T", HidKeyCodes.MOD_LEFT_CTRL or HidKeyCodes.MOD_LEFT_ALT, HidKeyCodes.KEY_T)
        )
        val encoded = PreferencesRepository.encodeCustomShortcuts(list)
        val decoded = PreferencesRepository.decodeCustomShortcuts(encoded)
        assertEquals(list, decoded)
    }

    @Test
    fun `wifi discovery strictly restricts probes to private RFC1918 LAN addresses`() {
        val lan1 = InetAddress.getByName("192.168.1.45") as Inet4Address
        val lan2 = InetAddress.getByName("10.0.0.12") as Inet4Address
        val lan3 = InetAddress.getByName("172.16.4.8") as Inet4Address
        val pub1 = InetAddress.getByName("8.8.8.8") as Inet4Address
        val pub2 = InetAddress.getByName("1.1.1.1") as Inet4Address

        assertTrue(WifiDiscoveryRepository.isPrivateLanIpv4(lan1))
        assertTrue(WifiDiscoveryRepository.isPrivateLanIpv4(lan2))
        assertTrue(WifiDiscoveryRepository.isPrivateLanIpv4(lan3))
        assertFalse(WifiDiscoveryRepository.isPrivateLanIpv4(pub1))
        assertFalse(WifiDiscoveryRepository.isPrivateLanIpv4(pub2))
    }

    @Test
    fun `auto-connect eligibility defaults to computers and honors user opt-in and opt-out`() {
        val pcAddr = "AA:BB:CC:DD:EE:01"
        val budsAddr = "AA:BB:CC:DD:EE:02"
        val tvAddr = "AA:BB:CC:DD:EE:03"

        // Computers are enabled by default
        assertTrue(
            BluetoothHidController.isDeviceAutoConnectEnabled(
                address = pcAddr,
                isLikelyComputer = true,
                lastConnectedAddress = null,
                autoConnectAddresses = emptySet(),
                disabledAutoConnectAddresses = emptySet()
            )
        )

        // Non-computer peripheral is disabled by default unless last connected or explicitly opted in
        assertFalse(
            BluetoothHidController.isDeviceAutoConnectEnabled(
                address = budsAddr,
                isLikelyComputer = false,
                lastConnectedAddress = null,
                autoConnectAddresses = emptySet(),
                disabledAutoConnectAddresses = emptySet()
            )
        )

        // Explicit opt-in enables non-computer devices
        assertTrue(
            BluetoothHidController.isDeviceAutoConnectEnabled(
                address = tvAddr,
                isLikelyComputer = false,
                lastConnectedAddress = null,
                autoConnectAddresses = setOf(tvAddr),
                disabledAutoConnectAddresses = emptySet()
            )
        )

        // Last connected device is eligible even if not flagged as computer
        assertTrue(
            BluetoothHidController.isDeviceAutoConnectEnabled(
                address = tvAddr,
                isLikelyComputer = false,
                lastConnectedAddress = tvAddr,
                autoConnectAddresses = emptySet(),
                disabledAutoConnectAddresses = emptySet()
            )
        )

        // Explicit opt-out overrides computer and last-connected defaults
        assertFalse(
            BluetoothHidController.isDeviceAutoConnectEnabled(
                address = pcAddr,
                isLikelyComputer = true,
                lastConnectedAddress = pcAddr,
                autoConnectAddresses = setOf(pcAddr),
                disabledAutoConnectAddresses = setOf(pcAddr)
            )
        )
    }

    @Test
    fun `auto-connect candidate ranking prioritizes ACL-connected devices and respects manual suppression`() {
        val lastPc = PairedPcDevice(
            name = "Work Desktop",
            address = "11:22:33:44:55:01",
            majorDeviceClass = 0x0100,
            isLikelyComputer = true,
            isConnected = false,
            lastRssiDbm = null,
            isBluetoothAclConnected = false,
            autoConnectEnabled = true
        )
        val aclActiveLaptop = PairedPcDevice(
            name = "HP ProBook 445R G6",
            address = "11:22:33:44:55:02",
            majorDeviceClass = 0x0100,
            isLikelyComputer = true,
            isConnected = false,
            lastRssiDbm = -52,
            isBluetoothAclConnected = true,
            autoConnectEnabled = true
        )
        val suppressedPc = PairedPcDevice(
            name = "Suppressed PC",
            address = "11:22:33:44:55:03",
            majorDeviceClass = 0x0100,
            isLikelyComputer = true,
            isConnected = false,
            lastRssiDbm = null,
            isBluetoothAclConnected = true,
            autoConnectEnabled = true
        )
        val optedOutPc = PairedPcDevice(
            name = "Opted Out PC",
            address = "11:22:33:44:55:04",
            majorDeviceClass = 0x0100,
            isLikelyComputer = true,
            isConnected = false,
            lastRssiDbm = null,
            isBluetoothAclConnected = true,
            autoConnectEnabled = false
        )

        val candidates = BluetoothHidController.selectAutoConnectCandidates(
            pairedDevices = listOf(lastPc, optedOutPc, suppressedPc, aclActiveLaptop),
            lastConnectedAddress = lastPc.address,
            suppressedAddresses = setOf(suppressedPc.address)
        )

        assertEquals(listOf(aclActiveLaptop.address, lastPc.address), candidates.map { it.address })
    }
}
