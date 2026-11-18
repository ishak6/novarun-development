package com.mangalocalrunnerplus

import org.junit.Assert.assertEquals
import org.junit.Test

class LanNetworkAddressSelectionTest {
    @Test
    fun wifiAddressIsPreferredOverOtherPrivateAddresses() {
        val wifi = listOf("192.168.1.24")
        val other = listOf("10.12.0.5")
        assertEquals("192.168.1.24", wifi.firstOrNull() ?: other.firstOrNull())
    }

    @Test
    fun missingWifiAddressFallsBackToAvailablePrivateAddress() {
        val wifi = emptyList<String>()
        val other = listOf("10.12.0.5")
        assertEquals("10.12.0.5", wifi.firstOrNull() ?: other.firstOrNull())
    }
}
