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

    @Test
    fun lanAuthenticationIsEnforcedInsideTlsSession() {
        val root = Files.createTempDirectory("novarun-tls-auth").toFile()
        File(root, "index.html").writeText("tls-private-project")
        val port = ServerSocket(0).use { it.localPort }
        val server = LocalWebServer(
            projectDirectory = root,
            port = port,
            bindToLan = true,
            accessToken = "tls-test-secret",
            sslContext = testServerContext()
        )
        val clientContext = testClientContext()

        fun request(authorization: String? = null): String {
            return (clientContext.socketFactory
                .createSocket("127.0.0.1", port) as SSLSocket).use { socket ->
                socket.soTimeout = 5000
                socket.startHandshake()
                val headers = buildString {
                    append("GET /index.html HTTP/1.1\\r\\n")
                    append("Host: localhost\\r\\n")
                    if (authorization != null) append("Authorization: $authorization\\r\\n")
                    append("Connection: close\\r\\n\\r\\n")
                }
                socket.getOutputStream().write(headers.toByteArray(Charsets.US_ASCII))
                socket.getInputStream().bufferedReader().readText()
            }
        }

        try {
            assertTrue(server.start())
            val denied = request()
            assertTrue(denied.startsWith("HTTP/1.1 401 Unauthorized"))
            assertTrue(!denied.contains("tls-private-project"))

            val encoded = Base64.getEncoder().encodeToString(
                "novarun:tls-test-secret".toByteArray(Charsets.UTF_8)
            )
            val allowed = request("Basic $encoded")
            assertTrue(allowed.startsWith("HTTP/1.1 200 OK"))
            assertTrue(allowed.contains("tls-private-project"))
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
        const val TEST_PRIVATE_KEY = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQDAiVW6dKcwkR057NTPaV5EGyhS/PbcV6R3vAbEnaJspLf2QAm8QgwswUa+M4A/6iNgIZW+OEdm35T6983vby8IOplwzvewe1/te8Ph05r0ToPTMYPMizzlLN0w5vp3radYkWQz+7cNZ64iKItnylPIJcfAMOqZY1JCdzIMkY7zR4KY2fWL72ziudWNQ/x4+8T6M97y1xj0rKxMmA13L5BgKzd1l6bzs2/nYfeJPwMtIbmHTJ2VICT0HVLzDCb+3T1ifxu/lRJS5sRfBL+ukow0s3cWMZVqaGBHDT48aNu55kMyyUGIOwGhMoYNPt48MBlzbq8OszJAB/fiMnjsZF+nAgMBAAECggEABSzLzparFb2YxQFOGQMSgw8dztk4w2Rhqcw2fFtmTbhBr486E8LMs/k/7wXQWSdwavmMHlBB7wxsYzB86wYYhAabMDKH6sjuI6JzeeLno9Bmb6tBiSJm8Rv7GADlU4kp2dISmOkXmvch8Y4YbSI9jUAitdI6xAOs5tdwU3EoH/bMptA3whUFeBbWAENEsRaaK7sVe01TGkCzgS2s9c9wdVooWxIaHMDA1SZfycstnk3vFuAPITGlUVltJpRIN3uTKVFVsj15CaPvaXy22U2qwUTNg8HLqltSNRixbm2JxrTIg13OiUTUlxZXMMYX6HFsTqjytHXCoHoVl2PhUQs2kQKBgQDg0JUIcjf3ljFLYA8buv+qQ8/s9ha5B7HXqs6XcBx8/WVqioy95z2XhhK7VCTuMD2eceQD0umuA20F9ORJUDiyn75F4dPG8oUDxXzY6QYR8SAGSoK/KbpZslRn3BA4Pv0m02CI+/eg1o3tc777LBJK4hOelC0mJUioH3SAKctRtwKBgQDbPoPFdEopfjEu0miMsG0iSz6jFt3Rau/3T5OF8YHsikkmMM63gxHYZOmodFNa5Pn+VfyL6ltN4o5dtHubJ6kr8raD0PD+xVFlIBa2clg5BRNKWNOd6jXBV8UvvLVUdRa2eWBzgTzJOT/qCnPxhJ+fnSm+hoS6B+VzcW8G8OGhkQKBgBziL4OaSR6CKWspuEczy00w0TH8nAbBulOfGlPSj5ZvZ+w8SMDNSNiC3T08HlKL2B7XOxhSrLsyrXsqb3oL2Hz6kROhY/gXiIdnhBbKOYFsnVSxlgLBdbO4eaeuWlcfoJ6wYaK33KTJbAqp6EuU6fS8/xBMsVBJUgBnqiwdMoYZAoGAK/3JNu90igrWF40EiWZFH6J1eQf3jjOk3rzK3w9sgGfm8ZDNiaj1BbSJ5dEhgJV9S/k46WIm2+PfqRGz05gLq9PRp3VfRd+Vy3K62/TpmtdX3Y0hOQhrrNXNrRxyTfYIjqigH80Iw1+HYZCjXwy7cq14WNBpBWLcOWPDYWMoAuECgYAeQXWCPTaqm4XeSGQMdqzEgL5JxhnUmS6+ErwQr/9RJ6JsxRCr2z5npZBx+lfJ0cjrFZazcoZgURuHDVrJoVggr5zCNtbznYip+5Ppa4cXAS41iEXbtRZ4vJmVuVqy/jwkq1Iw5ZKE2VzqiR+9QDY655kNN/iIbRnWB84xBZiTJQ=="
        const val TEST_CERTIFICATE = "MIIDFzCCAf+gAwIBAgIUXw1qDRS+1QfvbRZW3hOESZWWBYMwDQYJKoZIhvcNAQELBQAwGzEZMBcGA1UEAwwQTm92YVJ1biBUZXN0IFRMUzAeFw0yNjExMDkxNTA1NDBaFw0zNjExMDYxNTA1NDBaMBsxGTAXBgNVBAMMEE5vdmFSdW4gVGVzdCBUTFMwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDAiVW6dKcwkR057NTPaV5EGyhS/PbcV6R3vAbEnaJspLf2QAm8QgwswUa+M4A/6iNgIZW+OEdm35T6983vby8IOplwzvewe1/te8Ph05r0ToPTMYPMizzlLN0w5vp3radYkWQz+7cNZ64iKItnylPIJcfAMOqZY1JCdzIMkY7zR4KY2fWL72ziudWNQ/x4+8T6M97y1xj0rKxMmA13L5BgKzd1l6bzs2/nYfeJPwMtIbmHTJ2VICT0HVLzDCb+3T1ifxu/lRJS5sRfBL+ukow0s3cWMZVqaGBHDT48aNu55kMyyUGIOwGhMoYNPt48MBlzbq8OszJAB/fiMnjsZF+nAgMBAAGjUzBRMB0GA1UdDgQWBBSdasGDPeWixf/sbVZM3O+rubSdkjAfBgNVHSMEGDAWgBSdasGDPeWixf/sbVZM3O+rubSdkjAPBgNVHRMBAf8EBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQAkjUoqP6xYz0rm4RnbhImj1t6tUrBrQ+MYyTkTH+9+RImM27pVXFjFKHkjMva5y8TFVbvj113AFLXjQBifywq/+29H4W8Ws6zhhnDpDfzmKyudkmTy1ZAzMMkZV/cwlFaQX0Repte3K85oRqUJiKVa8wOvM30Vshor5jdHNkFiByOCEJqge9NCF7ilPctannNzmjv50SrznUdgR+JbHtJlJjQ8uuBa3cQCUPqqPaQ9YYugnyNQqviK4mdv3P8F/6XJZZEOoKaLEl3ZBMPDYnumhLh2UzsrSxHRTAXhIBte9kkrWxXCRSga5v3RXBftyJKGnvdDIVF5UeCOSPxrnzNy"
    }
}
