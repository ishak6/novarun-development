
package com.mangalocalrunnerplus

import java.io.File

data class PreviewValidationResult(
    val isValid: Boolean,
    val message: String,
    val projectRoot: File? = null,
    val entryFile: File? = null
)

object ProjectPreviewValidator {

    fun validate(
        projectDirectory: File
    ): PreviewValidationResult {

        return try {

            // Check whether project directory exists.
            if (!projectDirectory.exists()) {
                return PreviewValidationResult(
                    isValid = false,
                    message = "Project directory does not exist."
                )
            }

            // Check whether the selected path is a directory.
            if (!projectDirectory.isDirectory) {
                return PreviewValidationResult(
                    isValid = false,
                    message = "Selected project path is not a directory."
                )
            }

            // Resolve canonical project root.
            val root = projectDirectory.canonicalFile

            if (!root.canRead()) {
                return PreviewValidationResult(
                    isValid = false,
                    message = "Project directory is not readable."
                )
            }

            // Locate the main HTML entry file.
            val indexFile = File(
                root,
                "index.html"
            ).canonicalFile

            // Prevent paths outside the project directory.
            val isInsideProject =
                indexFile.path.startsWith(
                    root.path + File.separator
                )

            if (!isInsideProject) {
                return PreviewValidationResult(
                    isValid = false,
                    message = "Entry file is outside the project directory."
                )
            }

            // Check whether index.html exists.
            if (!indexFile.exists()) {
                return PreviewValidationResult(
                    isValid = false,
                    message = "index.html was not found."
                )
            }

            // Check whether the entry is a regular file.
            if (!indexFile.isFile) {
                return PreviewValidationResult(
                    isValid = false,
                    message = "index.html is not a regular file."
                )
            }

            // Check file readability.
            if (!indexFile.canRead()) {
                return PreviewValidationResult(
                    isValid = false,
                    message = "index.html cannot be read."
                )
            }

            PreviewValidationResult(
                isValid = true,
                message = "Project is ready for web preview.",
                projectRoot = root,
                entryFile = indexFile
            )

        } catch (exception: SecurityException) {

            PreviewValidationResult(
                isValid = false,
                message = "Permission denied while checking project files."
            )

        } catch (exception: Exception) {

            PreviewValidationResult(
                isValid = false,
                message = exception.message
                    ?: "Unknown project validation error."
            )
        }
    }
}
