
package com.mangalocalrunnerplus

import java.io.File

data class RuntimeValidationResult(
    val valid: Boolean,
    val message: String,
    val entryFile: File? = null
)

object RuntimeExecutionValidator {

    fun validate(
        projectDirectory: File,
        entryFile: String
    ): RuntimeValidationResult {

        if (!projectDirectory.exists() ||
            !projectDirectory.isDirectory
        ) {
            return RuntimeValidationResult(
                valid = false,
                message = "Project directory does not exist."
            )
        }

        if (entryFile.isBlank()) {
            return RuntimeValidationResult(
                valid = false,
                message = "Entry file cannot be empty."
            )
        }

        val root = projectDirectory.canonicalFile

        val candidate = File(root, entryFile).canonicalFile

        val rootPath = root.path + File.separator

        if (candidate.path != root.path &&
            !candidate.path.startsWith(rootPath)
        ) {
            return RuntimeValidationResult(
                valid = false,
                message = "Entry file is outside the project directory."
            )
        }

        if (!candidate.exists() || !candidate.isFile) {
            return RuntimeValidationResult(
                valid = false,
                message = "Entry file does not exist."
            )
        }

        return RuntimeValidationResult(
            valid = true,
            message = "Entry file validation successful.",
            entryFile = candidate
        )
    }
}
