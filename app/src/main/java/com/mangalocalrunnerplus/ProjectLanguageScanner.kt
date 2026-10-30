
package com.mangalocalrunnerplus

import java.io.File

data class LanguageScanResult(
    val languages: List<LanguageInfo>,
    val totalFiles: Int,
    val supportedFiles: Int,
    val unknownFiles: Int,
    val filesByLanguage: Map<String, List<File>>
)

object ProjectLanguageScanner {

    private val ignoredDirectories = setOf(
        ".git",
        ".gradle",
        ".idea",
        "node_modules",
        "build",
        "dist",
        "vendor",
        ".dart_tool",
        "__pycache__"
    )

    fun scan(projectDirectory: File): LanguageScanResult {
        if (!projectDirectory.exists() || !projectDirectory.isDirectory) {
            return emptyResult()
        }

        val filesByLanguage = linkedMapOf<String, MutableList<File>>()
        var totalFiles = 0
        var supportedFiles = 0
        var unknownFiles = 0

        fun visit(directory: File, depth: Int) {
            if (depth > 8 || totalFiles >= 10000) return

            val children = directory.listFiles() ?: return

            for (child in children) {
                if (totalFiles >= 10000) break

                if (child.isDirectory) {
                    if (child.name !in ignoredDirectories) {
                        visit(child, depth + 1)
                    }
                } else if (child.isFile) {
                    totalFiles++

                    val language =
                        ProjectLanguageDetector.detectFile(child)

                    if (language.name == "Unknown") {
                        unknownFiles++
                    } else {
                        supportedFiles++

                        filesByLanguage
                            .getOrPut(language.name) {
                                mutableListOf()
                            }
                            .add(child)
                    }
                }
            }
        }

        visit(projectDirectory, 0)

        val languageList = filesByLanguage.keys.map { name ->
            filesByLanguage[name]
                ?.firstOrNull()
                ?.let {
                    ProjectLanguageDetector.detectFile(it)
                }
        }.filterNotNull()

        return LanguageScanResult(
            languages = languageList,
            totalFiles = totalFiles,
            supportedFiles = supportedFiles,
            unknownFiles = unknownFiles,
            filesByLanguage = filesByLanguage.mapValues {
                it.value.toList()
            }
        )
    }

    private fun emptyResult() = LanguageScanResult(
        languages = emptyList(),
        totalFiles = 0,
        supportedFiles = 0,
        unknownFiles = 0,
        filesByLanguage = emptyMap()
    )
}
