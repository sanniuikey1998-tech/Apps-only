package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import com.example.domain.model.DiscoveredWifiPc
import com.example.domain.model.WifiStatusInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles real local Wi-Fi link inspection, zero-install Windows PC discovery
 * (mDNS/NsdManager, WS-Discovery UDP 3702, and built-in SMB/RDP/WinRM LAN port checks),
 * and hardware Wake-on-LAN magic packets.
 *
 * Security guarantee: strictly rejects any non-RFC1918 / external IP address.
 * Never claims arbitrary mouse/keyboard HID input over Wi-Fi without a Windows-side receiver.
 */
class WifiDiscoveryRepository(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val nsdManager =
        context.getSystemService(Context.NSD_SERVICE) as? NsdManager

    private val _discoveredPcs = MutableStateFlow<List<DiscoveredWifiPc>>(emptyList())
    val discoveredPcs: StateFlow<List<DiscoveredWifiPc>> = _discoveredPcs.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    fun observeWifiStatus(): Flow<WifiStatusInfo> = callbackFlow {
        fun readCurrentWifi(): WifiStatusInfo {
            val activeNetwork = connectivityManager.activeNetwork
            val caps = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
            val hasWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            if (!hasWifi) {
                return WifiStatusInfo(isWifiConnected = false)
            }
            val linkProps: LinkProperties? = connectivityManager.getLinkProperties(activeNetwork)
            val ipv4 = linkProps?.linkAddresses
                ?.mapNotNull { it.address as? Inet4Address }
                ?.firstOrNull { !it.isLoopbackAddress && isPrivateLanIpv4(it) }
                ?.hostAddress

            val prefix = ipv4?.substringBeforeLast('.')
            val wifiInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                caps.transportInfo as? WifiInfo
            } else {
                @Suppress("DEPRECATION")
                wifiManager?.connectionInfo
            }
            val rawSsid = wifiInfo?.ssid?.trim('"')
            val cleanSsid = rawSsid?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }
            val linkSpeed = wifiInfo?.linkSpeed?.takeIf { it > 0 }

            return WifiStatusInfo(
                isWifiConnected = true,
                ssid = cleanSsid ?: "Wi-Fi LAN Connected",
                localIpv4 = ipv4,
                subnetPrefix = prefix,
                linkSpeedMbps = linkSpeed
            )
        }

        trySend(readCurrentWifi())

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(readCurrentWifi())
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(readCurrentWifi())
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                trySend(readCurrentWifi())
            }

            override fun onLost(network: Network) {
                trySend(readCurrentWifi())
            }
        }

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        runCatching { connectivityManager.registerNetworkCallback(request, callback) }

        awaitClose {
            runCatching { connectivityManager.unregisterNetworkCallback(callback) }
        }
    }

    suspend fun scanLocalNetworkForWindowsPcs(localIpv4: String?): List<DiscoveredWifiPc> =
        withContext(Dispatchers.IO) {
            if (localIpv4.isNullOrBlank()) return@withContext emptyList()
            val localInet = runCatching { InetAddress.getByName(localIpv4) }.getOrNull()
            if (localInet !is Inet4Address || !isPrivateLanIpv4(localInet)) {
                return@withContext emptyList()
            }

            _isScanning.value = true
            val foundMap = ConcurrentHashMap<String, DiscoveredWifiPc>()
            val nsdListeners = mutableListOf<NsdManager.DiscoveryListener>()
            var multicastLock: WifiManager.MulticastLock? = null

            try {
                multicastLock = wifiManager?.createMulticastLock("DeckLinkLanScan")?.apply {
                    setReferenceCounted(false)
                    acquire()
                }

                // 1. Android NsdManager mDNS discovery for Windows/Workstation/SMB/RDP services
                val serviceTypes = listOf("_workstation._tcp.", "_smb._tcp.", "_rdp._tcp.")
                serviceTypes.forEach { serviceType ->
                    val listener = createNsdListener(serviceType, foundMap)
                    runCatching {
                        nsdManager?.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
                        nsdListeners.add(listener)
                    }
                }

                // 2. WS-Discovery Probe (Windows 11 native Function Discovery on UDP 239.255.255.250:3702)
                val wsJob = CoroutineScope(Dispatchers.IO).launch {
                    probeWsDiscovery(foundMap)
                }

                // 3. Bounded parallel TCP check for Windows built-in ports (445 SMB, 3389 RDP, 5985 WinRM)
                val subnet = localIpv4.substringBeforeLast('.')
                val semaphore = Semaphore(32)
                val scanJobs = (1..254).mapNotNull { host ->
                    val targetIp = "$subnet.$host"
                    if (targetIp == localIpv4) return@mapNotNull null
                    async {
                        semaphore.withPermit {
                            val openPorts = probeWindowsBuiltInPorts(targetIp)
                            if (openPorts.isNotEmpty()) {
                                val resolvedName = runCatching {
                                    InetAddress.getByName(targetIp).canonicalHostName
                                        .takeIf { it != targetIp && it.isNotBlank() }
                                }.getOrNull() ?: "Windows PC ($targetIp)"
                                foundMap.merge(
                                    targetIp,
                                    DiscoveredWifiPc(
                                        ipAddress = targetIp,
                                        hostname = resolvedName,
                                        discoverySource = "Windows LAN Services",
                                        openWindowsPorts = openPorts
                                    )
                                ) { old, incoming ->
                                    old.copy(
                                        hostname = if (old.hostname.startsWith("Windows PC")) incoming.hostname else old.hostname,
                                        openWindowsPorts = (old.openWindowsPorts + incoming.openWindowsPorts).distinct().sorted()
                                    )
                                }
                                _discoveredPcs.value = foundMap.values.sortedBy { it.ipAddress }
                            }
                        }
                    }
                }

                scanJobs.awaitAll()
                delay(400)
                wsJob.cancel()
            } finally {
                nsdListeners.forEach { listener ->
                    runCatching { nsdManager?.stopServiceDiscovery(listener) }
                }
                runCatching {
                    if (multicastLock?.isHeld == true) multicastLock.release()
                }
                _isScanning.value = false
            }

            val finalResults = foundMap.values.sortedBy { it.ipAddress }
            _discoveredPcs.value = finalResults
            finalResults
        }

    private fun createNsdListener(
        serviceType: String,
        foundMap: ConcurrentHashMap<String, DiscoveredWifiPc>
    ): NsdManager.DiscoveryListener {
        return object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {}
            override fun onServiceFound(service: NsdServiceInfo) {
                @Suppress("DEPRECATION")
                nsdManager?.resolveService(service, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        @Suppress("DEPRECATION")
                        val host = serviceInfo.host
                        if (host is Inet4Address && isPrivateLanIpv4(host)) {
                            val ip = host.hostAddress ?: return
                            val name = serviceInfo.serviceName.ifBlank { host.hostName ?: ip }
                            foundMap.merge(
                                ip,
                                DiscoveredWifiPc(
                                    ipAddress = ip,
                                    hostname = name,
                                    discoverySource = "mDNS ($serviceType)",
                                    openWindowsPorts = listOfNotNull(serviceInfo.port.takeIf { it > 0 })
                                )
                            ) { old, incoming ->
                                old.copy(
                                    hostname = if (old.hostname.startsWith("Windows PC")) incoming.hostname else old.hostname,
                                    discoverySource = "${old.discoverySource}, mDNS",
                                    openWindowsPorts = (old.openWindowsPorts + incoming.openWindowsPorts).distinct().sorted()
                                )
                            }
                            _discoveredPcs.value = foundMap.values.sortedBy { it.ipAddress }
                        }
                    }
                })
            }

            override fun onServiceLost(service: NsdServiceInfo) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }
    }

    private fun probeWsDiscovery(foundMap: ConcurrentHashMap<String, DiscoveredWifiPc>) {
        val probeXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
                xmlns:wsa="http://schemas.xmlsoap.org/ws/2004/08/addressing"
                xmlns:wsd="http://schemas.xmlsoap.org/ws/2005/04/discovery">
                <soap:Header>
                    <wsa:To>urn:schemas-xmlsoap-org:ws:2005:04:discovery</wsa:To>
                    <wsa:Action>http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</wsa:Action>
                    <wsa:MessageID>urn:uuid:decklink-probe-0001</wsa:MessageID>
                </soap:Header>
                <soap:Body>
                    <wsd:Probe />
                </soap:Body>
            </soap:Envelope>
        """.trimIndent().toByteArray(Charsets.UTF_8)

        runCatching {
            DatagramSocket().use { socket ->
                socket.soTimeout = 1400
                socket.broadcast = true
                val group = InetAddress.getByName("239.255.255.250")
                socket.send(DatagramPacket(probeXml, probeXml.size, group, 3702))

                val buffer = ByteArray(4096)
                val endTime = System.currentTimeMillis() + 1500
                while (System.currentTimeMillis() < endTime) {
                    val response = DatagramPacket(buffer, buffer.size)
                    socket.receive(response)
                    val addr = response.address
                    if (addr is Inet4Address && isPrivateLanIpv4(addr)) {
                        val ip = addr.hostAddress ?: continue
                        val name = runCatching {
                            addr.canonicalHostName.takeIf { it != ip && it.isNotBlank() }
                        }.getOrNull() ?: "Windows PC ($ip)"
                        foundMap.merge(
                            ip,
                            DiscoveredWifiPc(
                                ipAddress = ip,
                                hostname = name,
                                discoverySource = "WS-Discovery (3702)",
                                openWindowsPorts = emptyList()
                            )
                        ) { old, incoming ->
                            old.copy(discoverySource = "${old.discoverySource}, WS-Discovery")
                        }
                        _discoveredPcs.value = foundMap.values.sortedBy { it.ipAddress }
                    }
                }
            }
        }
    }

    private fun probeWindowsBuiltInPorts(ip: String): List<Int> {
        val candidatePorts = listOf(445, 3389, 5985)
        val open = mutableListOf<Int>()
        for (port in candidatePorts) {
            runCatching {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, port), 160)
                    open.add(port)
                }
            }
        }
        return open
    }

    /**
     * Sends a standard IEEE 802.3 Wake-on-LAN Magic Packet over the local Wi-Fi broadcast subnet.
     * Requires zero Windows software (only BIOS/NIC WoL support on the HP ProBook).
     */
    suspend fun sendWakeOnLan(macAddress: String, subnetPrefix: String?): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cleanMac = macAddress.replace(":", "").replace("-", "").trim()
                require(cleanMac.length == 12 && cleanMac.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
                    "Invalid MAC address format (expected AA:BB:CC:DD:EE:FF)"
                }
                val macBytes = ByteArray(6) { i ->
                    cleanMac.substring(i * 2, i * 2 + 2).toInt(16).toByte()
                }
                val packetBytes = ByteArray(6 + 16 * 6)
                for (i in 0 until 6) packetBytes[i] = 0xFF.toByte()
                for (i in 0 until 16) {
                    System.arraycopy(macBytes, 0, packetBytes, 6 + i * 6, 6)
                }
                val broadcastIp = if (!subnetPrefix.isNullOrBlank()) "$subnetPrefix.255" else "255.255.255.255"
                val address = InetAddress.getByName(broadcastIp)
                require(address is Inet4Address && (isPrivateLanIpv4(address) || broadcastIp == "255.255.255.255")) {
                    "Wake-on-LAN restricted to local LAN broadcast"
                }
                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    socket.send(DatagramPacket(packetBytes, packetBytes.size, address, 9))
                }
                "Magic packet sent to $macAddress via $broadcastIp:9"
            }
        }

    companion object {
        fun isPrivateLanIpv4(address: Inet4Address): Boolean {
            if (address.isSiteLocalAddress) return true
            val b = address.address
            val first = b[0].toInt() and 0xFF
            val second = b[1].toInt() and 0xFF
            return (first == 10) ||
                (first == 172 && second in 16..31) ||
                (first == 192 && second == 168)
        }
    }
}
