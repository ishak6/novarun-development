package com.mangalocalrunnerplus

import java.io.File

data class PreviewValidationResult(
    val isValid: Boolean,
    val message: String,
    val projectRoot: File? = null,
    val entryFile: File? = null
)

object ProjectPreviewValidator {

    fun validate(projectDirectory: File): PreviewValidationResult {
        return try {
            if (!projectDirectory.exists()) {
                return PreviewValidationResult(false, "Project directory does not exist.")
            }
            if (!projectDirectory.isDirectory) {
                return PreviewValidationResult(false, "Selected project path is not a directory.")
            }

            val root = projectDirectory.canonicalFile
            if (!root.canRead()) {
                return PreviewValidationResult(false, "Project directory is not readable.")
            }

            val entry = ProjectEntryPointFinder.find(root)
                ?: return PreviewValidationResult(
                    false,
                    "No readable index.html was found within the project scan limits. Add a website entry point or build the frontend first."
                )
            val previewRoot = entry.parentFile?.canonicalFile
                ?: return PreviewValidationResult(false, "Website entry point has no parent directory.")
            val rootPrefix = root.path + File.separator
            if (previewRoot != root && !previewRoot.path.startsWith(rootPrefix)) {
                return PreviewValidationResult(false, "Website entry point is outside the project directory.")
            }
            val canonicalEntry = entry.canonicalFile
            if (!canonicalEntry.isFile || !canonicalEntry.canRead() ||
                !canonicalEntry.path.startsWith(previewRoot.path + File.separator)
            ) {
                return PreviewValidationResult(false, "Website entry point is not a safe readable file.")
            }

            val relative = canonicalEntry.relativeTo(root).invariantSeparatorsPath
            PreviewValidationResult(
                isValid = true,
                message = "Website entry point found: $relative",
                projectRoot = previewRoot,
                entryFile = canonicalEntry
            )
        } catch (_: SecurityException) {
            PreviewValidationResult(false, "Permission denied while checking project files.")
        } catch (exception: Exception) {
            PreviewValidationResult(false, exception.message ?: "Unknown project validation error.")
        }
    }
}
