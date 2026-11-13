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
        val logFile: File?
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
            logFile = state.logPath?.let(::File)
            assertTrue("Node log file must be present", logFile?.isFile == true)
            assertTrue(
                "Captured stdout/stderr must stay within ${NodeLogLimits.MAX_BYTES} bytes",
                logFile!!.length() <= NodeLogLimits.MAX_BYTES
            )
            assertTrue(
                "Truncation must be visible in the bounded log",
                logFile.readText().contains(NodeLogLimits.TRUNCATION_MARKER)
            )
        } finally {
            if (NodeStateStore.read(context).status in setOf("STARTING", "RUNNING")) {
                NodeProjectService.stop(context)
            }
            project.deleteRecursively()
        }
        logFile?.delete()
    }
}
