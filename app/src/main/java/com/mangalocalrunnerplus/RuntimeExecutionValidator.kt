package com.mangalocalrunnerplus

import java.io.File

data class RuntimeValidationResult(
    val valid: Boolean,
    val message: String,
    val entryFile: File? = null
)

object RuntimeExecutionValidator {

    private const val MAX_ENTRY_PATH_LENGTH = 1024

    fun validate(
        projectDirectory: File,
        entryFile: String
    ): RuntimeValidationResult {
        if (!projectDirectory.exists() || !projectDirectory.isDirectory) {
            return RuntimeValidationResult(false, "Project directory does not exist.")
        }
        if (entryFile.isBlank()) {
            return RuntimeValidationResult(false, "Entry file cannot be empty.")
        }
        if (entryFile.length > MAX_ENTRY_PATH_LENGTH || entryFile.any { it.code == 0 }) {
            return RuntimeValidationResult(false, "Entry file path is invalid or too long.")
        }

        return try {
            val root = projectDirectory.canonicalFile
            if (!root.isDirectory || !root.canRead()) {
                return RuntimeValidationResult(false, "Project directory is not readable.")
            }

            // Runtime requests must use project-relative paths. Absolute paths
            // are rejected explicitly instead of relying on File(parent, child)
            // platform-specific resolution behavior.
            val supplied = File(entryFile)
            if (supplied.isAbsolute || entryFile.startsWith("/") ||
                entryFile.startsWith("\\\\") ||
                Regex("^[A-Za-z]:").containsMatchIn(entryFile)
            ) {
                return RuntimeValidationResult(false, "Entry file path must be relative to the selected project.")
            }

            val candidate = File(root, entryFile).canonicalFile
            val rootPrefix = root.path + File.separator
            if (candidate.path != root.path && !candidate.path.startsWith(rootPrefix)) {
                return RuntimeValidationResult(false, "Entry file is outside the project directory.")
            }
            if (!candidate.exists() || !candidate.isFile) {
                return RuntimeValidationResult(false, "Entry file does not exist or is not a regular file.")
            }
            if (!candidate.canRead()) {
                return RuntimeValidationResult(false, "Entry file is not readable.")
            }

            RuntimeValidationResult(true, "Entry file validation successful.", candidate)
        } catch (_: SecurityException) {
            RuntimeValidationResult(false, "Permission denied while validating the entry file.")
        } catch (_: Exception) {
            RuntimeValidationResult(false, "Could not safely validate the entry file.")
        }
    }
}
