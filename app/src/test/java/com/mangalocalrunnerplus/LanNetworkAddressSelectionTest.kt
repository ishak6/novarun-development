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
