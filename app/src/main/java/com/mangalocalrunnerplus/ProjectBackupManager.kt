package com.mangalocalrunnerplus

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Creates a portable ZIP backup without modifying the source project.
 * Archives use the same hard limits as ZIP import.
 */
object ProjectBackupManager {
    private const val BUFFER_SIZE = 8192

    data class Limits(
        val maxEntries: Int = ZipProjectImporter.HARD_MAX_ENTRIES,
        val maxTotalBytes: Long = ZipProjectImporter.HARD_MAX_TOTAL_BYTES,
        val maxSingleFileBytes: Long = ZipProjectImporter.HARD_MAX_SINGLE_FILE_BYTES
    ) {
        init {
            require(maxEntries in 1..ZipProjectImporter.HARD_MAX_ENTRIES)
            require(maxTotalBytes in 1..ZipProjectImporter.HARD_MAX_TOTAL_BYTES)
            require(maxSingleFileBytes in 1..ZipProjectImporter.HARD_MAX_SINGLE_FILE_BYTES)
        }
    }

    @Throws(IOException::class)
    fun createBackup(
        projectDirectory: File,
        cacheDirectory: File,
        limits: Limits = Limits()
    ): File {
        val project = try {
            projectDirectory.canonicalFile
        } catch (error: Exception) {
            throw IOException("Project directory cannot be resolved.", error)
        }

        if (!project.isDirectory || Files.isSymbolicLink(project.toPath())) {
            throw IOException("Select a valid project directory to back up.")
        }

        if (!cacheDirectory.exists() && !cacheDirectory.mkdirs()) {
            throw IOException("Could not create NovaRun backup cache.")
        }
        if (!cacheDirectory.isDirectory || !cacheDirectory.canWrite()) {
            throw IOException("NovaRun backup cache is not writable.")
        }

        val backup = File.createTempFile("novarun_backup_", ".zip", cacheDirectory)
        try {
            ZipOutputStream(FileOutputStream(backup)).use { zip ->
                val buffer = ByteArray(BUFFER_SIZE)
                var entries = 0
                var totalBytes = 0L
                val rootPrefix = project.path + File.separator

                project.walkTopDown().forEach { item ->
                    if (item == project) return@forEach

                    if (Files.isSymbolicLink(item.toPath())) {
                        throw IOException("Backup stopped: symbolic links are not supported (" + item.name + ").")
                    }

                    val canonical = try {
                        item.canonicalFile
                    } catch (error: Exception) {
                        throw IOException("Could not resolve project item: " + item.name, error)
                    }
                    if (!canonical.path.startsWith(rootPrefix)) {
                        throw IOException("Backup stopped: project item escaped its root.")
                    }

                    val relative = project.toPath().relativize(item.toPath()).toString()
                        .replace(File.separatorChar, '/')
                    if (relative.isBlank() || relative.startsWith("/") ||
                        relative.split('/').any { it == ".." }
                    ) {
                        throw IOException("Backup stopped: unsafe project path.")
                    }

                    entries++
                    if (entries > limits.maxEntries) {
                        throw IOException("Backup exceeds the " + limits.maxEntries + "-entry limit.")
                    }

                    if (item.isDirectory) {
                        zip.putNextEntry(ZipEntry("$relative/"))
                        zip.closeEntry()
                    } else if (item.isFile) {
                        if (item.length() > limits.maxSingleFileBytes) {
                            throw IOException("Backup file exceeds the per-file size limit: " + relative)
                        }
                        zip.putNextEntry(ZipEntry(relative))
                        var fileBytes = 0L
                        BufferedInputStream(FileInputStream(item)).use { input ->
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                fileBytes += count
                                totalBytes += count
                                if (fileBytes > limits.maxSingleFileBytes) {
                                    throw IOException("Backup file grew beyond the per-file size limit: " + relative)
                                }
                                if (totalBytes > limits.maxTotalBytes) {
                                    throw IOException("Backup exceeds the total extracted size limit.")
                                }
                                zip.write(buffer, 0, count)
                            }
                        }
                        zip.closeEntry()
                    } else {
                        throw IOException("Backup stopped: unsupported project item: " + relative)
                    }
                }
            }
            return backup
        } catch (error: Exception) {
            backup.delete()
            if (error is IOException) throw error
            throw IOException("Could not create project backup.", error)
        }
    }
}
