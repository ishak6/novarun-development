package com.mangalocalrunnerplus

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URL
import java.util.UUID
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NodeRuntimeMemoryLimitTest {
    @Test
    fun embeddedNodeReportsConfiguredV8HeapLimit() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val project = File(context.filesDir, "novarun-v8-heap-${UUID.randomUUID()}").apply {
            check(mkdirs())
        }
        try {
            File(project, "server.js").writeText(
                """
                const http = require('node:http');
                const v8 = require('node:v8');
                http.createServer((_request, response) => {
                    response.writeHead(200, { 'content-type': 'application/json' });
                    response.end(JSON.stringify({
                        heapLimitBytes: v8.getHeapStatistics().heap_size_limit
                    }));
                }).listen(Number(process.env.PORT), '127.0.0.1');
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
            assertEquals("Node backend must start: ${state.message}", "RUNNING", state.status)

            val connection = URL("http://127.0.0.1:$port/").openConnection() as HttpURLConnection
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            try {
                assertEquals(200, connection.responseCode)
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val heapLimit = JSONObject(body).getLong("heapLimitBytes")
                val mib = 1024L * 1024L
                assertTrue(
                    "V8 heap limit should reflect the configured cap (192–384 MiB), got ${heapLimit / mib} MiB",
                    heapLimit in (192L * mib)..(384L * mib)
                )
            } finally {
                connection.disconnect()
            }
        } finally {
            NodeProjectService.stop(context)
            val stopDeadline = System.currentTimeMillis() + 10_000L
            while (System.currentTimeMillis() < stopDeadline &&
                NodeStateStore.read(context).status != "STOPPED"
            ) {
                Thread.sleep(100)
            }
            project.deleteRecursively()
        }
    }
}
