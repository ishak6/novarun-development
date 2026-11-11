package com.mangalocalrunnerplus

import android.security.NetworkSecurityPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class NetworkSecurityPolicyTest {
    @Test
    fun cleartextIsAllowedOnlyForLoopbackHosts() {
        val policy = NetworkSecurityPolicy.getInstance()

        assertTrue("IPv4 loopback is required by the in-app preview", policy.isCleartextTrafficPermitted("127.0.0.1"))
        assertTrue("localhost is required for local development tools", policy.isCleartextTrafficPermitted("localhost"))
        assertFalse("Public hostnames must require TLS", policy.isCleartextTrafficPermitted("example.com"))
        assertFalse("LAN addresses must require TLS", policy.isCleartextTrafficPermitted("192.168.1.20"))
    }
}
