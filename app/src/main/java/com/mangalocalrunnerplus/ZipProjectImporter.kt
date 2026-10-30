
package com.mangalocalrunnerplus

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.zip.ZipInputStream

object ZipProjectImporter {

    private const val MAX_ENTRIES = 2000
    private const val MAX_TOTAL_BYTES = 200L * 1024L * 1024L
    private const val BUFFER_SIZE = 8192

    fun importZip(context: Context, uri: Uri): File {
        val projectsDir = File(context.filesDir, "projects")

        if (!projectsDir.exists() && !projectsDir.mkdirs()) {
            throw IOException("Could not create projects directory.")
        }

        val projectName = getZipName(context, uri)
            .substringBeforeLast(".")
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trim('_', '.', '-')
            .take(50)
            .ifBlank { "Imported_Project" }

        val tempDir = File(projectsDir, ".import_${UUID.randomUUID()}")
        val finalDir = File(projectsDir, "${projectName}_${System.currentTimeMillis()}")

        if (!tempDir.mkdirs()) {
            throw IOException("Could not create temporary project directory.")
        }

        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Could not open selected ZIP file.")

            input.use { stream ->
                ZipInputStream(stream).use { zip ->
                    val buffer = ByteArray(BUFFER_SIZE)
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

                        if (entry.isDirectory) {
                            if (!outputFile.exists() && !outputFile.mkdirs()) {
                                throw IOException("Could not create ZIP directory.")
                            }
                        } else {
                            val parent = outputFile.parentFile
                                ?: throw IOException("Invalid ZIP file path.")

                            if (!parent.exists() && !parent.mkdirs()) {
                                throw IOException("Could not create parent directory.")
                            }

                            outputFile.outputStream().use { output ->
                                while (true) {
                                    val count = zip.read(buffer)
                                    if (count == -1) break

                                    totalBytes += count

                                    if (totalBytes > MAX_TOTAL_BYTES) {
                                        throw IOException("ZIP extraction size limit exceeded.")
                                    }

                                    output.write(buffer, 0, count)
                                }
                            }
                        }

                        zip.closeEntry()
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

    private fun validateEntryName(name: String): String {
        val normalized = name.replace('\\', '/')

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
