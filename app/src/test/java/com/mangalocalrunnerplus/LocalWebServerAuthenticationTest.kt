package com.mangalocalrunnerplus

import java.io.File
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Base64
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalWebServerAuthenticationTest {

    private fun request(port: Int, authorization: String? = null, path: String = "/index.html", method: String = "GET"): String {
        Socket("127.0.0.1", port).use { socket ->
            socket.soTimeout = 3000
            val headers = buildString {
                append("$method $path HTTP/1.1\r\n")
                append("Host: localhost\r\n")
                if (authorization != null) append("Authorization: $authorization\r\n")
                append("Connection: close\r\n\r\n")
            }
            socket.getOutputStream().write(headers.toByteArray(StandardCharsets.US_ASCII))
            return socket.getInputStream().bufferedReader().readText()
        }
    }


    @Test
    fun servesWebProjectAssetsWithCorrectMimeTypes() {
        val root = Files.createTempDirectory("novarun-web-assets").toFile()
        File(root, "index.html").writeText("<html></html>")
        File(root, "styles.css").writeText("body { color: red; }")
        File(root, "app.js").writeText("console.log('ok')")
        File(root, "icon.svg").writeText("<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>")
        val port = ServerSocket(0).use { it.localPort }
        val server = LocalWebServer(root, port)
        try {
            assertTrue(server.start())
            assertTrue(request(port, path = "/styles.css").contains("Content-Type: text/css; charset=utf-8"))
            assertTrue(request(port, path = "/app.js").contains("Content-Type: application/javascript; charset=utf-8"))
            assertTrue(request(port, path = "/icon.svg").contains("Content-Type: image/svg+xml"))
        } finally {
            server.stop()
            root.deleteRecursively()
        }
    }

    @Test
    fun headRequestReturnsHeadersWithoutAssetBody() {
        val root = Files.createTempDirectory("novarun-head").toFile()
        File(root, "index.html").writeText("preview-body")
        val port = ServerSocket(0).use { it.localPort }
        val server = LocalWebServer(root, port)
        try {
            assertTrue(server.start())
            val response = request(port, method = "HEAD")
            assertTrue(response.startsWith("HTTP/1.1 200 OK"))
            assertTrue(response.contains("Content-Type: text/html; charset=utf-8"))
            assertTrue(!response.contains("preview-body"))
        } finally {
            server.stop()
            root.deleteRecursively()
        }
    }

    @Test
    fun lanRequestsWithoutCredentialsAreRejected() {
        val root = Files.createTempDirectory("novarun-auth").toFile()
        File(root, "index.html").writeText("private project")
        val port = ServerSocket(0).use { it.localPort }
        val server = LocalWebServer(root, port, bindToLan = true, accessToken = "test-token")
        try {
            assertTrue(server.start())
            val response = request(port)
            assertTrue(response.startsWith("HTTP/1.1 401 Unauthorized"))
            assertTrue(response.contains("WWW-Authenticate: Basic"))
            assertTrue(!response.contains("private project"))
        } finally {
            server.stop()
            root.deleteRecursively()
        }
    }

    @Test
    fun lanRequestsWithCorrectCredentialsAreAllowed() {
        val root = Files.createTempDirectory("novarun-auth").toFile()
        File(root, "index.html").writeText("private project")
        val port = ServerSocket(0).use { it.localPort }
        val server = LocalWebServer(root, port, bindToLan = true, accessToken = "test-token")
        try {
            assertTrue(server.start())
            val encoded = Base64.getEncoder().encodeToString(
                "novarun:test-token".toByteArray(StandardCharsets.UTF_8)
            )
            val response = request(port, "Basic $encoded")
            assertTrue(response.startsWith("HTTP/1.1 200 OK"))
            assertTrue(response.contains("private project"))
        } finally {
            server.stop()
            root.deleteRecursively()
        }
    }
}
