package com.mangalocalrunnerplus

import java.io.File
import java.nio.file.Files
import java.util.zip.ZipInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectBackupManagerTest {
    @Test
    fun backupPreservesNestedFilesHiddenFilesAndEmptyDirectories() {
        val temp = Files.createTempDirectory("novarun-backup-test-").toFile()
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
        val temp = Files.createTempDirectory("novarun-backup-missing-").toFile()
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
    fun backupRejectsSymbolicLinks() {
        val temp = Files.createTempDirectory("novarun-backup-symlink-").toFile()
        try {
            val project = File(temp, "project").apply { mkdirs() }
            val outside = File(temp, "outside.txt").apply { writeText("outside") }
            Files.createSymbolicLink(File(project, "outside-link").toPath(), outside.toPath())
            val error = runCatching {
                ProjectBackupManager.createBackup(project, File(temp, "cache"))
            }.exceptionOrNull()
            assertTrue(error is java.io.IOException)
        } finally {
            temp.deleteRecursively()
        }
    }

    @Test
    fun backupEnforcesConfiguredEntryAndByteLimits() {
        val temp = Files.createTempDirectory("novarun-backup-limits-").toFile()
        try {
            val project = File(temp, "project").apply { mkdirs() }
            File(project, "one.txt").writeText("123")
            File(project, "two.txt").writeText("456")
            val entryError = runCatching {
                ProjectBackupManager.createBackup(
                    project, File(temp, "entry-cache"),
                    ProjectBackupManager.Limits(maxEntries = 1, maxTotalBytes = 100, maxSingleFileBytes = 100)
                )
            }.exceptionOrNull()
            val byteError = runCatching {
                ProjectBackupManager.createBackup(
                    project, File(temp, "byte-cache"),
                    ProjectBackupManager.Limits(maxEntries = 10, maxTotalBytes = 2, maxSingleFileBytes = 100)
                )
            }.exceptionOrNull()
            assertTrue(entryError is java.io.IOException)
            assertTrue(byteError is java.io.IOException)
        } finally {
            temp.deleteRecursively()
        }
    }

    @Test
    fun backupUsesImportCompatibleHardLimits() {
        val limits = ProjectBackupManager.Limits()
        assertEquals(ZipImportLimits.HARD_MAX_ENTRIES, limits.maxEntries)
        assertEquals(ZipImportLimits.HARD_MAX_TOTAL_BYTES, limits.maxTotalBytes)
        assertEquals(ZipImportLimits.HARD_MAX_SINGLE_FILE_BYTES, limits.maxSingleFileBytes)
    }
}
