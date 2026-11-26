package com.mangalocalrunnerplus

import java.io.File
import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Files
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalWebServerImageMimeTest {
    @Test
    fun servesCommonImageFormatsWithImageContentTypes() {
        val root = Files.createTempDirectory("novarun-image-mime").toFile()
        File(root, "index.html").writeText("<html>image-test</html>")
        val fixtures = linkedMapOf(
            "png" to "image/png",
            "jpg" to "image/jpeg",
            "jpeg" to "image/jpeg",
            "gif" to "image/gif",
            "webp" to "image/webp",
            "ico" to "image/x-icon",
            "avif" to "image/avif",
            "bmp" to "image/bmp",
            "tif" to "image/tiff",
            "tiff" to "image/tiff",
            "heic" to "image/heic",
            "heif" to "image/heif"
        )
        fixtures.keys.forEach { extension ->
            File(root, "asset.$extension").writeBytes(byteArrayOf(0x41, 0x42, 0x43))
        }
        val port = ServerSocket(0).use { it.localPort }
        val server = LocalWebServer(projectDirectory = root, port = port, bindToLan = false)

        fun response(path: String): String = Socket("127.0.0.1", port).use { socket ->
            socket.soTimeout = 5000
            socket.getOutputStream().write(
                "GET /$path HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n"
                    .toByteArray(Charsets.US_ASCII)
            )
            socket.getOutputStream().flush()
            socket.getInputStream().readBytes().toString(Charsets.ISO_8859_1)
        }

        try {
            assertTrue(server.start())
            fixtures.forEach { (extension, mime) ->
                val result = response("asset.$extension")
                assertTrue("Missing HTTP 200 for .$extension", result.startsWith("HTTP/1.1 200 OK"))
                assertTrue("Wrong MIME for .$extension: $result", result.contains("Content-Type: $mime"))
                assertTrue("Image payload missing for .$extension", result.endsWith("ABC"))
            }
        } finally {
            server.stop()
            root.deleteRecursively()
        }
    }
}
