package com.mangalocalrunnerplus

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NodeRuntimeSmokeTest {
    @Test
    fun embeddedNode24ExecutesSqliteServerPrimitives() {
        assertEquals("24.22.1", NodeLaunchPlan.BUNDLED_NODE_VERSION)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val work = File(context.cacheDir, "novarun-node24-smoke").apply {
            deleteRecursively()
            mkdirs()
        }
        val log = File(work, "node-smoke.log")
        val script = """
            const { DatabaseSync } = require('node:sqlite');
            const db = new DatabaseSync(':memory:');
            db.exec('CREATE TABLE smoke (value INTEGER)');
            db.prepare('INSERT INTO smoke(value) VALUES (?)').run(42);
            const row = db.prepare('SELECT value FROM smoke').get();
            if (row.value !== 42) throw new Error('SQLite round-trip failed');
            console.log('NOVARUN_NODE24_SQLITE_OK', process.version);
            db.close();
        """.trimIndent()
        val code = NodeNativeBridge().startNodeWithArguments(
            arrayOf("node", "-e", script),
            work.absolutePath,
            File(work, "node_modules").absolutePath,
            context.cacheDir.absolutePath,
            log.absolutePath,
            "3000"
        )
        val output = log.takeIf(File::isFile)?.readText().orEmpty()
        assertEquals("Node.js exited with a non-zero code. Log: $output", 0, code)
        assertTrue("Expected Node 24 + SQLite smoke marker. Log: $output", output.contains("NOVARUN_NODE24_SQLITE_OK"))
        assertTrue("Expected Node 24 runtime version in log. Log: $output", output.contains("v24.22.1"))
    }
}
