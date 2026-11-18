package com.mangalocalrunnerplus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LanNetworkAddressSelectionTest {
    @Test
    fun prefersHotspotGatewayOverWifiUpstreamWhenBothAreActive() {
        assertEquals(
            "192.168.43.1",
            LanNetworkAddress.selectAddress(
                hotspotAddresses = listOf("192.168.43.1"),
                wifiAddresses = listOf("192.168.1.24"),
                interfaceAddresses = listOf("10.12.0.5")
            )
        )
    }

    @Test
    fun prefersWifiRouterAddressWhenHotspotIsNotActive() {
        assertEquals(
            "192.168.1.24",
            LanNetworkAddress.selectAddress(
                hotspotAddresses = emptyList(),
                wifiAddresses = listOf("192.168.1.24"),
                interfaceAddresses = listOf("10.12.0.5")
            )
        )
    }

    @Test
    fun identifiesSecondaryWlanAsHotspotWhenRouterWifiIsAlsoConnected() {
        assertEquals(
            listOf("192.168.43.1"),
            LanNetworkAddress.findHotspotCandidates(
                interfaceAddresses = listOf(
                    "wlan0" to "192.168.1.24",
                    "wlan1" to "192.168.43.1"
                ),
                wifiAddresses = listOf("192.168.1.24")
            )
        )
    }

    @Test
    fun recognizesVendorSpecificHotspotInterfaceNames() {
        assertEquals(
            listOf("192.168.43.1", "192.168.44.1", "192.168.45.1"),
            LanNetworkAddress.findHotspotCandidates(
                interfaceAddresses = listOf(
                    "ap0" to "192.168.43.1",
                    "swlan0" to "192.168.44.1",
                    "softap0" to "192.168.45.1"
                ),
                wifiAddresses = emptyList()
            )
        )
    }

    @Test
    fun fallsBackToInterfaceAddressWhenNoWifiTransportIsAvailable() {
        assertEquals(
            "192.168.43.1",
            LanNetworkAddress.selectAddress(
                hotspotAddresses = emptyList(),
                wifiAddresses = emptyList(),
                interfaceAddresses = listOf("192.168.43.1", "10.12.0.5")
            )
        )
    }

    @Test
    fun returnsNullWhenNoReachableAddressExists() {
        assertNull(
            LanNetworkAddress.selectAddress(
                hotspotAddresses = emptyList(),
                wifiAddresses = emptyList(),
                interfaceAddresses = emptyList()
            )
        )
    }
}
