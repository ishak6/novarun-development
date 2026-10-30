
package com.mangalocalrunnerplus

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class LocalWebServer(
    private val projectDirectory: File,
    private val port: Int = 8080,
    private val bindToLan: Boolean = true
) {

    private val running = AtomicBoolean(false)

    @Volatile
    private var serverSocket: ServerSocket? = null

    @Volatile
    private var executor: ExecutorService? = null

    val isRunning: Boolean
        get() = running.get()

    @Synchronized
    fun start(): Boolean {

        if (running.get()) {
            return true
        }

        val root = try {
            projectDirectory.canonicalFile
        } catch (exception: Exception) {
            return false
        }

        if (!root.exists() || !root.isDirectory || !root.canRead()) {
            return false
        }

        if (port !in 1024..65535) {
            return false
        }

        return try {

            // LAN mode listens on all network interfaces.
            // Local-only mode listens only on localhost.
            val bindAddress = if (bindToLan) {
                InetAddress.getByName("0.0.0.0")
            } else {
                InetAddress.getLoopbackAddress()
            }

            val socket = ServerSocket().apply {
                reuseAddress = true
                bind(
                    InetSocketAddress(
                        bindAddress,
                        port
                    )
                )
            }

            val pool = Executors.newCachedThreadPool()

            serverSocket = socket
            executor = pool
            running.set(true)

            pool.execute {

                while (running.get()) {

                    try {

                        val client = socket.accept()

                        runCatching {
                            pool.execute {
                                handleClient(client, root)
                            }
                        }.onFailure {
                            runCatching { client.close() }
                        }

                    } catch (exception: IOException) {

                        if (running.get()) {
                            stop()
                        }
                    }
                }
            }

            true

        } catch (exception: Exception) {

            running.set(false)

            runCatching {
                serverSocket?.close()
            }

            runCatching {
                executor?.shutdownNow()
            }

            serverSocket = null
            executor = null

            false
        }
    }

    private fun handleClient(
        socket: java.net.Socket,
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

            var method = ""

            try {

                val requestLine = readBoundedLine(
                    input,
                    8192
                ) ?: return

                val parts = requestLine.split(" ")

                if (parts.size != 3) {
                    sendError(output, 400, "Bad Request")
                    return
                }

                method = parts[0]

                val requestTarget = parts[1]
                val httpVersion = parts[2]

                if (
                    httpVersion != "HTTP/1.1" &&
                    httpVersion != "HTTP/1.0"
                ) {
                    sendError(output, 400, "Bad Request")
                    return
                }

                if (method != "GET" && method != "HEAD") {
                    sendError(output, 405, "Method Not Allowed")
                    return
                }

                // Read headers with size and count limits.
                var headerCount = 0
                var totalHeaderBytes = 0

                while (true) {

                    val header = readBoundedLine(
                        input,
                        8192
                    ) ?: run {
                        sendError(output, 400, "Bad Request")
                        return
                    }

                    totalHeaderBytes += header.length

                    if (totalHeaderBytes > 16384) {
                        sendError(
                            output,
                            431,
                            "Request Header Too Large"
                        )
                        return
                    }

                    if (header.isEmpty()) {
                        break
                    }

                    headerCount++

                    if (headerCount > 32) {
                        sendError(
                            output,
                            431,
                            "Request Header Too Large"
                        )
                        return
                    }

                    if (!header.contains(":")) {
                        sendError(output, 400, "Bad Request")
                        return
                    }
                }

                val rawPath = try {
                    URI(requestTarget).rawPath
                        ?: throw IllegalArgumentException()
                } catch (exception: Exception) {
                    sendError(output, 400, "Bad Request")
                    return
                }

                val path = try {
                    URLDecoder.decode(
                        rawPath.replace("+", "%2B"),
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

                val requestedPath = path
                    .removePrefix("/")
                    .ifEmpty { "index.html" }

                val requestedFile = try {
                    File(root, requestedPath).canonicalFile
                } catch (exception: Exception) {
                    sendError(output, 403, "Forbidden")
                    return
                }

                if (!isInsideRoot(requestedFile, root)) {
                    sendError(output, 403, "Forbidden")
                    return
                }

                val target = try {
                    if (requestedFile.isDirectory) {
                        File(
                            requestedFile,
                            "index.html"
                        ).canonicalFile
                    } else {
                        requestedFile
                    }
                } catch (exception: Exception) {
                    sendError(output, 403, "Forbidden")
                    return
                }

                if (
                    !isInsideRoot(target, root) ||
                    !target.isFile ||
                    !target.canRead()
                ) {
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
                    sendError(
                        output,
                        500,
                        "Internal Server Error",
                        method == "HEAD"
                    )
                }
            }
        }
    }

    private fun readBoundedLine(
        input: BufferedInputStream,
        maxLength: Int
    ): String? {

        val bytes = ByteArrayOutputStream()

        while (true) {

            val value = input.read()

            if (value == -1) {
                if (bytes.size() == 0) {
                    return null
                }
                break
            }

            if (value == '\n'.code) {
                break
            }

            if (value != '\r'.code) {

                if (bytes.size() >= maxLength) {
                    throw IOException("Request line too large")
                }

                bytes.write(value)
            }
        }

        return bytes.toString(
            StandardCharsets.ISO_8859_1.name()
        )
    }

    private fun isInsideRoot(
        file: File,
        root: File
    ): Boolean {

        return file == root ||
            file.path.startsWith(root.path + File.separator)
    }

    private fun sendError(
        output: BufferedOutputStream,
        status: Int,
        message: String,
        headOnly: Boolean = false
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

        if (!headOnly) {
            output.write(body)
        }

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

    @Synchronized
    fun stop() {

        running.set(false)

        runCatching {
            serverSocket?.close()
        }

        runCatching {
            executor?.shutdownNow()
        }

        serverSocket = null
        executor = null
    }

    companion object {

        /**
         * Finds a private IPv4 address from the device's
         * active network interfaces.
         */
        fun getLocalNetworkAddress(): String? {

            return try {

                val interfaces = Collections.list(
                    NetworkInterface.getNetworkInterfaces()
                )

                interfaces
                    .filter { networkInterface ->
                        networkInterface.isUp &&
                            !networkInterface.isLoopback
                    }
                    .flatMap { networkInterface ->
                        Collections.list(
                            networkInterface.inetAddresses
                        )
                    }
                    .filterIsInstance<Inet4Address>()
                    .firstOrNull { address ->
                        !address.isLoopbackAddress &&
                            address.isSiteLocalAddress
                    }
                    ?.hostAddress

            } catch (exception: Exception) {
                null
            }
        }
    }
}
