
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
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLSocket
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class LocalWebServer(
    private val projectDirectory: File,
    private val port: Int = 8080,
    private val bindToLan: Boolean = false,
    private val accessToken: String? = null,
    private val accessUsername: String = "novarun",
    private val sslContext: SSLContext? = null
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

        // Never expose a project on the LAN over plaintext HTTP.
        // Loopback-only preview may still use HTTP for local browser access.
        if (bindToLan && sslContext == null) {
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

            val socket = (sslContext?.serverSocketFactory?.createServerSocket() ?: ServerSocket()).apply {
                reuseAddress = true
                if (this is SSLServerSocket) {
                    val supported = supportedProtocols.toSet()
                    enabledProtocols = listOf("TLSv1.3", "TLSv1.2")
                        .filter { it in supported }
                        .toTypedArray()
                    if (enabledProtocols.isEmpty()) {
                        throw IOException("No supported TLS protocol is available")
                    }
                    needClientAuth = false
                }
                // A bounded backlog prevents unbounded pending TCP connections.
                bind(
                    InetSocketAddress(
                        bindAddress,
                        port
                    ),
                    64
                )
            }

            // Bound both worker threads and queued clients. If saturated,
            // the accept loop handles one client itself, applying backpressure.
            val pool: ExecutorService = ThreadPoolExecutor(
                4,
                8,
                60L,
                TimeUnit.SECONDS,
                ArrayBlockingQueue<Runnable>(64),
                ThreadPoolExecutor.CallerRunsPolicy()
            )

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
            if (client is SSLSocket) {
                client.startHandshake()
            }

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
                val headers = mutableMapOf<String, String>()
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

                    val separator = header.indexOf(":")
                    if (separator <= 0) {
                        sendError(output, 400, "Bad Request")
                        return
                    }
                    val headerName = header.substring(0, separator).trim().lowercase()
                    val headerValue = header.substring(separator + 1).trim()
                    if (headerName.isNotEmpty()) headers[headerName] = headerValue
                }

                if (bindToLan && accessToken != null && !isAuthorized(headers["authorization"])) {
                    sendUnauthorized(output, method == "HEAD")
                    return
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

                if (isSensitivePath(requestedPath)) {
                    // Never publish VCS metadata, dotfiles, credentials or private keys.
                    sendError(output, 404, "Not Found")
                    return
                }

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

                var target = try {
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

                if (!isInsideRoot(target, root)) {
                    sendError(output, 403, "Forbidden")
                    return
                }

                if (!target.isFile || !target.canRead()) {
                    // Support client-side routes in React/Vite and other SPAs.
                    // Keep missing asset requests as 404s instead of returning HTML.
                    val lastSegment = requestedPath.substringAfterLast('/')
                    val isExtensionlessRoute = !lastSegment.contains('.')
                    val fallbackIndex = try {
                        File(root, "index.html").canonicalFile
                    } catch (exception: Exception) {
                        sendError(output, 403, "Forbidden")
                        return
                    }

                    if (
                        isExtensionlessRoute &&
                        isInsideRoot(fallbackIndex, root) &&
                        fallbackIndex.isFile &&
                        fallbackIndex.canRead()
                    ) {
                        target = fallbackIndex
                    } else {
                        sendError(output, 404, "Not Found")
                        return
                    }
                }

                val mimeType = getMimeType(target.extension)
                val fileLength = target.length()

                output.write(
                    (
                        "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: $mimeType\r\n" +
                        "Content-Length: $fileLength\r\n" +
                        "Connection: close\r\n" +
                        "Cache-Control: no-store\r\n" +
                        "Referrer-Policy: no-referrer\r\n" +
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

    private fun isAuthorized(authorization: String?): Boolean {
        val expectedToken = accessToken ?: return false
        if (authorization.isNullOrBlank() || !authorization.startsWith("Basic ", ignoreCase = true)) return false
        return try {
            val decoded = String(java.util.Base64.getDecoder().decode(authorization.substringAfter(" ").trim()), StandardCharsets.UTF_8)
            val separator = decoded.indexOf(58.toChar())
            if (separator <= 0 || decoded.substring(0, separator) != accessUsername) false
            else java.security.MessageDigest.isEqual(decoded.substring(separator + 1).toByteArray(StandardCharsets.UTF_8), expectedToken.toByteArray(StandardCharsets.UTF_8))
        } catch (_: Exception) { false }
    }

    private fun sendUnauthorized(output: BufferedOutputStream, headOnly: Boolean) {
        val body = "Authentication required".toByteArray(StandardCharsets.UTF_8)
        val response = "HTTP/1.1 401 Unauthorized\r\n" +
            "WWW-Authenticate: Basic realm=" + 34.toChar() + "NovaRun" + 34.toChar() + "\r\n" +
            "Content-Type: text/plain; charset=utf-8\r\n" +
            "Content-Length: ${body.size}\r\n" +
            "Cache-Control: no-store\r\n" +
            "Connection: close\r\n\r\n"
        output.write(response.toByteArray(StandardCharsets.UTF_8))
        if (!headOnly) output.write(body)
        output.flush()
    }
    private fun isSensitivePath(path: String): Boolean {
        val segments = path.split('/').filter { it.isNotBlank() }
        if (segments.any { it.startsWith(".") && it != ".well-known" }) {
            return true
        }

        val blockedNames = setOf(
            "id_rsa",
            "id_ed25519",
            "credentials.json",
            "service-account.json",
            "secrets.json",
            "secrets.yaml",
            "secrets.yml"
        )

        val blockedExtensions = setOf(
            "pem",
            "key",
            "p12",
            "pfx",
            "jks",
            "keystore",
            "mobileprovision",
            "db",
            "sqlite",
            "sqlite3",
            "sqlite-wal",
            "sqlite-shm"
        )

        return segments.any { segment ->
            val name = segment.lowercase()
            val extension = name.substringAfterLast('.', "")
            name in blockedNames || extension in blockedExtensions
        }
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
            "woff" -> "font/woff"
            "woff2" -> "font/woff2"
            "ttf" -> "font/ttf"
            "otf" -> "font/otf"
            "eot" -> "application/vnd.ms-fontobject"
            "avif" -> "image/avif"
            "webmanifest" -> "application/manifest+json"
            "map" -> "application/json; charset=utf-8"
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
