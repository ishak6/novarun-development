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
 * When this phone is sharing a hotspot, clients must use the phone's AP-side
 * address, not its Wi-Fi upstream address. When hotspot mode is not active,
 * prefer the address belonging to the active Wi-Fi network (router mode).
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
        }

        val interfaces = runCatching {
            Collections.list(NetworkInterface.getNetworkInterfaces())
        }.getOrDefault(emptyList())

        val activeInterfaces = interfaces.filter { it.isUp && !it.isLoopback }
        val hotspotAddresses = activeInterfaces
            .filter { isHotspotInterface(it.name) }
            .flatMap(::privateIpv4Addresses)

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

    private fun isHotspotInterface(name: String): Boolean {
        val normalized = name.lowercase()
        return normalized.startsWith("ap") ||
            normalized.startsWith("swlan") ||
            normalized.startsWith("softap")
    }

    private fun interfacePriority(name: String): Int {
        val normalized = name.lowercase()
        return when {
            isHotspotInterface(normalized) -> 0
            normalized.startsWith("wlan") || normalized.startsWith("wifi") -> 1
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
