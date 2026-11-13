package com.mangalocalrunnerplus

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.net.ServerSocket
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NodeRuntimeLogQuotaTest {
    @Test
    fun capturedNodeOutputNeverExceedsTwoMiB() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val project = File(context.filesDir, "novarun-log-quota-${UUID.randomUUID()}").apply {
            check(mkdirs())
        }
        var logFile: File? = null
        try {
            File(project, "server.js").writeText(
                """
                const payload = "x".repeat(8 * 1024 * 1024);
                process.stdout.write(payload, () => {
                    process.stderr.write(payload, () => process.exit(0));
                });
                """.trimIndent()
            )
            val port = ServerSocket(0).use { it.localPort }
            NodeProjectService.start(context, project, port)

            val deadline = System.currentTimeMillis() + 30_000L
            var state = NodeStateStore.read(context)
            while (System.currentTimeMillis() < deadline &&
                state.status !in setOf("STOPPED", "ERROR")
            ) {
                Thread.sleep(100)
                state = NodeStateStore.read(context)
            }

            assertEquals("Node log fixture must exit normally: ${state.message}", "STOPPED", state.status)
            val capturedLog = state.logPath?.let(::File)
                ?: throw AssertionError("Node log path must be present")
            logFile = capturedLog
            assertTrue("Node log file must exist", capturedLog.isFile)
            assertTrue(
                "Captured stdout/stderr must stay within ${NodeLogLimits.MAX_BYTES} bytes",
                capturedLog.length() <= NodeLogLimits.MAX_BYTES
            )
            assertTrue(
                "Truncation must be visible in the bounded log",
                capturedLog.readText().contains(NodeLogLimits.TRUNCATION_MARKER)
            )
        } finally {
            if (NodeStateStore.read(context).status in setOf("STARTING", "RUNNING")) {
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
