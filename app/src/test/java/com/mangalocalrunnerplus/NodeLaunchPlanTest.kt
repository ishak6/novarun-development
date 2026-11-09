package com.mangalocalrunnerplus

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertTrue
import org.junit.Test

class NodeLaunchPlanTest {
    @Test fun findsServerEntry() {
        val root = Files.createTempDirectory("node-plan").toFile()
        try {
            File(root, "server.js").writeText("console.log('server')")
            assertTrue(NodeLaunchPlan.create(root, 3000).isSuccess)
        } finally { root.deleteRecursively() }
    }
    @Test fun rejectsMissingEntry() {
        val root = Files.createTempDirectory("node-plan-missing").toFile()
        try {
            File(root, "index.html").writeText("<html/>")
            assertTrue(NodeLaunchPlan.create(root, 3000).isFailure)
        } finally { root.deleteRecursively() }
    }
}
