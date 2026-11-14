package com.mangalocalrunnerplus

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URL
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NodeRuntimeLogQuotaTest {
    @Test
    fun capturedNodeOutputNeverExceedsTwoMiBWhileServerRuns() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val project = File(context.filesDir, "novarun-log-quota-${UUID.randomUUID()}").apply {
            check(mkdirs())
        }
        var logFile: File? = null
        try {
            File(project, "server.js").writeText(
                """
                const http = require('node:http');
                const payload = "x".repeat(8 * 1024 * 1024);
                process.stdout.write(payload, () => {
                    process.stderr.write(payload, () => {
                        http.createServer((_request, response) => response.end("ok"))
                            .listen(Number(process.env.PORT), "127.0.0.1");
                    });
                });
                """.trimIndent()
            )
            val port = ServerSocket(0).use { it.localPort }
            NodeProjectService.start(context, project, port)

            val startDeadline = System.currentTimeMillis() + 30_000L
            var state = NodeStateStore.read(context)
            while (System.currentTimeMillis() < startDeadline &&
                state.status !in setOf("RUNNING", "ERROR", "STOPPED")
            ) {
                Thread.sleep(100)
                state = NodeStateStore.read(context)
            }
            assertEquals(
                "Node server must reach RUNNING: ${state.message}",
                "RUNNING",
                state.status
            )

            val capturedLog = state.logPath?.let(::File)
                ?: throw AssertionError("Node log path must be present")
            logFile = capturedLog

            val markerDeadline = System.currentTimeMillis() + 10_000L
            var logText = ""
            while (System.currentTimeMillis() < markerDeadline) {
                logText = runCatching { capturedLog.readText() }.getOrDefault("")
                if (NodeLogLimits.TRUNCATION_MARKER in logText) break
                Thread.sleep(100)
            }
            assertTrue(
                "Truncation marker must be visible after stdout/stderr exceed the quota",
                NodeLogLimits.TRUNCATION_MARKER in logText
            )
            assertTrue(
                "Captured stdout/stderr must stay within ${NodeLogLimits.MAX_BYTES} bytes",
                capturedLog.length() <= NodeLogLimits.MAX_BYTES
            )
            assertTrue(
                "Service compatibility diagnostics must be present before Node output capture",
                logText.contains("Compatibility notes:")
            )

            val connection = URL("http://127.0.0.1:$port/").openConnection() as HttpURLConnection
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            try {
                assertEquals("Node server must still answer after log truncation", 200, connection.responseCode)
                assertEquals("ok", connection.inputStream.bufferedReader().use { it.readText() })
            } finally {
                connection.disconnect()
            }

            NodeProjectService.stop(context)
            val stopDeadline = System.currentTimeMillis() + 5_000L
            state = NodeStateStore.read(context)
            while (System.currentTimeMillis() < stopDeadline && state.status != "STOPPED") {
                Thread.sleep(100)
                state = NodeStateStore.read(context)
            }
            assertEquals("User Stop action must stop the Node service", "STOPPED", state.status)
            assertTrue(
                "Final log size must remain within the configured cap after Stop",
                capturedLog.length() <= NodeLogLimits.MAX_BYTES
            )
        } finally {
            if (NodeStateStore.read(context).status in setOf("STARTING", "RUNNING", "ERROR")) {
                NodeProjectService.stop(context)
                val stopDeadline = System.currentTimeMillis() + 5_000L
                while (System.currentTimeMillis() < stopDeadline &&
                    NodeStateStore.read(context).status != "STOPPED"
                ) {
                    Thread.sleep(100)
                }
            }
            project.deleteRecursively()
            logFile?.delete()
        }
    }
}
