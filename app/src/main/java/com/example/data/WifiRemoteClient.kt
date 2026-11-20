package com.example.data

import com.example.domain.hid.WindowsVirtualKeyMapper
import com.example.domain.model.FileTransferState
import com.example.domain.model.WifiConnectionDirection
import com.example.domain.model.WifiConnectionPhase
import com.example.domain.model.WifiRemoteStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

/**
 * Local Wi-Fi / Android Hotspot TCP remote client paired with the built-in Windows PowerShell receiver.
 * Strictly validates that peer addresses belong to private RFC1918 LAN/hotspot ranges only.
 */
class WifiRemoteClient {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _status = MutableStateFlow(WifiRemoteStatus())
    val status: StateFlow<WifiRemoteStatus> = _status.asStateFlow()

    private val _incomingClipboard = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val incomingClipboard: SharedFlow<String> = _incomingClipboard.asSharedFlow()

    private val _fileTransferState = MutableStateFlow(FileTransferState())
    val fileTransferState: StateFlow<FileTransferState> = _fileTransferState.asStateFlow()

    private var socket: Socket? = null
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null
    private var writer: BufferedWriter? = null
    private var outboundChannel: Channel<String>? = null
    private var writerJob: Job? = null
    private var heartbeatJob: Job? = null
    private var lastPongAwaitStartMs: Long = 0L

    fun startHotspotServer(port: Int, pin: String, hotspotIpHint: String?) {
        val safePort = port.coerceIn(1024, 65535)
        val cleanPin = pin.trim()

        disconnectInternal(keepMessage = false)

        _status.value = WifiRemoteStatus(
            phase = WifiConnectionPhase.LISTENING,
            direction = WifiConnectionDirection.PC_TO_PHONE_HOTSPOT,
            hostIp = hotspotIpHint,
            port = safePort,
            statusMessage = if (!hotspotIpHint.isNullOrBlank()) {
                "Hotspot listener ready at $hotspotIpHint:$safePort · Run PowerShell script on PC"
            } else {
                "Listening on port $safePort · Connect PC to Phone Hotspot & run PowerShell"
            }
        )

        acceptJob = scope.launch {
            try {
                val srv = ServerSocket()
                srv.reuseAddress = true
                srv.bind(InetSocketAddress(safePort))
                serverSocket = srv

                while (isActive && !srv.isClosed) {
                    val client = srv.accept()
                    val remoteAddr = client.inetAddress
                    if (remoteAddr !is Inet4Address || !WifiDiscoveryRepository.isPrivateLanIpv4(remoteAddr)) {
                        runCatching { client.close() }
                        continue
                    }

                    client.tcpNoDelay = true
                    client.soTimeout = 4500

                    val inp = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
                    val out = BufferedWriter(OutputStreamWriter(client.getOutputStream(), Charsets.UTF_8))

                    val hello = runCatching { inp.readLine() }.getOrNull()
                    val parsedPcName = parseHotspotHello(hello, cleanPin)
                    if (parsedPcName == null) {
                        runCatching {
                            out.write("ERR|BAD_PIN\n")
                            out.flush()
                            client.close()
                        }
                        continue
                    }

                    val pcName = parsedPcName.takeIf { it.isNotBlank() } ?: (remoteAddr.hostAddress ?: "Windows PC")
                    val pcIp = remoteAddr.hostAddress
                    client.soTimeout = 0
                    runCatching { srv.close() }
                    serverSocket = null

                    socket = client
                    writer = out
                    out.write("OK|DeckLink\n")
                    out.flush()

                    val channel = Channel<String>(capacity = 256)
                    outboundChannel = channel

                    _status.value = WifiRemoteStatus(
                        phase = WifiConnectionPhase.CONNECTED,
                        direction = WifiConnectionDirection.PC_TO_PHONE_HOTSPOT,
                        hostIp = pcIp,
                        port = safePort,
                        remoteHostname = pcName,
                        statusMessage = "Connected via Hotspot ($pcName)"
                    )

                    startWriterAndReader(
                        tcpSocket = client,
                        inp = inp,
                        out = out,
                        channel = channel,
                        hostIp = pcIp ?: "",
                        port = safePort,
                        direction = WifiConnectionDirection.PC_TO_PHONE_HOTSPOT
                    )
                    break
                }
            } catch (_: Exception) {
                if (_status.value.phase == WifiConnectionPhase.LISTENING) {
                    _status.value = WifiRemoteStatus(
                        phase = WifiConnectionPhase.ERROR,
                        direction = WifiConnectionDirection.PC_TO_PHONE_HOTSPOT,
                        hostIp = hotspotIpHint,
                        port = safePort,
                        statusMessage = "Could not open hotspot listener on port $safePort"
                    )
                }
            }
        }
    }

    fun connect(hostIp: String, port: Int, pin: String) {
        val cleanIp = hostIp.trim()
        val safePort = port.coerceIn(1024, 65535)
        val cleanPin = pin.trim()

        if (cleanIp.isBlank()) {
            _status.value = WifiRemoteStatus(
                phase = WifiConnectionPhase.ERROR,
                direction = WifiConnectionDirection.PHONE_TO_PC,
                hostIp = cleanIp,
                port = safePort,
                statusMessage = "Enter your Windows PC's local Wi-Fi IP address first"
            )
            return
        }

        disconnectInternal(keepMessage = false)

        _status.value = WifiRemoteStatus(
            phase = WifiConnectionPhase.CONNECTING,
            direction = WifiConnectionDirection.PHONE_TO_PC,
            hostIp = cleanIp,
            port = safePort,
            statusMessage = "Connecting to $cleanIp:$safePort..."
        )

        scope.launch {
            val resolvedInet = runCatching { InetAddress.getByName(cleanIp) }.getOrNull()
            if (resolvedInet !is Inet4Address || !WifiDiscoveryRepository.isPrivateLanIpv4(resolvedInet)) {
                _status.value = WifiRemoteStatus(
                    phase = WifiConnectionPhase.ERROR,
                    direction = WifiConnectionDirection.PHONE_TO_PC,
                    hostIp = cleanIp,
                    port = safePort,
                    statusMessage = "Blocked: Wi-Fi mode allows private local LAN IPv4 addresses only"
                )
                return@launch
            }

            try {
                val tcpSocket = Socket()
                tcpSocket.tcpNoDelay = true
                tcpSocket.soTimeout = 4500
                tcpSocket.connect(InetSocketAddress(resolvedInet, safePort), 4000)

                _status.update {
                    it.copy(
                        phase = WifiConnectionPhase.AUTHENTICATING,
                        statusMessage = "Verifying PIN with Windows PowerShell receiver..."
                    )
                }

                val out = BufferedWriter(OutputStreamWriter(tcpSocket.getOutputStream(), Charsets.UTF_8))
                val inp = BufferedReader(InputStreamReader(tcpSocket.getInputStream(), Charsets.UTF_8))

                out.write("AUTH|$cleanPin\n")
                out.flush()

                val reply = sanitizeProtocolLine(inp.readLine())
                if (reply == null || !reply.startsWith("OK|")) {
                    tcpSocket.close()
                    val reason = if (reply == "ERR|BAD_PIN") {
                        "Incorrect PIN rejected by Windows receiver"
                    } else {
                        "Receiver did not accept DeckLink handshake"
                    }
                    _status.value = WifiRemoteStatus(
                        phase = WifiConnectionPhase.ERROR,
                        direction = WifiConnectionDirection.PHONE_TO_PC,
                        hostIp = cleanIp,
                        port = safePort,
                        statusMessage = reason
                    )
                    return@launch
                }

                val pcName = reply.split("|").getOrNull(2)?.takeIf { it.isNotBlank() } ?: cleanIp
                tcpSocket.soTimeout = 0 // Long-lived connection

                socket = tcpSocket
                writer = out
                val channel = Channel<String>(capacity = 256)
                outboundChannel = channel

                _status.value = WifiRemoteStatus(
                    phase = WifiConnectionPhase.CONNECTED,
                    direction = WifiConnectionDirection.PHONE_TO_PC,
                    hostIp = cleanIp,
                    port = safePort,
                    remoteHostname = pcName,
                    statusMessage = "Connected via Local Wi-Fi ($pcName)"
                )

                startWriterAndReader(
                    tcpSocket = tcpSocket,
                    inp = inp,
                    out = out,
                    channel = channel,
                    hostIp = cleanIp,
                    port = safePort,
                    direction = WifiConnectionDirection.PHONE_TO_PC
                )
            } catch (e: Exception) {
                disconnectInternal(keepMessage = true)
                _status.value = WifiRemoteStatus(
                    phase = WifiConnectionPhase.ERROR,
                    direction = WifiConnectionDirection.PHONE_TO_PC,
                    hostIp = cleanIp,
                    port = safePort,
                    statusMessage = "Could not reach $cleanIp:$safePort — start the PowerShell receiver on your PC"
                )
            }
        }
    }

    private fun startWriterAndReader(
        tcpSocket: Socket,
        inp: BufferedReader,
        out: BufferedWriter,
        channel: Channel<String>,
        hostIp: String,
        port: Int,
        direction: WifiConnectionDirection
    ) {
        writerJob = scope.launch {
            try {
                for (frame in channel) {
                    out.write(frame)
                    out.write("\n")
                    // Drain any immediately queued frames before flushing for low latency
                    while (true) {
                        val next = channel.tryReceive().getOrNull() ?: break
                        out.write(next)
                        out.write("\n")
                    }
                    out.flush()
                }
            } catch (_: Exception) {
                onRemoteClosed(hostIp, port, direction, "Wi-Fi connection closed by PC")
            }
        }

        scope.launch {
            try {
                while (isActive && !tcpSocket.isClosed) {
                    val rawLine = inp.readLine() ?: break
                    val line = sanitizeProtocolLine(rawLine) ?: continue
                    when {
                        line == "PONG" && lastPongAwaitStartMs > 0L -> {
                            val rtt = (System.currentTimeMillis() - lastPongAwaitStartMs).coerceAtLeast(1L)
                            _status.update { it.copy(latencyMs = rtt) }
                        }
                        line.startsWith("CLIP|") -> {
                            val decoded = WindowsVirtualKeyMapper.decodeBase64ToUtf8(line.removePrefix("CLIP|"))
                            _incomingClipboard.tryEmit(decoded)
                        }
                        line.startsWith("FILE_OK|") -> {
                            val saved = line.removePrefix("FILE_OK|").ifBlank { "file" }
                            _fileTransferState.value = FileTransferState(
                                isTransferring = false,
                                fileName = saved,
                                progressFraction = 1f,
                                lastResultMessage = "Saved $saved to PC Downloads"
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // Socket closed
            } finally {
                if (_status.value.phase == WifiConnectionPhase.CONNECTED) {
                    onRemoteClosed(hostIp, port, direction, "Wi-Fi / Hotspot receiver disconnected")
                }
            }
        }

        heartbeatJob = scope.launch {
            while (isActive && _status.value.phase == WifiConnectionPhase.CONNECTED) {
                lastPongAwaitStartMs = System.currentTimeMillis()
                enqueueFrame("PING")
                delay(6000L)
            }
        }
    }

    private fun onRemoteClosed(
        hostIp: String,
        port: Int,
        direction: WifiConnectionDirection,
        reason: String
    ) {
        disconnectInternal(keepMessage = true)
        _status.value = WifiRemoteStatus(
            phase = WifiConnectionPhase.DISCONNECTED,
            direction = direction,
            hostIp = hostIp,
            port = port,
            statusMessage = reason
        )
    }

    fun disconnect() {
        val current = _status.value
        disconnectInternal(keepMessage = false)
        _status.value = WifiRemoteStatus(
            phase = WifiConnectionPhase.DISCONNECTED,
            direction = current.direction,
            hostIp = current.hostIp,
            port = current.port,
            statusMessage = "Disconnected from Local Wi-Fi receiver"
        )
    }

    private fun disconnectInternal(keepMessage: Boolean) {
        acceptJob?.cancel()
        acceptJob = null
        runCatching { serverSocket?.close() }
        serverSocket = null
        heartbeatJob?.cancel()
        heartbeatJob = null
        writerJob?.cancel()
        writerJob = null
        outboundChannel?.close()
        outboundChannel = null
        runCatching { writer?.close() }
        writer = null
        runCatching { socket?.close() }
        socket = null
        if (!keepMessage) {
            lastPongAwaitStartMs = 0L
        }
    }

    fun sendMouseReport(
        buttonsMask: Int,
        dx: Int,
        dy: Int,
        wheel: Int = 0,
        pan: Int = 0
    ): Boolean {
        val frame = WindowsVirtualKeyMapper.formatMouseCommand(buttonsMask, dx, dy, wheel, pan)
        return enqueueFrame(frame)
    }

    fun sendKeyPressAndRelease(modifierMask: Int, hidKeyCode: Int): Boolean {
        val frame = WindowsVirtualKeyMapper.formatKeyCommand(modifierMask, hidKeyCode) ?: return false
        return enqueueFrame(frame)
    }

    fun sendKeyState(modifierMask: Int, hidKeyCodes: List<Int>): Boolean {
        val frame = WindowsVirtualKeyMapper.formatKeyStateCommand(modifierMask, hidKeyCodes)
        return enqueueFrame(frame)
    }

    suspend fun typeText(text: String): Boolean = withContext(Dispatchers.IO) {
        if (text.isEmpty() || _status.value.phase != WifiConnectionPhase.CONNECTED) return@withContext false
        val frame = WindowsVirtualKeyMapper.formatUnicodeTextCommand(text)
        enqueueFrame(frame)
    }

    fun sendConsumerKey(usageCode: Int): Boolean {
        val frame = WindowsVirtualKeyMapper.formatConsumerCommand(usageCode) ?: return false
        return enqueueFrame(frame)
    }

    fun pushClipboardToPc(text: String): Boolean {
        if (_status.value.phase != WifiConnectionPhase.CONNECTED) return false
        return enqueueFrame(WindowsVirtualKeyMapper.formatClipboardSetCommand(text))
    }

    fun pullClipboardFromPc(): Boolean {
        if (_status.value.phase != WifiConnectionPhase.CONNECTED) return false
        return enqueueFrame(WindowsVirtualKeyMapper.formatClipboardGetCommand())
    }

    suspend fun sendFileToPc(fileName: String, bytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        if (_status.value.phase != WifiConnectionPhase.CONNECTED) {
            return@withContext Result.failure(IllegalStateException("Connect via Local Wi-Fi or Hotspot first"))
        }
        val ch = outboundChannel
            ?: return@withContext Result.failure(IllegalStateException("Wi-Fi channel unavailable"))
        if (bytes.size > MAX_FILE_BYTES) {
            return@withContext Result.failure(IllegalArgumentException("File exceeds 25 MB quick-drop limit"))
        }
        runCatching {
            _fileTransferState.value = FileTransferState(
                isTransferring = true,
                fileName = fileName,
                progressFraction = 0f,
                lastResultMessage = null
            )
            ch.send(WindowsVirtualKeyMapper.formatFileStartCommand(fileName, bytes.size))
            val chunkSize = 24 * 1024
            var offset = 0
            while (offset < bytes.size) {
                val len = minOf(chunkSize, bytes.size - offset)
                ch.send(WindowsVirtualKeyMapper.formatFileChunkCommand(bytes, offset, len))
                offset += len
                val frac = if (bytes.isEmpty()) 1f else (offset.toFloat() / bytes.size.toFloat()).coerceIn(0f, 0.98f)
                _fileTransferState.update { it.copy(progressFraction = frac) }
            }
            ch.send(WindowsVirtualKeyMapper.formatFileEndCommand())
            _fileTransferState.value = FileTransferState(
                isTransferring = false,
                fileName = fileName,
                progressFraction = 1f,
                lastResultMessage = "Sent $fileName to PC Downloads"
            )
            "Sent $fileName (${bytes.size / 1024} KB) to PC Downloads"
        }.onFailure { err ->
            _fileTransferState.value = FileTransferState(
                isTransferring = false,
                fileName = fileName,
                progressFraction = 0f,
                lastResultMessage = err.message ?: "Transfer failed"
            )
        }
    }

    private fun enqueueFrame(frame: String): Boolean {
        if (_status.value.phase != WifiConnectionPhase.CONNECTED) return false
        return outboundChannel?.trySend(frame)?.isSuccess == true
    }

    companion object {
        const val MAX_FILE_BYTES = 25 * 1024 * 1024

        fun sanitizeProtocolLine(raw: String?): String? {
            return raw?.trimStart('\uFEFF')?.trim()
        }

        /**
         * Validates a Windows hotspot HELLO handshake frame (`HELLO|<PIN>|<HOSTNAME>`),
         * stripping any leading UTF-8 BOM emitted by .NET StreamWriter.
         * Returns the remote hostname (empty string if omitted) when valid, or null if rejected.
         */
        fun parseHotspotHello(rawLine: String?, expectedPin: String): String? {
            val cleanLine = sanitizeProtocolLine(rawLine) ?: return null
            val parts = cleanLine.split("|")
            if (parts.size < 2) return null
            if (parts[0].trim() != "HELLO") return null
            if (parts[1].trim() != expectedPin.trim()) return null
            return parts.getOrNull(2)?.trim().orEmpty()
        }
    }
}
