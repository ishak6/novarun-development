package com.mangalocalrunnerplus

import java.io.File

data class PreviewValidationResult(
    val isValid: Boolean,
    val message: String,
    val projectRoot: File? = null,
    val entryFile: File? = null
)

object ProjectPreviewValidator {

    private val webOutputDirectories = listOf("dist", "build", "out")

    fun validate(
        projectDirectory: File
    ): PreviewValidationResult {
        return try {
            if (!projectDirectory.exists()) {
                return PreviewValidationResult(
                    isValid = false,
                    message = "Project directory does not exist."
                )
            }

            if (!projectDirectory.isDirectory) {
                return PreviewValidationResult(
                    isValid = false,
                    message = "Selected project path is not a directory."
                )
            }

            val root = projectDirectory.canonicalFile

            if (!root.canRead()) {
                return PreviewValidationResult(
                    isValid = false,
                    message = "Project directory is not readable."
                )
            }

            // Prefer compiled static output for common frontend build tools.
            // Vite/React projects commonly place their deployable site in dist;
            // Create React App commonly uses build; static exports may use out.
            val candidateRoots = webOutputDirectories
                .map { File(root, it) } + root

            for (candidate in candidateRoots) {
                if (!candidate.exists() || !candidate.isDirectory || !candidate.canRead()) {
                    continue
                }

                val previewRoot = candidate.canonicalFile
                val isInsideProject = previewRoot == root ||
                    previewRoot.path.startsWith(root.path + File.separator)

                if (!isInsideProject) {
                    continue
                }

                val indexFile = File(previewRoot, "index.html").canonicalFile
                val isInsidePreviewRoot = indexFile.path.startsWith(
                    previewRoot.path + File.separator
                )

                if (
                    isInsidePreviewRoot &&
                    indexFile.isFile &&
                    indexFile.canRead()
                ) {
                    val outputLabel = if (previewRoot == root) {
                        "project root"
                    } else {
                        previewRoot.name
                    }

                    return PreviewValidationResult(
                        isValid = true,
                        message = "Static web output found in $outputLabel.",
                        projectRoot = previewRoot,
                        entryFile = indexFile
                    )
                }
            }

            PreviewValidationResult(
                isValid = false,
                message = "index.html was not found in the project root or dist/build/out. Build the frontend first, then preview its static output."
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
