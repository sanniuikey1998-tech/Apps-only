package com.example.domain.hid

/**
 * Maps DeckLink's USB HID Usage IDs (HidKeyCodes) and Consumer usages to standard
 * Windows Win32 Virtual-Key (VK_*) codes, formats local Wi-Fi wire commands, and
 * generates the self-contained Windows PowerShell receiver script (uses built-in
 * .NET + user32.dll SendInput — zero third-party installs).
 */
object WindowsVirtualKeyMapper {

    const val DEFAULT_WIFI_PORT = 58732
    const val DEFAULT_WIFI_PIN = "482910"

    // Extended keys that require KEYEVENTF_EXTENDEDKEY (0x0001) on Windows SendInput
    private val extendedVkCodes = setOf(
        0x21, // VK_PRIOR (Page Up)
        0x22, // VK_NEXT (Page Down)
        0x23, // VK_END
        0x24, // VK_HOME
        0x25, // VK_LEFT
        0x26, // VK_UP
        0x27, // VK_RIGHT
        0x28, // VK_DOWN
        0x2C, // VK_SNAPSHOT (PrintScreen)
        0x2D, // VK_INSERT
        0x2E, // VK_DELETE
        0x5B, // VK_LWIN
        0x5C, // VK_RWIN
        0x5D, // VK_APPS
        0xA3, // VK_RCONTROL
        0xA5  // VK_RMENU (Right Alt)
    )

    fun isExtendedVk(vk: Int): Boolean = vk in extendedVkCodes

    fun modifierMaskToVks(modifierMask: Int): List<Int> {
        val list = mutableListOf<Int>()
        if ((modifierMask and HidKeyCodes.MOD_LEFT_CTRL) != 0) list.add(0xA2)   // VK_LCONTROL
        if ((modifierMask and HidKeyCodes.MOD_LEFT_SHIFT) != 0) list.add(0xA0)  // VK_LSHIFT
        if ((modifierMask and HidKeyCodes.MOD_LEFT_ALT) != 0) list.add(0xA4)    // VK_LMENU
        if ((modifierMask and HidKeyCodes.MOD_LEFT_GUI) != 0) list.add(0x5B)    // VK_LWIN
        if ((modifierMask and HidKeyCodes.MOD_RIGHT_CTRL) != 0) list.add(0xA3)  // VK_RCONTROL
        if ((modifierMask and HidKeyCodes.MOD_RIGHT_SHIFT) != 0) list.add(0xA1) // VK_RSHIFT
        if ((modifierMask and HidKeyCodes.MOD_RIGHT_ALT) != 0) list.add(0xA5)   // VK_RMENU
        if ((modifierMask and HidKeyCodes.MOD_RIGHT_GUI) != 0) list.add(0x5C)   // VK_RWIN
        return list
    }

    fun hidUsageToVk(hidCode: Int): Int? {
        if (hidCode in HidKeyCodes.KEY_A..HidKeyCodes.KEY_Z) {
            return 0x41 + (hidCode - HidKeyCodes.KEY_A) // 'A'..'Z'
        }
        if (hidCode in HidKeyCodes.KEY_1..HidKeyCodes.KEY_9) {
            return 0x31 + (hidCode - HidKeyCodes.KEY_1) // '1'..'9'
        }
        if (hidCode in HidKeyCodes.KEY_F1..HidKeyCodes.KEY_F12) {
            return 0x70 + (hidCode - HidKeyCodes.KEY_F1) // VK_F1..VK_F12
        }
        return when (hidCode) {
            HidKeyCodes.KEY_0 -> 0x30
            HidKeyCodes.KEY_ENTER -> 0x0D
            HidKeyCodes.KEY_ESC -> 0x1B
            HidKeyCodes.KEY_BACKSPACE -> 0x08
            HidKeyCodes.KEY_TAB -> 0x09
            HidKeyCodes.KEY_SPACE -> 0x20
            HidKeyCodes.KEY_MINUS -> 0xBD         // VK_OEM_MINUS
            HidKeyCodes.KEY_EQUAL -> 0xBB         // VK_OEM_PLUS
            HidKeyCodes.KEY_LEFT_BRACKET -> 0xDB  // VK_OEM_4
            HidKeyCodes.KEY_RIGHT_BRACKET -> 0xDD // VK_OEM_6
            HidKeyCodes.KEY_BACKSLASH -> 0xDC     // VK_OEM_5
            HidKeyCodes.KEY_SEMICOLON -> 0xBA     // VK_OEM_1
            HidKeyCodes.KEY_APOSTROPHE -> 0xDE    // VK_OEM_7
            HidKeyCodes.KEY_GRAVE -> 0xC0         // VK_OEM_3
            HidKeyCodes.KEY_COMMA -> 0xBC         // VK_OEM_COMMA
            HidKeyCodes.KEY_PERIOD -> 0xBE        // VK_OEM_PERIOD
            HidKeyCodes.KEY_SLASH -> 0xBF         // VK_OEM_2
            HidKeyCodes.KEY_CAPS_LOCK -> 0x14     // VK_CAPITAL
            HidKeyCodes.KEY_PRINT_SCREEN -> 0x2C  // VK_SNAPSHOT
            HidKeyCodes.KEY_SCROLL_LOCK -> 0x91   // VK_SCROLL
            HidKeyCodes.KEY_PAUSE -> 0x13         // VK_PAUSE
            HidKeyCodes.KEY_INSERT -> 0x2D        // VK_INSERT
            HidKeyCodes.KEY_HOME -> 0x24          // VK_HOME
            HidKeyCodes.KEY_PAGE_UP -> 0x21       // VK_PRIOR
            HidKeyCodes.KEY_DELETE -> 0x2E        // VK_DELETE
            HidKeyCodes.KEY_END -> 0x23           // VK_END
            HidKeyCodes.KEY_PAGE_DOWN -> 0x22     // VK_NEXT
            HidKeyCodes.KEY_RIGHT -> 0x27         // VK_RIGHT
            HidKeyCodes.KEY_LEFT -> 0x25          // VK_LEFT
            HidKeyCodes.KEY_DOWN -> 0x28          // VK_DOWN
            HidKeyCodes.KEY_UP -> 0x26            // VK_UP
            HidKeyCodes.KEY_APP_MENU -> 0x5D      // VK_APPS
            else -> null
        }
    }

    fun consumerUsageToVk(usageCode: Int): Int? = when (usageCode) {
        HidKeyCodes.CONSUMER_NEXT_TRACK -> 0xB0 // VK_MEDIA_NEXT_TRACK
        HidKeyCodes.CONSUMER_PREV_TRACK -> 0xB1 // VK_MEDIA_PREV_TRACK
        HidKeyCodes.CONSUMER_STOP -> 0xB2       // VK_MEDIA_STOP
        HidKeyCodes.CONSUMER_PLAY_PAUSE -> 0xB3 // VK_MEDIA_PLAY_PAUSE
        HidKeyCodes.CONSUMER_MUTE -> 0xAD       // VK_VOLUME_MUTE
        HidKeyCodes.CONSUMER_VOL_UP -> 0xAF     // VK_VOLUME_UP
        HidKeyCodes.CONSUMER_VOL_DOWN -> 0xAE   // VK_VOLUME_DOWN
        else -> null
    }

    fun formatMouseCommand(
        buttonsMask: Int,
        dx: Int,
        dy: Int,
        wheel: Int = 0,
        pan: Int = 0
    ): String {
        val clampedDx = dx.coerceIn(-4096, 4096)
        val clampedDy = dy.coerceIn(-4096, 4096)
        val clampedWheel = wheel.coerceIn(-30, 30)
        val clampedPan = pan.coerceIn(-30, 30)
        return "M|$buttonsMask|$clampedDx|$clampedDy|$clampedWheel|$clampedPan"
    }

    fun formatKeyCommand(modifierMask: Int, hidKeyCode: Int): String? {
        val vk = hidUsageToVk(hidKeyCode) ?: return null
        val modVks = modifierMaskToVks(modifierMask).joinToString(",")
        return "K|$modVks|$vk"
    }

    fun formatKeyStateCommand(modifierMask: Int, hidKeyCodes: List<Int>): String {
        val vks = (modifierMaskToVks(modifierMask) + hidKeyCodes.mapNotNull(::hidUsageToVk))
            .distinct()
            .joinToString(",")
        return "KS|$vks"
    }

    fun formatConsumerCommand(usageCode: Int): String? {
        val vk = consumerUsageToVk(usageCode) ?: return null
        return "C|$vk"
    }

    fun formatUnicodeTextCommand(text: String): String {
        return "U|${encodeBase64(text.toByteArray(Charsets.UTF_8))}"
    }

    fun formatClipboardSetCommand(text: String): String {
        return "CLIP_SET|${encodeBase64(text.toByteArray(Charsets.UTF_8))}"
    }

    fun formatClipboardGetCommand(): String = "CLIP_GET"

    fun formatFileStartCommand(fileName: String, totalBytes: Int): String {
        val cleanName = fileName.replace("/", "_").replace("\\", "_").ifBlank { "decklink_file.bin" }
        return "FILE_START|${encodeBase64(cleanName.toByteArray(Charsets.UTF_8))}|$totalBytes"
    }

    fun formatFileChunkCommand(bytes: ByteArray, offset: Int, length: Int): String {
        val slice = bytes.copyOfRange(offset, offset + length)
        return "FILE_CHUNK|${encodeBase64(slice)}"
    }

    fun formatFileEndCommand(): String = "FILE_END"

    fun decodeBase64ToUtf8(encoded: String): String {
        val decoded = decodeBase64Bytes(encoded)
        return String(decoded, Charsets.UTF_8)
    }

    internal fun decodeBase64Bytes(encoded: String): ByteArray {
        val clean = encoded.trim()
        if (clean.isEmpty()) return ByteArray(0)
        val table = IntArray(128) { -1 }
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        chars.forEachIndexed { idx, c -> table[c.code] = idx }
        val out = java.io.ByteArrayOutputStream((clean.length * 3) / 4)
        var acc = 0
        var bits = 0
        for (ch in clean) {
            if (ch == '=') break
            val code = ch.code
            if (code >= 128) continue
            val v = table[code]
            if (v < 0) continue
            acc = (acc shl 6) or v
            bits += 6
            if (bits >= 8) {
                bits -= 8
                out.write((acc shr bits) and 0xFF)
            }
        }
        return out.toByteArray()
    }

    private fun encodeBase64(bytes: ByteArray): String {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val out = StringBuilder(((bytes.size + 2) / 3) * 4)
        var i = 0
        while (i < bytes.size) {
            val b0 = bytes[i].toInt() and 0xFF
            val b1 = if (i + 1 < bytes.size) bytes[i + 1].toInt() and 0xFF else 0
            val b2 = if (i + 2 < bytes.size) bytes[i + 2].toInt() and 0xFF else 0
            val triplet = (b0 shl 16) or (b1 shl 8) or b2
            out.append(alphabet[(triplet shr 18) and 0x3F])
            out.append(alphabet[(triplet shr 12) and 0x3F])
            out.append(if (i + 1 < bytes.size) alphabet[(triplet shr 6) and 0x3F] else '=')
            out.append(if (i + 2 < bytes.size) alphabet[triplet and 0x3F] else '=')
            i += 3
        }
        return out.toString()
    }

    /**
     * Generates a readable, zero-dependency Windows PowerShell script that starts a
     * local-LAN-only, PIN-authenticated TCP input receiver using built-in .NET & Win32 SendInput.
     */
    fun buildPowerShellReceiverScript(port: Int, pin: String): String {
        val safePort = port.coerceIn(1024, 65535)
        val safePin = pin.filter { it.isLetterOrDigit() }.ifBlank { DEFAULT_WIFI_PIN }
        return POWER_SHELL_TEMPLATE
            .replace("__PORT__", safePort.toString())
            .replace("__PIN__", safePin)
    }

    /**
     * Generates a zero-dependency Windows PowerShell script for Android Hotspot outdoor mode:
     * The PC connects OUTBOUND to the Android phone's Hotspot IP (auto-detecting the hotspot
     * gateway if needed) — avoiding Windows Public-Hotspot Firewall inbound blocks.
     */
    fun buildPowerShellHotspotClientScript(
        port: Int,
        pin: String,
        phoneIpHint: String? = null
    ): String {
        val safePort = port.coerceIn(1024, 65535)
        val safePin = pin.filter { it.isLetterOrDigit() }.ifBlank { DEFAULT_WIFI_PIN }
        val safeIpHint = phoneIpHint?.trim().orEmpty()
        return POWER_SHELL_HOTSPOT_TEMPLATE
            .replace("__PORT__", safePort.toString())
            .replace("__PIN__", safePin)
            .replace("__PHONE_IP__", safeIpHint)
    }

    private const val NATIVE_SENDINPUT_CS = $$"""
using System;
using System.Runtime.InteropServices;

public static class DeckLinkNative {
    [StructLayout(LayoutKind.Sequential)]
    public struct INPUT {
        public uint type;
        public InputUnion U;
    }

    [StructLayout(LayoutKind.Explicit)]
    public struct InputUnion {
        [FieldOffset(0)] public MOUSEINPUT mi;
        [FieldOffset(0)] public KEYBDINPUT ki;
        [FieldOffset(0)] public HARDWAREINPUT hi;
    }

    [StructLayout(LayoutKind.Sequential)]
    public struct MOUSEINPUT {
        public int dx;
        public int dy;
        public int mouseData;
        public uint dwFlags;
        public uint time;
        public IntPtr dwExtraInfo;
    }

    [StructLayout(LayoutKind.Sequential)]
    public struct KEYBDINPUT {
        public ushort wVk;
        public ushort wScan;
        public uint dwFlags;
        public uint time;
        public IntPtr dwExtraInfo;
    }

    [StructLayout(LayoutKind.Sequential)]
    public struct HARDWAREINPUT {
        public uint uMsg;
        public ushort wParamL;
        public ushort wParamH;
    }

    [DllImport("user32.dll", SetLastError = true)]
    public static extern uint SendInput(uint nInputs, INPUT[] pInputs, int cbSize);

    private const uint INPUT_MOUSE = 0;
    private const uint INPUT_KEYBOARD = 1;
    private const uint KEYEVENTF_EXTENDEDKEY = 0x0001;
    private const uint KEYEVENTF_KEYUP = 0x0002;
    private const uint KEYEVENTF_UNICODE = 0x0004;

    private static bool IsExtended(ushort vk) {
        return vk == 0x21 || vk == 0x22 || vk == 0x23 || vk == 0x24 ||
               vk == 0x25 || vk == 0x26 || vk == 0x27 || vk == 0x28 ||
               vk == 0x2C || vk == 0x2D || vk == 0x2E || vk == 0x5B ||
               vk == 0x5C || vk == 0x5D || vk == 0xA3 || vk == 0xA5;
    }

    public static void SendMouse(int dx, int dy, uint flags, int data) {
        INPUT inp = new INPUT();
        inp.type = INPUT_MOUSE;
        inp.U.mi.dx = dx;
        inp.U.mi.dy = dy;
        inp.U.mi.mouseData = data;
        inp.U.mi.dwFlags = flags;
        SendInput(1, new INPUT[] { inp }, Marshal.SizeOf(typeof(INPUT)));
    }

    public static void SendVk(ushort vk, bool up) {
        INPUT inp = new INPUT();
        inp.type = INPUT_KEYBOARD;
        inp.U.ki.wVk = vk;
        uint flags = up ? KEYEVENTF_KEYUP : 0;
        if (IsExtended(vk)) flags |= KEYEVENTF_EXTENDEDKEY;
        inp.U.ki.dwFlags = flags;
        SendInput(1, new INPUT[] { inp }, Marshal.SizeOf(typeof(INPUT)));
    }

    public static void SendText(string text) {
        if (string.IsNullOrEmpty(text)) return;
        foreach (char c in text) {
            if (c == '\r') continue;
            if (c == '\n') {
                SendVk(0x0D, false);
                SendVk(0x0D, true);
                continue;
            }
            if (c == '\t') {
                SendVk(0x09, false);
                SendVk(0x09, true);
                continue;
            }
            INPUT down = new INPUT();
            down.type = INPUT_KEYBOARD;
            down.U.ki.wScan = (ushort)c;
            down.U.ki.dwFlags = KEYEVENTF_UNICODE;
            INPUT up = new INPUT();
            up.type = INPUT_KEYBOARD;
            up.U.ki.wScan = (ushort)c;
            up.U.ki.dwFlags = KEYEVENTF_UNICODE | KEYEVENTF_KEYUP;
            SendInput(2, new INPUT[] { down, up }, Marshal.SizeOf(typeof(INPUT)));
        }
    }
}
"""

    private val POWER_SHELL_DISPATCH_BODY = $$"""
            while ($null -ne ($line = $reader.ReadLine())) {
                if ($line -eq "PING") {
                    $writer.WriteLine("PONG")
                    continue
                }
                $parts = $line.Split("|")
                switch ($parts[0]) {
                    "M" {
                        if ($parts.Length -lt 6) { break }
                        $btn = [int]$parts[1]
                        $dx = [int]$parts[2]
                        $dy = [int]$parts[3]
                        $wh = [int]$parts[4]
                        $pan = [int]$parts[5]
                        if ($dx -ne 0 -or $dy -ne 0) {
                            [DeckLinkNative]::SendMouse($dx, $dy, 0x0001, 0)
                        }
                        if ($wh -ne 0) {
                            [DeckLinkNative]::SendMouse(0, 0, 0x0800, $wh * 120)
                        }
                        if ($pan -ne 0) {
                            [DeckLinkNative]::SendMouse(0, 0, 0x1000, $pan * 120)
                        }
                        $changes = $btn -bxor $lastButtons
                        if ($changes -ne 0) {
                            if (($changes -band 1) -ne 0) { [DeckLinkNative]::SendMouse(0, 0, $(if (($btn -band 1) -ne 0) { 0x0002 } else { 0x0004 }), 0) }
                            if (($changes -band 2) -ne 0) { [DeckLinkNative]::SendMouse(0, 0, $(if (($btn -band 2) -ne 0) { 0x0008 } else { 0x0010 }), 0) }
                            if (($changes -band 4) -ne 0) { [DeckLinkNative]::SendMouse(0, 0, $(if (($btn -band 4) -ne 0) { 0x0020 } else { 0x0040 }), 0) }
                            if (($changes -band 8) -ne 0) { [DeckLinkNative]::SendMouse(0, 0, $(if (($btn -band 8) -ne 0) { 0x0080 } else { 0x0100 }), 1) }
                            if (($changes -band 16) -ne 0) { [DeckLinkNative]::SendMouse(0, 0, $(if (($btn -band 16) -ne 0) { 0x0080 } else { 0x0100 }), 2) }
                            $lastButtons = $btn
                        }
                    }
                    "K" {
                        if ($parts.Length -lt 3) { break }
                        $mods = @()
                        if (-not [string]::IsNullOrWhiteSpace($parts[1])) {
                            $mods = $parts[1].Split(",") | ForEach-Object { [int]$_ }
                        }
                        $vk = [int]$parts[2]
                        foreach ($m in $mods) { [DeckLinkNative]::SendVk([uint16]$m, $false) }
                        if ($vk -gt 0) {
                            [DeckLinkNative]::SendVk([uint16]$vk, $false)
                            [DeckLinkNative]::SendVk([uint16]$vk, $true)
                        }
                        for ($i = $mods.Count - 1; $i -ge 0; $i--) {
                            [DeckLinkNative]::SendVk([uint16]$mods[$i], $true)
                        }
                    }
                    "KS" {
                        $nextSet = [System.Collections.Generic.HashSet[int]]::new()
                        if ($parts.Length -ge 2 -and -not [string]::IsNullOrWhiteSpace($parts[1])) {
                            foreach ($item in $parts[1].Split(",")) {
                                $nextSet.Add([int]$item) | Out-Null
                            }
                        }
                        foreach ($old in @($heldVks)) {
                            if (-not $nextSet.Contains($old)) {
                                [DeckLinkNative]::SendVk([uint16]$old, $true)
                                $heldVks.Remove($old) | Out-Null
                            }
                        }
                        foreach ($n in $nextSet) {
                            if (-not $heldVks.Contains($n)) {
                                [DeckLinkNative]::SendVk([uint16]$n, $false)
                                $heldVks.Add($n) | Out-Null
                            }
                        }
                    }
                    "C" {
                        if ($parts.Length -ge 2) {
                            $vk = [int]$parts[1]
                            if ($vk -gt 0) {
                                [DeckLinkNative]::SendVk([uint16]$vk, $false)
                                [DeckLinkNative]::SendVk([uint16]$vk, $true)
                            }
                        }
                    }
                    "U" {
                        if ($parts.Length -ge 2 -and -not [string]::IsNullOrEmpty($parts[1])) {
                            $txt = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($parts[1]))
                            [DeckLinkNative]::SendText($txt)
                        }
                    }
                    "CLIP_SET" {
                        if ($parts.Length -ge 2) {
                            $clipTxt = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($parts[1]))
                            Set-Clipboard -Value $clipTxt
                            $writer.WriteLine("CLIP_OK")
                        }
                    }
                    "CLIP_GET" {
                        $curClip = Get-Clipboard -Raw -ErrorAction SilentlyContinue
                        if ($null -eq $curClip) { $curClip = "" }
                        $encClip = [System.Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($curClip))
                        $writer.WriteLine("CLIP|$encClip")
                    }
                    "FILE_START" {
                        if ($parts.Length -ge 2) {
                            if ($null -ne $rxFileStream) { $rxFileStream.Dispose(); $rxFileStream = $null }
                            $rawName = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($parts[1]))
                            $safeName = [System.IO.Path]::GetFileName($rawName)
                            if ([string]::IsNullOrWhiteSpace($safeName)) { $safeName = "decklink_drop.bin" }
                            $dlDir = Join-Path $env:USERPROFILE "Downloads"
                            if (-not (Test-Path $dlDir)) { New-Item -ItemType Directory -Path $dlDir -Force | Out-Null }
                            $destPath = Join-Path $dlDir $safeName
                            if (Test-Path $destPath) {
                                $stem = [System.IO.Path]::GetFileNameWithoutExtension($safeName)
                                $ext = [System.IO.Path]::GetExtension($safeName)
                                $destPath = Join-Path $dlDir ("${stem}_" + [DateTimeOffset]::UtcNow.ToUnixTimeSeconds() + $ext)
                            }
                            $rxFileName = [System.IO.Path]::GetFileName($destPath)
                            $rxFileStream = [System.IO.File]::Open($destPath, [System.IO.FileMode]::Create, [System.IO.FileAccess]::Write)
                        }
                    }
                    "FILE_CHUNK" {
                        if ($parts.Length -ge 2 -and $null -ne $rxFileStream) {
                            $chunkBytes = [System.Convert]::FromBase64String($parts[1])
                            $rxFileStream.Write($chunkBytes, 0, $chunkBytes.Length)
                        }
                    }
                    "FILE_END" {
                        if ($null -ne $rxFileStream) {
                            $rxFileStream.Flush()
                            $rxFileStream.Dispose()
                            $rxFileStream = $null
                            $writer.WriteLine("FILE_OK|$rxFileName")
                        }
                    }
                }
            }
"""

    private val POWER_SHELL_TEMPLATE = $$"""
# DeckLink Built-in Windows PowerShell Receiver (Zero Third-Party Installs)
# Accepts PIN-authenticated private Wi-Fi LAN connections only and injects input via Win32 SendInput.
$Port = __PORT__
$Pin = "__PIN__"

Add-Type -TypeDefinition @"
$$NATIVE_SENDINPUT_CS
"@

function Test-PrivateIp([System.Net.IPAddress]$ip) {
    $b = $ip.GetAddressBytes()
    if ($b.Length -ne 4) { return $false }
    return ($b[0] -eq 10) -or
           ($b[0] -eq 172 -and $b[1] -ge 16 -and $b[1] -le 31) -or
           ($b[0] -eq 192 -and $b[1] -eq 168) -or
           ($b[0] -eq 127)
}

$listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Any, $Port)
$listener.Start()
$ips = [System.Net.Dns]::GetHostAddresses([System.Net.Dns]::GetHostName()) |
    Where-Object { $_.AddressFamily -eq 'InterNetwork' -and (Test-PrivateIp $_) } |
    ForEach-Object { $_.IPAddressToString }

Write-Host "DeckLink Wi-Fi Receiver listening on port $Port | PIN: $Pin" -ForegroundColor Cyan
Write-Host "PC LAN IP(s): $($ips -join ', ')" -ForegroundColor Green
Write-Host "Press Ctrl+C to stop." -ForegroundColor DarkGray

try {
    while ($true) {
        $client = $listener.AcceptTcpClient()
        $client.NoDelay = $true
        $remoteIp = ([System.Net.IPEndPoint]$client.Client.RemoteEndPoint).Address
        if (-not (Test-PrivateIp $remoteIp)) {
            $client.Close()
            continue
        }

        $stream = $client.GetStream()
        $utf8NoBom = [System.Text.UTF8Encoding]::new($false)
        $reader = [System.IO.StreamReader]::new($stream, $utf8NoBom)
        $writer = [System.IO.StreamWriter]::new($stream, $utf8NoBom)
        $writer.AutoFlush = $true

        $lastButtons = 0
        $heldVks = [System.Collections.Generic.HashSet[int]]::new()
        $rxFileStream = $null
        $rxFileName = ""

        try {
            $authLine = $reader.ReadLine()
            if ($null -ne $authLine) { $authLine = $authLine.Trim().TrimStart([char]0xFEFF) }
            if ($authLine -ne "AUTH|$Pin") {
                $writer.WriteLine("ERR|BAD_PIN")
                continue
            }
            $writer.WriteLine("OK|DeckLink|$env:COMPUTERNAME")
            Write-Host "Connected from $($remoteIp.IPAddressToString)" -ForegroundColor Green

$$POWER_SHELL_DISPATCH_BODY
        } catch {
            Write-Host "Session ended: $($_.Exception.Message)" -ForegroundColor Yellow
        } finally {
            if ($null -ne $rxFileStream) { $rxFileStream.Dispose(); $rxFileStream = $null }
            if (($lastButtons -band 1) -ne 0) { [DeckLinkNative]::SendMouse(0, 0, 0x0004, 0) }
            if (($lastButtons -band 2) -ne 0) { [DeckLinkNative]::SendMouse(0, 0, 0x0010, 0) }
            if (($lastButtons -band 4) -ne 0) { [DeckLinkNative]::SendMouse(0, 0, 0x0040, 0) }
            foreach ($vk in $heldVks) { [DeckLinkNative]::SendVk([uint16]$vk, $true) }
            $client.Close()
        }
    }
} finally {
    $listener.Stop()
}
    """.trimIndent()

    private val POWER_SHELL_HOTSPOT_TEMPLATE = $$"""
# DeckLink Android Hotspot Outbound Receiver (No Inbound Firewall Rules Needed)
# Connects outbound from this Windows PC to your Android phone's Hotspot Gateway.
$Port = __PORT__
$Pin = "__PIN__"
$PhoneIp = "__PHONE_IP__"

if ([string]::IsNullOrWhiteSpace($PhoneIp)) {
    $PhoneIp = (Get-NetIPConfiguration | Where-Object { $_.IPv4DefaultGateway -ne $null } | Select-Object -First 1).IPv4DefaultGateway.NextHop
}
if ([string]::IsNullOrWhiteSpace($PhoneIp)) {
    $PhoneIp = Read-Host "Enter Phone Hotspot IP (e.g. 192.168.43.1)"
}

Add-Type -TypeDefinition @"
$$NATIVE_SENDINPUT_CS
"@

Write-Host "DeckLink Hotspot Receiver -> Phone ${PhoneIp}:$Port | PIN: $Pin" -ForegroundColor Cyan
Write-Host "Press Ctrl+C to stop." -ForegroundColor DarkGray

while ($true) {
    $client = $null
    $lastButtons = 0
    $heldVks = [System.Collections.Generic.HashSet[int]]::new()
    $rxFileStream = $null
    $rxFileName = ""
    try {
        $client = [System.Net.Sockets.TcpClient]::new()
        $client.NoDelay = $true
        $client.Connect($PhoneIp, $Port)
        $stream = $client.GetStream()
        $utf8NoBom = [System.Text.UTF8Encoding]::new($false)
        $reader = [System.IO.StreamReader]::new($stream, $utf8NoBom)
        $writer = [System.IO.StreamWriter]::new($stream, $utf8NoBom)
        $writer.AutoFlush = $true

        $writer.WriteLine("HELLO|$Pin|$env:COMPUTERNAME")
        $reply = $reader.ReadLine()
        if ($null -ne $reply) { $reply = $reply.Trim().TrimStart([char]0xFEFF) }
        if ($reply -ne "OK|DeckLink") {
            Write-Host "Rejected by phone: $reply" -ForegroundColor Red
            Start-Sleep -Seconds 3
            continue
        }
        Write-Host "Connected to DeckLink on $PhoneIp!" -ForegroundColor Green

$$POWER_SHELL_DISPATCH_BODY
    } catch {
        Write-Host "Waiting for DeckLink Hotspot listener on ${PhoneIp}:$Port..." -ForegroundColor Yellow
        Start-Sleep -Seconds 2
    } finally {
        if ($null -ne $rxFileStream) { $rxFileStream.Dispose(); $rxFileStream = $null }
        if (($lastButtons -band 1) -ne 0) { [DeckLinkNative]::SendMouse(0, 0, 0x0004, 0) }
        if (($lastButtons -band 2) -ne 0) { [DeckLinkNative]::SendMouse(0, 0, 0x0010, 0) }
        if (($lastButtons -band 4) -ne 0) { [DeckLinkNative]::SendMouse(0, 0, 0x0040, 0) }
        foreach ($vk in $heldVks) { [DeckLinkNative]::SendVk([uint16]$vk, $true) }
        if ($client -ne $null) { $client.Close() }
    }
}
    """.trimIndent()
}
