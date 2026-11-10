package com.mangalocalrunnerplus

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URL
import java.util.zip.ZipInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClothingStoreZipRuntimeTest {
    @Test
    fun secureFullstackZipStartsAndAnswersHealthCheck() {
        runStoreZip("clothing_store_secure_fullstack.zip")
    }

    @Test
    fun nokshiProfessionalZipStartsAndAnswersHealthCheck() {
        runStoreZip("clothing_store_nokshi_v1.5.3_professional.zip")
    }

    private fun runStoreZip(assetName: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val activity = ActivityScenario.launch(MainActivity::class.java)
        val projectRoot = extractZip(context, assetName)
        val port = ServerSocket(0).use { it.localPort }
        var started = false
        try {
            val launchPlan = NodeLaunchPlan.create(projectRoot, port).getOrElse { cause ->
                throw AssertionError("Could not discover a runnable Node.js project in $assetName: ${cause.message}", cause)
            }
            assertTrue("Detected Node.js project root must exist for $assetName", launchPlan.root.isDirectory)
            assertTrue("Detected Node.js entry point must exist for $assetName", launchPlan.entry.isFile)
            NodeProjectService.start(context, projectRoot, port)

            val deadline = System.currentTimeMillis() + 45_000L
            var state = NodeStateStore.read(context)
            while (System.currentTimeMillis() < deadline && state.status !in setOf("RUNNING", "ERROR")) {
                Thread.sleep(250)
                state = NodeStateStore.read(context)
            }
            assertEquals(
                "Node backend failed to start for $assetName: ${state.message}. Log: ${state.logPath?.let { File(it).takeIf(File::isFile)?.readText()?.takeLast(6000) }}",
                "RUNNING",
                state.status
            )
            started = true

            val health = get("http://127.0.0.1:$port/healthz")
            assertEquals("Health endpoint must return HTTP 200 for $assetName", 200, health.first)
            assertTrue("Health response must identify a healthy clothing-store service", health.second.contains("\\"ok\\":true"))
            assertTrue("Health response must identify the expected service", health.second.contains("\\"service\\":\\"clothing-store\\""))

            // The health route is registered only after the server initializes
            // its built-in node:sqlite database. Verify the persistent file too.
            val database = File(launchPlan.root, "data/store.sqlite")
            assertTrue("SQLite database must be created for $assetName", database.isFile && database.length() > 0L)

            // A listening API is not enough: verify the actual storefront and
            // a referenced CSS asset are served over HTTP from the imported ZIP.
            val storefront = get("http://127.0.0.1:$port/")
            assertEquals("Storefront must return HTTP 200 for $assetName", 200, storefront.first)
            assertTrue("Storefront response must contain HTML", storefront.second.contains("<html", ignoreCase = true))

            val stylesheet = get("http://127.0.0.1:$port/css/styles.css")
            assertEquals("Storefront stylesheet must return HTTP 200 for $assetName", 200, stylesheet.first)
            assertTrue("Storefront stylesheet must not be empty", stylesheet.second.isNotBlank())
        } finally {
            NodeProjectService.stop(context)
            if (started) {
                val stopDeadline = System.currentTimeMillis() + 10_000L
                while (System.currentTimeMillis() < stopDeadline && NodeStateStore.read(context).status != "STOPPED") {
                    Thread.sleep(200)
                }
            }
            activity.close()
            projectRoot.deleteRecursively()
        }
    }

    private fun get(url: String): Pair<Int, String> {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 5_000
        connection.readTimeout = 5_000
        return try {
            val status = connection.responseCode
            val stream = if (status >= 400) connection.errorStream else connection.inputStream
            status to (stream?.bufferedReader()?.use { it.readText() }.orEmpty())
        } finally {
            connection.disconnect()
        }
    }

    private fun extractZip(context: Context, assetName: String): File {
        val root = File(context.cacheDir, "zip-runtime-${assetName.hashCode()}-${System.nanoTime()}")
        check(root.mkdirs())
        var totalBytes = 0L
        var entries = 0
        ZipInputStream(context.assets.open(assetName)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries++
                require(entries <= 2_000) { "ZIP contains too many entries" }
                val output = File(root, entry.name).canonicalFile
                require(output.path.startsWith(root.canonicalPath + File.separator)) {
                    "ZIP path escapes the test extraction directory"
                }
                if (entry.isDirectory) {
                    check(output.mkdirs() || output.isDirectory)
                } else {
                    check(output.parentFile?.mkdirs() == true || output.parentFile?.isDirectory == true)
                    FileOutputStream(output).use { stream ->
                        val buffer = ByteArray(8 * 1024)
                        var fileBytes = 0L
                        while (true) {
                            val count = zip.read(buffer)
                            if (count < 0) break
                            fileBytes += count
                            totalBytes += count
                            require(fileBytes <= 64L * 1024 * 1024) { "ZIP entry exceeds 64 MiB" }
                            require(totalBytes <= 200L * 1024 * 1024) { "ZIP exceeds 200 MiB" }
                            stream.write(buffer, 0, count)
                        }
                    }
                }
                zip.closeEntry()
            }
        }
        return root
    }
}
