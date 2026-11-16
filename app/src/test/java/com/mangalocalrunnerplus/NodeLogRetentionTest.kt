package com.mangalocalrunnerplus

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NodeLogRetentionTest {
    @Test
    fun prunesOldestLogsAndPreservesCurrentLogWithinGlobalBudgets() {
        val directory = createTempDir(prefix = "novarun-node-log-retention-")
        try {
            val unrelated = File(directory, "unrelated.log").apply { writeText("keep") }
            val logs = (0 until 12).map { index ->
                File(directory, "node-backend-project-$index-$index.log").apply {
                    writeBytes(ByteArray(2 * 1024 * 1024))
                    setLastModified(1_000_000L + index * 10_000L)
                }
            }
            val current = logs.first()

            assertTrue(NodeLogRetention.prune(directory, current))

            val retained = directory.listFiles { file ->
                file.isFile && file.name.startsWith("node-backend-") && file.name.endsWith(".log")
            }?.toList().orEmpty()
            assertEquals(NodeLogRetention.MAX_FILES, retained.size)
            assertTrue(retained.sumOf { it.length() } <= NodeLogRetention.MAX_TOTAL_BYTES)
            assertTrue(current.exists())
            assertTrue(logs.drop(1).take(2).none { it.exists() })
            assertTrue(logs.drop(3).all { it.exists() })
            assertTrue(unrelated.exists())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun refusesToPruneWhenCurrentLogIsOutsideTheManagedDirectory() {
        val directory = createTempDir(prefix = "novarun-node-log-root-")
        val outside = createTempDir(prefix = "novarun-node-log-outside-")
        try {
            val oldLog = File(directory, "node-backend-old.log").apply { writeText("old") }
            val current = File(outside, "node-backend-current.log").apply { writeText("current") }

            assertFalse(NodeLogRetention.prune(directory, current))
            assertTrue(oldLog.exists())
        } finally {
            directory.deleteRecursively()
            outside.deleteRecursively()
        }
    }
}
