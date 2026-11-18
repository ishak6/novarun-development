package com.mangalocalrunnerplus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LanNetworkAddressSelectionTest {
    @Test
    fun prefersWifiAddressOverCellularOrOtherInterfaceAddress() {
        assertEquals("192.168.1.24", LanNetworkAddress.selectAddress(
            wifiAddresses = listOf("192.168.1.24"),
            interfaceAddresses = listOf("10.12.0.5")
        ))
    }

    @Test
    fun fallsBackWhenWifiAddressIsUnavailable() {
        assertEquals("10.12.0.5", LanNetworkAddress.selectAddress(
            wifiAddresses = emptyList(),
            interfaceAddresses = listOf("10.12.0.5")
        ))
    }

    @Test
    fun returnsNullWhenNoReachableAddressExists() {
        assertNull(LanNetworkAddress.selectAddress(
            wifiAddresses = emptyList(),
            interfaceAddresses = emptyList()
        ))
    }
}
