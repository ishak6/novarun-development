package com.mangalocalrunnerplus

import java.io.File
import java.util.zip.ZipInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectBackupManagerTest {
    @Test
    fun backupPreservesNestedFilesHiddenFilesAndEmptyDirectories() {
        val temp = createTempDir(prefix = "novarun-backup-test-")
        try {
            val project = File(temp, "shop").apply { mkdirs() }
            File(project, "index.html").writeText("<html>shop</html>")
            File(project, "data").mkdirs()
            File(project, "data/.gitkeep").writeText("")
            File(project, "empty-folder").mkdirs()
            val cache = File(temp, "cache")

            val backup = ProjectBackupManager.createBackup(project, cache)
            val entries = ZipInputStream(backup.inputStream()).use { zip ->
                buildList {
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        add(entry.name to if (entry.isDirectory) "" else zip.readBytes().toString(Charsets.UTF_8))
                        zip.closeEntry()
                    }
                }
            }

            assertTrue(entries.any { it.first == "index.html" && it.second == "<html>shop</html>" })
            assertTrue(entries.any { it.first == "data/.gitkeep" })
            assertTrue(entries.any { it.first == "empty-folder/" })
            assertTrue(entries.none { it.first.startsWith("shop/") })
            assertTrue(project.exists())
            backup.delete()
        } finally {
            temp.deleteRecursively()
        }
    }

    @Test
    fun backupRejectsMissingProjectDirectory() {
        val temp = createTempDir(prefix = "novarun-backup-missing-")
        try {
            val error = runCatching {
                ProjectBackupManager.createBackup(File(temp, "missing"), File(temp, "cache"))
            }.exceptionOrNull()
            assertTrue(error is java.io.IOException)
        } finally {
            temp.deleteRecursively()
        }
    }

    @Test
    fun backupUsesImportCompatibleHardLimits() {
        val limits = ProjectBackupManager.Limits()
        assertEquals(ZipProjectImporter.HARD_MAX_ENTRIES, limits.maxEntries)
        assertEquals(ZipProjectImporter.HARD_MAX_TOTAL_BYTES, limits.maxTotalBytes)
        assertEquals(ZipProjectImporter.HARD_MAX_SINGLE_FILE_BYTES, limits.maxSingleFileBytes)
    }
}
