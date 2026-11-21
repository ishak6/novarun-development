package com.mangalocalrunnerplus

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectBackupRuntimeTest {
    @Test
    fun exportedBackupRestoresThroughProductionZipImporter() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val project = File(context.cacheDir, "backup-roundtrip-source").apply {
            deleteRecursively()
            mkdirs()
        }
        val cache = File(context.cacheDir, "backup-roundtrip-cache").apply {
            deleteRecursively()
            mkdirs()
        }

        try {
            File(project, "index.html").writeText("<html><body>backup round trip</body></html>")
            File(project, "assets").mkdirs()
            File(project, "assets/.gitkeep").writeText("")
            File(project, "empty").mkdirs()

            val archive = ProjectBackupManager.createBackup(project, cache)
            val restored = archive.inputStream().use { input ->
                ZipProjectImporter.importZip(context, input, "restored_backup.zip", ZipImportLimits())
            }

            assertEquals(
                "<html><body>backup round trip</body></html>",
                File(restored, "index.html").readText()
            )
            assertTrue(File(restored, "assets/.gitkeep").exists())
            assertTrue(File(restored, "empty").isDirectory)
            assertTrue("Restore must not modify the original project", File(project, "index.html").exists())
            restored.deleteRecursively()
            archive.delete()
        } finally {
            project.deleteRecursively()
            cache.deleteRecursively()
        }
    }
}
