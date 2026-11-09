package com.mangalocalrunnerplus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ProjectFileTypeSupportTest {
    @Test
    fun recognizesGitKeepMarkdownAndJavaScriptFiles() {
        val root = Files.createTempDirectory("novarun-file-types").toFile()
        try {
            File(root, ".gitkeep").writeText("")
            File(root, "README.md").writeText("# Project")
            File(root, "app.js").writeText("console.log('ok')")
            val scan = ProjectLanguageScanner.scan(root)
            val detected = scan.languages.map { it.name }.toSet()
            assertTrue(detected.contains("Git Keep"))
            assertTrue(detected.contains("Markdown"))
            assertTrue(detected.contains("JavaScript"))
            assertEquals(3, scan.totalFiles)
        } finally {
            root.deleteRecursively()
        }
    }
}
