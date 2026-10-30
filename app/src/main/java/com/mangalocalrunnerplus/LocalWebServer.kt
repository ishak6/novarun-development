
package com.mangalocalrunnerplus

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class LocalWebServer(
    private val projectDirectory: File,
    private val port: Int = 8080
) {

    private val running = AtomicBoolean(false)

    private var serverSocket: ServerSocket? = null

    private val executor = Executors.newCachedThreadPool()

    val isRunning: Boolean
        get() = running.get()

    fun start(): Boolean {

        if (running.get()) {
            return true
        }

        val root = try {
            projectDirectory.canonicalFile
        } catch (exception: IOException) {
            return false
        }

        if (!root.exists() || !root.isDirectory || !root.canRead()) {
            return false
        }

        return try {

            val socket = ServerSocket(port)

            serverSocket = socket
            running.set(true)

            executor.execute {

                while (running.get()) {

                    try {
                        val client = socket.accept()

                        executor.execute {
                            handleClient(client, root)
                        }

                    } catch (exception: IOException) {

                        if (running.get()) {
                            stop()
                        }
                    }
                }
            }

            true

        } catch (exception: IOException) {

            running.set(false)
            false
        }
    }

    private fun handleClient(
        socket: Socket,
        root: File
    ) {

        socket.use { client ->

            client.soTimeout = 10000

            val input = BufferedInputStream(
                client.getInputStream()
            )

            val output = BufferedOutputStream(
                client.getOutputStream()
            )

            try {

                val requestLine = input.bufferedReader(
                    StandardCharsets.UTF_8
                ).readLine() ?: return

                val parts = requestLine.split(" ")

                if (parts.size < 2) {
                    sendError(output, 400, "Bad Request")
                    return
                }

                val method = parts[0]
                val rawPath = parts[1]

                if (method != "GET" && method != "HEAD") {
                    sendError(output, 405, "Method Not Allowed")
                    return
                }

                val path = try {
                    URLDecoder.decode(
                        rawPath.substringBefore("?"),
                        StandardCharsets.UTF_8.name()
                    )
                } catch (exception: Exception) {
                    sendError(output, 400, "Bad Request")
                    return
                }

                if (
                    path.contains('\\') ||
                    path.contains('\u0000')
                ) {
                    sendError(output, 400, "Bad Request")
                    return
                }

                val requestedPath = path.removePrefix("/")
                    .ifEmpty { "index.html" }

                val file = try {
                    File(root, requestedPath).canonicalFile
                } catch (exception: IOException) {
                    sendError(output, 403, "Forbidden")
                    return
                }

                val insideRoot =
                    file.path.startsWith(
                        root.path + File.separator
                    ) || file == root

                if (!insideRoot) {
                    sendError(output, 403, "Forbidden")
                    return
                }

                val target = if (file.isDirectory) {
                    File(file, "index.html").canonicalFile
                } else {
                    file
                }

                val targetInsideRoot =
                    target.path.startsWith(
                        root.path + File.separator
                    )

                if (!targetInsideRoot || !target.isFile || !target.canRead()) {
                    sendError(output, 404, "Not Found")
                    return
                }

                val mimeType = getMimeType(target.extension)

                val fileLength = target.length()

                output.write(
                    (
                        "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: $mimeType\r\n" +
                        "Content-Length: $fileLength\r\n" +
                        "Connection: close\r\n" +
                        "X-Content-Type-Options: nosniff\r\n" +
                        "\r\n"
                    ).toByteArray(StandardCharsets.UTF_8)
                )

                if (method == "GET") {
                    target.inputStream().use { fileInput ->
                        fileInput.copyTo(output)
                    }
                }

                output.flush()

            } catch (exception: Exception) {

                runCatching {
                    sendError(output, 500, "Internal Server Error")
                }
            }
        }
    }

    private fun sendError(
        output: BufferedOutputStream,
        status: Int,
        message: String
    ) {

        val body = message.toByteArray(
            StandardCharsets.UTF_8
        )

        val response =
            "HTTP/1.1 $status $message\r\n" +
            "Content-Type: text/plain; charset=utf-8\r\n" +
            "Content-Length: ${body.size}\r\n" +
            "Connection: close\r\n" +
            "\r\n"

        output.write(
            response.toByteArray(StandardCharsets.UTF_8)
        )

        output.write(body)
        output.flush()
    }

    private fun getMimeType(extension: String): String {

        return when (extension.lowercase()) {

            "html", "htm" -> "text/html; charset=utf-8"
            "css" -> "text/css; charset=utf-8"
            "js", "mjs" -> "application/javascript; charset=utf-8"
            "json" -> "application/json; charset=utf-8"
            "txt" -> "text/plain; charset=utf-8"
            "xml" -> "application/xml; charset=utf-8"
            "svg" -> "image/svg+xml"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "ico" -> "image/x-icon"
            "pdf" -> "application/pdf"
            "wasm" -> "application/wasm"
            "mp3" -> "audio/mpeg"
            "mp4" -> "video/mp4"

            else -> "application/octet-stream"
        }
    }

    fun stop() {

        running.set(false)

        runCatching {
            serverSocket?.close()
        }

        serverSocket = null
    }
}
