package com.mangalocalrunnerplus

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NodeRuntimeLogQuotaTest {
    @Test
    fun rapidNodeStartStopLeavesBackendStopped() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val project = File(context.filesDir, "novarun-node-race-${UUID.randomUUID()}").apply {
            check(mkdirs())
        }
        val port = ServerSocket(0).use { it.localPort }
        try {
            File(project, "server.js").writeText(
                """
                const http = require('node:http');
                http.createServer((_request, response) => response.end('race-ok'))
                    .listen(Number(process.env.PORT), '127.0.0.1');
                """.trimIndent()
            )

            NodeProjectService.start(context, project, port)
            NodeProjectService.stop(context)

            val deadline = System.currentTimeMillis() + 15_000L
            var state = NodeStateStore.read(context)
            while (System.currentTimeMillis() < deadline && state.status != "STOPPED") {
                Thread.sleep(100)
                state = NodeStateStore.read(context)
            }
            assertEquals("Node backend must stop after immediate Stop: ${state.message}", "STOPPED", state.status)

            // A delayed Start command must not resurrect the backend after Stop.
            Thread.sleep(1_000L)
            state = NodeStateStore.read(context)
            assertEquals("A queued Node Start restarted the backend: ${state.message}", "STOPPED", state.status)

            val connection = runCatching {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress("127.0.0.1", port), 500)
                }
            }
            assertTrue("Node server socket must remain closed after Stop", connection.isFailure)
        } finally {
            if (NodeStateStore.read(context).status in setOf("STARTING", "RUNNING", "ERROR")) {
                NodeProjectService.stop(context)
                val stopDeadline = System.currentTimeMillis() + 5_000L
                while (System.currentTimeMillis() < stopDeadline && NodeStateStore.read(context).status != "STOPPED") {
                    Thread.sleep(100)
                }
            }
            project.deleteRecursively()
        }
    }

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

    @Test
    fun nodeStartupTimeoutDoesNotRedeliverTheSameFailedStart() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val project = File(context.filesDir, "novarun-node-timeout-${UUID.randomUUID()}").apply {
            check(mkdirs())
        }
        val port = ServerSocket(0).use { it.localPort }
        try {
            // Keep the Node event loop alive but deliberately never bind the port.
            // This exercises the real 30-second startup watchdog and service restart mode.
            File(project, "server.js").writeText("setInterval(() => {}, 1000);")
            NodeProjectService.start(context, project, port)

            val deadline = System.currentTimeMillis() + 45_000L
            var state = NodeStateStore.read(context)
            while (System.currentTimeMillis() < deadline &&
                !(state.status == "ERROR" && state.message.contains("No server accepted connections"))
            ) {
                Thread.sleep(100)
                state = NodeStateStore.read(context)
            }
            assertEquals(
                "The startup watchdog must report its timeout instead of silently restarting",
                true,
                state.status == "ERROR" && state.message.contains("No server accepted connections")
            )

            // A redelivered failed Start would change the persisted state back to STARTING.
            val observationDeadline = System.currentTimeMillis() + 10_000L
            while (System.currentTimeMillis() < observationDeadline) {
                Thread.sleep(250)
                state = NodeStateStore.read(context)
                assertEquals(
                    "A timed-out Node Start was automatically redelivered",
                    "ERROR",
                    state.status
                )
            }

            val connection = runCatching {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress("127.0.0.1", port), 500)
                }
            }
            assertTrue("Timed-out Node project must not leave a listening socket", connection.isFailure)
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
        }
    }


    @Test
    fun nodeServiceCanRestartAfterNaturalExit() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val firstProject = File(context.filesDir, "novarun-node-natural-exit-${UUID.randomUUID()}").apply {
            check(mkdirs())
        }
        val secondProject = File(context.filesDir, "novarun-node-restart-${UUID.randomUUID()}").apply {
            check(mkdirs())
        }
        val firstPort = ServerSocket(0).use { it.localPort }
        val secondPort = ServerSocket(0).use { it.localPort }

        try {
            // The first Node instance opens and closes its listener by itself,
            // causing node::Start() to return without an explicit Stop action.
            File(firstProject, "server.js").writeText(
                """
                const http = require('node:http');
                const server = http.createServer((_request, response) => response.end('first'));
                server.listen(Number(process.env.PORT), '127.0.0.1', () => {
                    server.close();
                });
                """.trimIndent()
            )
            NodeProjectService.start(context, firstProject, firstPort)

            val firstDeadline = System.currentTimeMillis() + 30_000L
            var firstState = NodeStateStore.read(context)
            while (System.currentTimeMillis() < firstDeadline &&
                !(firstState.status == "STOPPED" && firstState.message.contains("exited with code 0"))
            ) {
                Thread.sleep(100)
                firstState = NodeStateStore.read(context)
            }
            assertTrue(
                "First Node instance must exit naturally: ${firstState.message}",
                firstState.status == "STOPPED" && firstState.message.contains("exited with code 0")
            )

            // Give Service.onStartCommand/finally a chance to finish. The second
            // user Start must work even if Android reuses the :nodejs process.
            Thread.sleep(1_000L)
            File(secondProject, "server.js").writeText(
                """
                const http = require('node:http');
                http.createServer((_request, response) => response.end('second'))
                    .listen(Number(process.env.PORT), '127.0.0.1');
                """.trimIndent()
            )
            NodeProjectService.start(context, secondProject, secondPort)

            val secondDeadline = System.currentTimeMillis() + 30_000L
            var secondState = NodeStateStore.read(context)
            while (System.currentTimeMillis() < secondDeadline &&
                secondState.status !in setOf("RUNNING", "ERROR", "STOPPED")
            ) {
                Thread.sleep(100)
                secondState = NodeStateStore.read(context)
            }
            assertEquals(
                "A second Node session must start after a natural exit: ${secondState.message}",
                "RUNNING",
                secondState.status
            )

            val connection = URL("http://127.0.0.1:$secondPort/").openConnection() as HttpURLConnection
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            try {
                assertEquals("Restarted Node service must answer HTTP 200", 200, connection.responseCode)
                assertEquals("second", connection.inputStream.bufferedReader().use { it.readText() })
            } finally {
                connection.disconnect()
            }

            NodeProjectService.stop(context)
            val stopDeadline = System.currentTimeMillis() + 5_000L
            secondState = NodeStateStore.read(context)
            while (System.currentTimeMillis() < stopDeadline && secondState.status != "STOPPED") {
                Thread.sleep(100)
                secondState = NodeStateStore.read(context)
            }
            assertEquals("Second Node session must stop cleanly", "STOPPED", secondState.status)
        } finally {
            if (NodeStateStore.read(context).status in setOf("STARTING", "RUNNING", "ERROR")) {
                NodeProjectService.stop(context)
                val stopDeadline = System.currentTimeMillis() + 5_000L
                while (System.currentTimeMillis() < stopDeadline && NodeStateStore.read(context).status != "STOPPED") {
                    Thread.sleep(100)
                }
            }
            firstProject.deleteRecursively()
            secondProject.deleteRecursively()
        }
    }

