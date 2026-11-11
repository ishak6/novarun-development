package com.mangalocalrunnerplus

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipInputStream

data class ZipImportLimits(
    val maxEntries: Int = 2000,
    val maxTotalBytes: Long = 200L * 1024L * 1024L,
    val maxSingleFileBytes: Long = 64L * 1024L * 1024L
) {
    companion object {
        fun from(context: Context): ZipImportLimits {
            val prefs = context.getSharedPreferences("novarun_settings", Context.MODE_PRIVATE)
            val entries = prefs.getInt("zip_max_entries", 2000).coerceAtLeast(0)
            val totalMb = prefs.getInt("zip_max_total_mb", 200).coerceAtLeast(0)
            val singleMb = prefs.getInt("zip_max_single_mb", 64).coerceAtLeast(0)
            return ZipImportLimits(
                maxEntries = entries,
                maxTotalBytes = totalMb.toLong() * 1024L * 1024L,
                maxSingleFileBytes = singleMb.toLong() * 1024L * 1024L
            )
        }
    }
}

object ZipProjectImporter {

    private const val MAX_ENTRY_NAME_LENGTH = 1024
    private const val BUFFER_SIZE = 8192

    fun importZip(context: Context, uri: Uri, limits: ZipImportLimits = ZipImportLimits.from(context)): File {
        val displayName = getZipName(context, uri)
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Could not open selected ZIP file.")
        return input.use { stream ->
            importZip(context, stream, displayName, limits)
        }
    }

    // The stream entry point is shared by the OpenDocument picker and the
    // instrumentation tests, so tests exercise the production extraction path.
    internal fun importZip(
        context: Context,
        input: InputStream,
        displayName: String,
        limits: ZipImportLimits = ZipImportLimits.from(context)
    ): File {
        val projectsDir = File(context.filesDir, "projects")

        if (!projectsDir.exists() && !projectsDir.mkdirs()) {
            throw IOException("Could not create projects directory.")
        }

        if (!projectsDir.isDirectory || !projectsDir.canWrite()) {
            throw IOException("Projects directory is not writable.")
        }

        val projectName = displayName
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
            ZipInputStream(input).use { zip ->
                val buffer = ByteArray(BUFFER_SIZE)
                val seenPaths = HashSet<String>()
                var entryCount = 0
                var totalBytes = 0L

                while (true) {
                    val entry = zip.nextEntry ?: break
                    entryCount++

                    if (limits.maxEntries > 0 && entryCount > limits.maxEntries) {
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

                                if (limits.maxSingleFileBytes > 0 && fileBytes > limits.maxSingleFileBytes) {
                                    throw IOException("A ZIP file exceeds the configured per-file limit (${limits.maxSingleFileBytes / (1024L * 1024L)} MB).")
                                }
                                if (limits.maxTotalBytes > 0 && totalBytes > limits.maxTotalBytes) {
                                    throw IOException("ZIP extraction exceeds the configured total limit (${limits.maxTotalBytes / (1024L * 1024L)} MB).")
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
