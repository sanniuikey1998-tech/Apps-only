package com.example.data

import com.example.domain.model.ScriptServerStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.ServerSocket

/**
 * Temporary 60-second local-LAN HTTP server that delivers the active PowerShell receiver script
 * so Windows users can start it with a single command: `irm http://<phone-ip>:<port> | iex`.
 * Strictly rejects non-RFC1918 clients and stops automatically when the countdown finishes.
 */
class PowerShellScriptHttpServer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _status = MutableStateFlow(ScriptServerStatus())
    val status: StateFlow<ScriptServerStatus> = _status.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null
    private var countdownJob: Job? = null

    fun startServing(
        scriptText: String,
        port: Int,
        displayIpHint: String?,
        durationSeconds: Int = 60
    ) {
        stop()

        val safePort = port.coerceIn(1024, 65535)
        val displayIp = displayIpHint?.takeIf { it.isNotBlank() } ?: "192.168.43.1"
        val command = formatOneLiner(displayIp, safePort)
        val bodyBytes = scriptText.toByteArray(Charsets.UTF_8)

        _status.value = ScriptServerStatus(
            isRunning = true,
            port = safePort,
            oneLinerCommand = command,
            remainingSeconds = durationSeconds,
            serveCount = 0,
            statusNote = "Serving script on private LAN for ${durationSeconds}s"
        )

        acceptJob = scope.launch {
            try {
                val socket = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(safePort))
                }
                serverSocket = socket

                while (isActive && !socket.isClosed) {
                    val client = socket.accept()
                    launch {
                        client.use { conn ->
                            conn.soTimeout = 2500
                            val remote = conn.inetAddress
                            if (remote !is Inet4Address || !WifiDiscoveryRepository.isPrivateLanIpv4(remote)) {
                                return@use
                            }
                            val reader = BufferedReader(InputStreamReader(conn.getInputStream(), Charsets.UTF_8))
                            val requestLine = runCatching { reader.readLine() }.getOrNull().orEmpty()
                            if (!requestLine.startsWith("GET ")) {
                                val forbidden = "HTTP/1.1 405 Method Not Allowed\r\nConnection: close\r\n\r\n"
                                conn.getOutputStream().write(forbidden.toByteArray(Charsets.US_ASCII))
                                return@use
                            }
                            val headers = buildString {
                                append("HTTP/1.1 200 OK\r\n")
                                append("Content-Type: text/plain; charset=utf-8\r\n")
                                append("Content-Length: ").append(bodyBytes.size).append("\r\n")
                                append("Cache-Control: no-store\r\n")
                                append("Connection: close\r\n\r\n")
                            }.toByteArray(Charsets.US_ASCII)

                            val out = conn.getOutputStream()
                            out.write(headers)
                            out.write(bodyBytes)
                            out.flush()

                            _status.update { current ->
                                current.copy(
                                    serveCount = current.serveCount + 1,
                                    statusNote = "Delivered script to ${remote.hostAddress}"
                                )
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                if (_status.value.isRunning) {
                    _status.update {
                        it.copy(
                            isRunning = false,
                            remainingSeconds = 0,
                            statusNote = "Unable to bind port $safePort"
                        )
                    }
                }
            }
        }

        countdownJob = scope.launch {
            var left = durationSeconds
            while (isActive && left > 0) {
                delay(1000L)
                left -= 1
                _status.update { it.copy(remainingSeconds = left) }
            }
            if (isActive) {
                stop("One-line setup window closed")
            }
        }
    }

    fun stop(reason: String = "") {
        countdownJob?.cancel()
        countdownJob = null
        acceptJob?.cancel()
        acceptJob = null
        runCatching { serverSocket?.close() }
        serverSocket = null
        _status.update {
            it.copy(
                isRunning = false,
                remainingSeconds = 0,
                statusNote = reason
            )
        }
    }

    companion object {
        fun formatOneLiner(hostIp: String, port: Int): String {
            return "irm http://$hostIp:$port | iex"
        }
    }
}
