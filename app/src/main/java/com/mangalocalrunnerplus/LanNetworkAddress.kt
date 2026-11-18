package com.mangalocalrunnerplus

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

/**
 * Resolves the IPv4 address another device can use to reach this phone.
 *
 * Hotspot AP interfaces are preferred over the Wi-Fi client/upstream address.
 * Some Android vendors expose the AP as wlan1 rather than ap0/swlan0, so an
 * unmatched wlan interface is treated as a hotspot candidate when a distinct
 * Wi-Fi transport address is also present.
 */
internal object LanNetworkAddress {
    fun find(context: Context): String? {
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager

        val wifiNetworks = runCatching {
            connectivity?.allNetworks.orEmpty().filter { network: Network ->
                connectivity?.getNetworkCapabilities(network)
                    ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            }
        }.getOrDefault(emptyList())

        val wifiAddresses = wifiNetworks.flatMap { network ->
            runCatching {
                connectivity?.getLinkProperties(network)
                    ?.linkAddresses
                    .orEmpty()
                    .mapNotNull { it.address as? Inet4Address }
                    .filter(::isUsablePrivateIpv4)
                    .mapNotNull { it.hostAddress }
            }.getOrDefault(emptyList())
        }.distinct()

        val interfaces = runCatching {
            Collections.list(NetworkInterface.getNetworkInterfaces())
        }.getOrDefault(emptyList())

        val activeInterfaces = interfaces.filter { it.isUp && !it.isLoopback }
        val interfaceEntries = activeInterfaces.flatMap { networkInterface ->
            privateIpv4Addresses(networkInterface).map { networkInterface.name to it }
        }
        val hotspotAddresses = findHotspotCandidates(interfaceEntries, wifiAddresses)
        val interfaceAddresses = activeInterfaces
            .sortedBy { interfacePriority(it.name) }
            .flatMap(::privateIpv4Addresses)

        return selectAddress(
            hotspotAddresses = hotspotAddresses,
            wifiAddresses = wifiAddresses,
            interfaceAddresses = interfaceAddresses
        )
    }

    internal fun selectAddress(
        hotspotAddresses: List<String>,
        wifiAddresses: List<String>,
        interfaceAddresses: List<String>
    ): String? = hotspotAddresses.firstOrNull()
        ?: wifiAddresses.firstOrNull()
        ?: interfaceAddresses.firstOrNull()

    /**
     * Handles vendor AP names (ap0/swlan0/softap0) and the common dual-WLAN
     * layout where wlan0 is the router client and wlan1 is the phone hotspot.
     */
    internal fun findHotspotCandidates(
        interfaceAddresses: List<Pair<String, String>>,
        wifiAddresses: List<String>
    ): List<String> = interfaceAddresses
        .filter { (name, address) ->
            isHotspotInterface(name) ||
                (isWlanInterface(name) && address !in wifiAddresses)
        }
        .map { it.second }
        .distinct()

    private fun isHotspotInterface(name: String): Boolean {
        val normalized = name.lowercase()
        return normalized.startsWith("ap") ||
            normalized.startsWith("swlan") ||
            normalized.startsWith("softap")
    }

    private fun isWlanInterface(name: String): Boolean {
        val normalized = name.lowercase()
        return normalized.startsWith("wlan") || normalized.startsWith("wifi")
    }

    private fun interfacePriority(name: String): Int {
        val normalized = name.lowercase()
        return when {
            isHotspotInterface(normalized) -> 0
            isWlanInterface(normalized) -> 1
            normalized.startsWith("p2p") -> 2
            else -> 3
        }
    }

    private fun privateIpv4Addresses(networkInterface: NetworkInterface): List<String> =
        runCatching {
            Collections.list(networkInterface.inetAddresses)
                .filterIsInstance<Inet4Address>()
                .filter(::isUsablePrivateIpv4)
                .mapNotNull { it.hostAddress }
        }.getOrDefault(emptyList())

    private fun isUsablePrivateIpv4(address: Inet4Address): Boolean =
        !address.isAnyLocalAddress &&
            !address.isLoopbackAddress &&
            !address.isLinkLocalAddress &&
            !address.isMulticastAddress &&
            address.isSiteLocalAddress
}
