package com.mangalocalrunnerplus

import java.io.File
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalhostLoopbackHttpTest {
    @Test
    fun servesLocalProjectOverDeviceLoopbackHttp() {
        val root = Files.createTempDirectory("novarun-loopback").toFile()
        File(root, "index.html").writeText("<html>local-preview-ok</html>")
        val port = ServerSocket(0).use { it.localPort }
        val server = LocalWebServer(
            projectDirectory = root,
            port = port,
            bindToLan = false,
            defaultDocument = "index.html"
        )
        try {
            assertTrue("Loopback server should start", server.start())
            Socket("127.0.0.1", port).use { socket ->
                socket.soTimeout = 3000
                socket.getOutputStream().write(
                    "GET / HTTP/1.1\r\nHost: 127.0.0.1\r\nConnection: close\r\n\r\n"
                        .toByteArray(StandardCharsets.US_ASCII)
                )
                val response = socket.getInputStream().bufferedReader().readText()
                assertTrue(response.startsWith("HTTP/1.1 200 OK"))
                assertTrue(response.contains("local-preview-ok"))
            }
        } finally {
            server.stop()
            root.deleteRecursively()
        }
    }
}
