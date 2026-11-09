package com.mangalocalrunnerplus

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.zip.ZipInputStream

object ZipProjectImporter {

    private const val MAX_ENTRIES = 2000
    private const val MAX_TOTAL_BYTES = 200L * 1024L * 1024L
    private const val MAX_SINGLE_FILE_BYTES = 64L * 1024L * 1024L
    private const val MAX_ENTRY_NAME_LENGTH = 1024
    private const val BUFFER_SIZE = 8192

    fun importZip(context: Context, uri: Uri): File {
        val projectsDir = File(context.filesDir, "projects")

        if (!projectsDir.exists() && !projectsDir.mkdirs()) {
            throw IOException("Could not create projects directory.")
        }

        if (!projectsDir.isDirectory || !projectsDir.canWrite()) {
            throw IOException("Projects directory is not writable.")
        }

        val projectName = getZipName(context, uri)
            .substringBeforeLast(".")
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trim('_', '.', '-')
            .take(50)
            .ifBlank { "Imported_Project" }

        val tempDir = File(projectsDir, ".import_${UUID.randomUUID()}")
        val finalDir = File(projectsDir, "${projectName}_${System.currentTimeMillis()}")

        if (!tempDir.mkdir()) {
            throw IOException("Could not create temporary project directory.")
        }

        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Could not open selected ZIP file.")

            input.use { stream ->
                ZipInputStream(stream).use { zip ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    val seenPaths = HashSet<String>()
                    var entryCount = 0
                    var totalBytes = 0L

                    while (true) {
                        val entry = zip.nextEntry ?: break
                        entryCount++

                        if (entryCount > MAX_ENTRIES) {
                            throw IOException("ZIP contains too many entries.")
                        }

                        val safeName = validateEntryName(entry.name)
                        val outputFile = File(tempDir, safeName)
                        val canonicalRoot = tempDir.canonicalPath + File.separator
                        val canonicalOutput = outputFile.canonicalPath

                        if (!canonicalOutput.startsWith(canonicalRoot)) {
                            throw IOException("Unsafe ZIP entry path.")
                        }

                        // Reject duplicate normalized paths instead of silently overwriting
                        // an earlier file or directory from the same archive.
                        val normalizedPath = canonicalOutput
                        if (!seenPaths.add(normalizedPath)) {
                            throw IOException("ZIP contains duplicate file paths.")
                        }

                        if (entry.isDirectory) {
                            if (outputFile.exists() && !outputFile.isDirectory) {
                                throw IOException("ZIP file and directory paths conflict.")
                            }
                            if (!outputFile.exists() && !outputFile.mkdirs()) {
                                throw IOException("Could not create ZIP directory.")
                            }
                        } else {
                            val parent = outputFile.parentFile
                                ?: throw IOException("Invalid ZIP file path.")

                            if (!parent.exists() && !parent.mkdirs()) {
                                throw IOException("Could not create parent directory.")
                            }
                            if (!parent.isDirectory) {
                                throw IOException("ZIP path parent is not a directory.")
                            }

                            if (!outputFile.createNewFile()) {
                                throw IOException("ZIP contains conflicting file paths.")
                            }

                            var fileBytes = 0L
                            FileOutputStream(outputFile).use { output ->
                                while (true) {
                                    val count = zip.read(buffer)
                                    if (count == -1) break

                                    fileBytes += count
                                    totalBytes += count

                                    if (fileBytes > MAX_SINGLE_FILE_BYTES) {
                                        throw IOException("A ZIP file exceeds the 64 MB per-file limit.")
                                    }
                                    if (totalBytes > MAX_TOTAL_BYTES) {
                                        throw IOException("ZIP extraction size limit exceeded (200 MB).")
                                    }

                                    output.write(buffer, 0, count)
                                }
                            }
                        }

                        zip.closeEntry()
                    }

                    if (entryCount == 0) {
                        throw IOException("The selected ZIP archive is empty.")
                    }
                }
            }

            if (!tempDir.renameTo(finalDir)) {
                throw IOException("Could not finalize imported project.")
            }

            return finalDir

        } catch (error: Exception) {
            tempDir.deleteRecursively()
            throw error
        }
    }

    internal fun validateEntryName(name: String): String {
        if (name.isBlank() || name.length > MAX_ENTRY_NAME_LENGTH || name.any { it.code == 0 }) {
            throw IOException("ZIP contains an invalid file path.")
        }

        val normalized = name.replace(92.toChar(), '/')

        if (
            normalized.startsWith("/") ||
            normalized.contains(":") ||
            normalized.split("/").any { it == ".." }
        ) {
            throw IOException("ZIP contains an unsafe file path.")
        }

        val safeParts = normalized
            .split("/")
            .filter { it.isNotBlank() && it != "." }

        if (safeParts.isEmpty()) {
            throw IOException("ZIP contains an invalid file path.")
        }

        return safeParts.joinToString(File.separator)
    }

    private fun getZipName(context: Context, uri: Uri): String {
        var name = "Imported_Project.zip"

        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    name = cursor.getString(index) ?: name
                }
            }
        }

        return name
    }
}
