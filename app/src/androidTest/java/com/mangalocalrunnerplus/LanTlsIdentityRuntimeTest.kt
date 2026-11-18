package com.mangalocalrunnerplus

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.net.ServerSocket
import java.nio.file.Files
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LanTlsIdentityRuntimeTest {
    @Test
    fun androidKeystoreIdentityPersistsAndCompletesTlsHandshake() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val identity = LanTlsIdentity.create(context)
        val fingerprint = identity.sha256Fingerprint

        assertTrue(
            "Keystore certificate fingerprint must be a SHA-256 hex digest",
            fingerprint.matches(Regex("([0-9A-F]{2}:){31}[0-9A-F]{2}"))
        )
        assertEquals(
            "Repeated identity lookup must return the same persistent certificate",
            fingerprint,
            LanTlsIdentity.create(context).sha256Fingerprint
        )

        val root = Files.createTempDirectory(context.cacheDir.toPath(), "novarun-keystore-tls-").toFile()
        File(root, "index.html").writeText("<html>android-keystore-tls-ok</html>")
        val port = ServerSocket(0).use { it.localPort }
        val server = LocalWebServer(
            projectDirectory = root,
            port = port,
            bindToLan = true,
            sslContext = identity.sslContext,
            tlsProtocols = identity.enabledProtocols
        )
        val trustOnlyExpectedCertificate = object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()

            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit

            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                assertTrue("TLS peer must provide a certificate", chain.isNotEmpty())
                val actual = MessageDigest.getInstance("SHA-256")
                    .digest(chain[0].encoded)
                    .joinToString(":") { byte -> "%02X".format(byte) }
                assertEquals("TLS server certificate must match the Keystore fingerprint", fingerprint, actual)
            }
        }
        val clientContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustOnlyExpectedCertificate), SecureRandom())
        }

        try {
            assertTrue("TLS listener must start with the Android Keystore identity", server.start())
            assertTrue("TLS 1.2 must remain enabled", "TLSv1.2" in identity.enabledProtocols)
            identity.enabledProtocols.forEach { protocol ->
                val response = (clientContext.socketFactory
                    .createSocket("127.0.0.1", port) as SSLSocket).use { socket ->
                    socket.enabledProtocols = arrayOf(protocol)
                    socket.soTimeout = 5_000
                    socket.startHandshake()
                    socket.getOutputStream().write(
                        "GET /index.html HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n"
                            .toByteArray(Charsets.US_ASCII)
                    )
                    socket.getInputStream().bufferedReader().readText()
                }

                assertTrue("$protocol response must return HTTP 200", response.startsWith("HTTP/1.1 200 OK"))
                assertTrue("$protocol response must serve the selected project", response.contains("android-keystore-tls-ok"))
            }
        } finally {
            server.stop()
            root.deleteRecursively()
        }
    }
}
