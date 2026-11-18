package com.mangalocalrunnerplus

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.LinkProperties
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

/**
 * Resolves the IPv4 address other devices can use to reach this phone.
 *
 * Prefer an address assigned to a Wi-Fi transport (including a Wi-Fi hotspot)
 * instead of whichever private address happens to be returned first by the
 * interface enumeration. Fall back to active Wi-Fi-like interfaces for devices
 * whose tethering interface is not exposed through ConnectivityManager.
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

        val candidates = interfaces
            .filter { it.isUp && !it.isLoopback }
            .map { networkInterface ->
                val name = networkInterface.name.lowercase()
                val priority = when {
                    name.startsWith("wlan") || name.startsWith("wifi") -> 0
                    name.startsWith("ap") || name.startsWith("swlan") -> 1
                    name.startsWith("p2p") -> 2
                    else -> 3
                }
                priority to runCatching {
                    Collections.list(networkInterface.inetAddresses)
                        .filterIsInstance<Inet4Address>()
                        .filter(::isUsablePrivateIpv4)
                        .mapNotNull { it.hostAddress }
                }.getOrDefault(emptyList())
            }
            .sortedBy { it.first }

        val interfaceAddresses = candidates.flatMap { it.second }
        return selectAddress(wifiAddresses, interfaceAddresses)
    }

    internal fun selectAddress(
        wifiAddresses: List<String>,
        interfaceAddresses: List<String>
    ): String? = wifiAddresses.firstOrNull() ?: interfaceAddresses.firstOrNull()

    private fun isUsablePrivateIpv4(address: Inet4Address): Boolean =
        !address.isAnyLocalAddress &&
            !address.isLoopbackAddress &&
            !address.isLinkLocalAddress &&
            !address.isMulticastAddress &&
            address.isSiteLocalAddress
}
