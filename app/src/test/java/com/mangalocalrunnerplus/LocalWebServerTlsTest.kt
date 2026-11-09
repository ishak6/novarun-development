package com.mangalocalrunnerplus

import java.io.File
import java.net.ServerSocket
import java.security.KeyFactory
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class LocalWebServerTlsTest {

    @Test
    fun servesProjectOverTlsAndCompletesHandshake() {
        val root = Files.createTempDirectory("novarun-tls-test").toFile()
        File(root, "index.html").writeText("encrypted-preview")
        val port = ServerSocket(0).use { it.localPort }
        val serverContext = testServerContext()
        val clientContext = testClientContext()
        val server = LocalWebServer(
            projectDirectory = root,
            port = port,
            bindToLan = true,
            sslContext = serverContext
        )

        try {
            assertTrue(server.start())
            val response = (clientContext.socketFactory
                .createSocket("127.0.0.1", port) as SSLSocket).use { socket ->
                socket.soTimeout = 5000
                socket.startHandshake()
                socket.getOutputStream().write(
                    "GET /index.html HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n"
                        .toByteArray(Charsets.US_ASCII)
                )
                socket.getInputStream().bufferedReader().readText()
            }

            assertTrue(response.startsWith("HTTP/1.1 200 OK"))
            assertTrue(response.contains("encrypted-preview"))
        } finally {
            server.stop()
            root.deleteRecursively()
        }
    }

    private fun testServerContext(): SSLContext {
        val certificateBytes = Base64.getDecoder().decode(TEST_CERTIFICATE)
        val certificate = CertificateFactory.getInstance("X.509")
            .generateCertificate(certificateBytes.inputStream())
        val privateKeyBytes = Base64.getDecoder().decode(TEST_PRIVATE_KEY)
        val privateKey = KeyFactory.getInstance("RSA")
            .generatePrivate(PKCS8EncodedKeySpec(privateKeyBytes))
        val password = "test-only".toCharArray()
        val keyStore = KeyStore.getInstance("PKCS12").apply {
            load(null, password)
            setKeyEntry("test", privateKey, password, arrayOf(certificate))
        }
        val keyManagers = KeyManagerFactory.getInstance(
            KeyManagerFactory.getDefaultAlgorithm()
        ).apply {
            init(keyStore, password)
        }.keyManagers
        return SSLContext.getInstance("TLS").apply {
            init(keyManagers, null, SecureRandom())
        }
    }

    private fun testClientContext(): SSLContext {
        val trustAllForTestOnly = arrayOf(object : X509TrustManager {
            override fun getAcceptedIssuers() = emptyArray<java.security.cert.X509Certificate>()
            override fun checkClientTrusted(
                chain: Array<java.security.cert.X509Certificate>,
                authType: String
            ) = Unit
            override fun checkServerTrusted(
                chain: Array<java.security.cert.X509Certificate>,
                authType: String
            ) = Unit
        })
        return SSLContext.getInstance("TLS").apply {
            init(null, trustAllForTestOnly, SecureRandom())
        }
    }

    private companion object {
        // Test-only self-signed fixture; never used by the production TLS identity.
        const val TEST_PRIVATE_KEY = "$key"
        const val TEST_CERTIFICATE = "$cert"
    }
}
