package com.mangalocalrunnerplus

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClothingStoreZipRuntimeTest {
    @Test
    fun localWebServerServiceStartsLocalhostAndStopsCleanly() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val activity = ActivityScenario.launch(MainActivity::class.java)
        val assetName = "clothing_store_nokshi_v1.5.3_professional.zip"
        val projectRoot = instrumentation.context.assets.open(assetName).use { input ->
            ZipProjectImporter.importZip(context, input, assetName, ZipImportLimits())
        }
        val lanPort = ServerSocket(0).use { it.localPort }
        var startedUrl: String? = null

        try {
            val preview = ProjectPreviewValidator.validate(projectRoot)
            assertTrue("ZIP must pass preview validation: ${preview.message}", preview.isValid)

            // Exercise the actual Android foreground service used by the Start button,
            // not a directly constructed LocalWebServer instance.
            LocalWebServerService.start(
                context = context,
                projectDirectory = projectRoot,
                durationMillis = 60_000L,
                port = lanPort,
                authenticationEnabled = true
            )

            val startDeadline = System.currentTimeMillis() + 30_000L
            var state = LocalServerServiceStateStore.state.value
            while (System.currentTimeMillis() < startDeadline &&
                state.status !in setOf(LocalServerServiceStatus.RUNNING, LocalServerServiceStatus.ERROR)
            ) {
                Thread.sleep(200)
                state = LocalServerServiceStateStore.state.value
            }

            assertEquals("Local server service failed to start: ${state.message}",
                LocalServerServiceStatus.RUNNING, state.status)
            val localUrl = state.localUrl
                ?: throw AssertionError("Service reported RUNNING without a localhost URL")
            startedUrl = localUrl
            val response = get(localUrl)
            assertEquals("Service localhost URL must return HTTP 200", 200, response.first)
            assertTrue("Service must serve the imported storefront HTML",
                response.second.contains("<html", ignoreCase = true))

            LocalWebServerService.stop(context)
            val stopDeadline = System.currentTimeMillis() + 15_000L
            state = LocalServerServiceStateStore.state.value
            while (System.currentTimeMillis() < stopDeadline &&
                state.status != LocalServerServiceStatus.STOPPED
            ) {
                Thread.sleep(200)
                state = LocalServerServiceStateStore.state.value
            }
            assertEquals("Service did not transition to STOPPED: ${state.message}",
                LocalServerServiceStatus.STOPPED, state.status)

            val stoppedConnection = runCatching {
                val connection = URL(localUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 1_000
                connection.readTimeout = 1_000
                try { connection.responseCode } finally { connection.disconnect() }
            }
            assertTrue("Localhost socket must reject requests after Stop", stoppedConnection.isFailure)
        } finally {
            LocalWebServerService.stop(context)
            activity.close()
            projectRoot.deleteRecursively()
        }
    }

    @Test
    fun invalidLanPortDoesNotPreventLocalhostStartup() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val activity = ActivityScenario.launch(MainActivity::class.java)
        val assetName = "clothing_store_nokshi_v1.5.3_professional.zip"
        val projectRoot = instrumentation.context.assets.open(assetName).use { input ->
            ZipProjectImporter.importZip(context, input, assetName, ZipImportLimits())
        }

        try {
            LocalWebServerService.start(
                context = context,
                projectDirectory = projectRoot,
                durationMillis = 60_000L,
                port = 80,
                authenticationEnabled = false
            )

            val deadline = System.currentTimeMillis() + 30_000L
            var state = LocalServerServiceStateStore.state.value
            while (System.currentTimeMillis() < deadline &&
                state.status !in setOf(LocalServerServiceStatus.RUNNING, LocalServerServiceStatus.ERROR)
            ) {
                Thread.sleep(150)
                state = LocalServerServiceStateStore.state.value
            }

            assertEquals(
                "Invalid optional LAN port must not fail localhost startup: ${state.message}",
                LocalServerServiceStatus.RUNNING,
                state.status
            )
            assertTrue("Localhost URL must still be available", !state.localUrl.isNullOrBlank())
            assertEquals("LAN URL must be absent when its port is invalid", null, state.lanUrl)
            val response = get(state.localUrl!!)
            assertEquals("Localhost must serve the project despite invalid LAN port", 200, response.first)
        } finally {
            LocalWebServerService.stop(context)
            val stopDeadline = System.currentTimeMillis() + 10_000L
            while (System.currentTimeMillis() < stopDeadline &&
                LocalServerServiceStateStore.state.value.status != LocalServerServiceStatus.STOPPED
            ) {
                Thread.sleep(100)
            }
            activity.close()
            projectRoot.deleteRecursively()
        }
    }

    @Test
    fun rapidStartStopDoesNotLeaveLocalhostListenerRunning() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val activity = ActivityScenario.launch(MainActivity::class.java)
        val assetName = "clothing_store_nokshi_v1.5.3_professional.zip"
        val projectRoot = instrumentation.context.assets.open(assetName).use { input ->
            ZipProjectImporter.importZip(context, input, assetName, ZipImportLimits())
        }
        val observedUrls = linkedSetOf<String>()

        try {
            repeat(5) {
                LocalWebServerService.start(
                    context = context,
                    projectDirectory = projectRoot,
                    durationMillis = 60_000L,
                    port = 0 + 19000 + it,
                    authenticationEnabled = false
                )
                // Immediately issue Stop to exercise cancellation during startup.
                LocalWebServerService.stop(context)

                val deadline = System.currentTimeMillis() + 15_000L
                var state = LocalServerServiceStateStore.state.value
                while (System.currentTimeMillis() < deadline &&
                    state.status != LocalServerServiceStatus.STOPPED
                ) {
                    state.localUrl?.let(observedUrls::add)
                    Thread.sleep(100)
                    state = LocalServerServiceStateStore.state.value
                }
                state.localUrl?.let(observedUrls::add)
                assertEquals(
                    "Service must settle in STOPPED after rapid Start/Stop: ${state.message}",
                    LocalServerServiceStatus.STOPPED,
                    state.status
                )

                // Allow any already-queued Start intent to arrive after Stop.
                // The final state must remain stopped, not merely be transiently stopped.
                Thread.sleep(1_000L)
                state = LocalServerServiceStateStore.state.value
                state.localUrl?.let(observedUrls::add)
                assertEquals(
                    "A delayed Start intent restarted the service after Stop: ${state.message}",
                    LocalServerServiceStatus.STOPPED,
                    state.status
                )
            }

            observedUrls.forEach { url ->
                val result = runCatching {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.connectTimeout = 1_000
                    connection.readTimeout = 1_000
                    try { connection.responseCode } finally { connection.disconnect() }
                }
                assertTrue("Listener must be closed after rapid Stop: $url", result.isFailure)
            }
        } finally {
            LocalWebServerService.stop(context)
            activity.close()
            projectRoot.deleteRecursively()
        }
    }

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
        val projectRoot = instrumentation.context.assets.open(assetName).use { input ->
            ZipProjectImporter.importZip(context, input, assetName, ZipImportLimits())
        }
        val port = ServerSocket(0).use { it.localPort }
        var started = false
        try {
            // Exercise the same safe importer used by the document picker, then
            // validate and serve the imported static website through NovaRun's
            // loopback preview server before starting its optional Node backend.
            val preview = ProjectPreviewValidator.validate(projectRoot)
            assertTrue(
                "Imported ZIP must expose a safe HTML entry point for $assetName: ${preview.message}",
                preview.isValid
            )
            val previewRoot = preview.projectRoot
                ?: throw AssertionError("Preview root missing for $assetName")
            val previewEntry = preview.entryFile
                ?: throw AssertionError("Preview entry point missing for $assetName")
            val previewPort = ServerSocket(0).use { it.localPort }
            val previewServer = LocalWebServer(
                projectDirectory = previewRoot,
                port = previewPort,
                bindToLan = false,
                defaultDocument = previewEntry.name
            )
            assertTrue("Loopback preview server must start for $assetName", previewServer.start())
            try {
                val staticStorefront = get("http://127.0.0.1:$previewPort/")
                assertEquals("Imported static storefront must return HTTP 200 for $assetName", 200, staticStorefront.first)
                assertTrue("Imported static storefront must contain HTML", staticStorefront.second.contains("<html", ignoreCase = true))

                val staticStylesheet = get("http://127.0.0.1:$previewPort/css/styles.css")
                assertEquals("Imported static stylesheet must return HTTP 200 for $assetName", 200, staticStylesheet.first)
                assertTrue("Imported static stylesheet must not be empty", staticStylesheet.second.isNotBlank())
            } finally {
                previewServer.stop()
            }

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
            val healthJson = org.json.JSONObject(health.second)
            assertTrue("Health response must report ok=true", healthJson.optBoolean("ok"))
            assertEquals("clothing-store", healthJson.optString("service"))

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
}
