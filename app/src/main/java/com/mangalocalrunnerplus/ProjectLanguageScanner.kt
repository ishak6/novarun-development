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
        ".git", ".gradle", ".idea", "node_modules", "build", "dist",
        "vendor", ".dart_tool", "__pycache__", ".next", ".nuxt", ".cache"
    )

    fun scan(projectDirectory: File): LanguageScanResult {
        if (!projectDirectory.exists() || !projectDirectory.isDirectory) {
            return emptyResult()
        }

        val root = runCatching { projectDirectory.canonicalFile }.getOrNull()
            ?: return emptyResult()
        val rootPrefix = root.path + File.separator
        val visitedDirectories = HashSet<String>()
        val filesByLanguage = linkedMapOf<String, MutableList<File>>()
        var totalFiles = 0
        var supportedFiles = 0
        var unknownFiles = 0

        fun visit(directory: File, depth: Int) {
            if (depth > 8 || totalFiles >= 10000) return
            val canonicalDirectory = runCatching { directory.canonicalFile }.getOrNull()
                ?: return
            if (
                canonicalDirectory.path != root.path &&
                !canonicalDirectory.path.startsWith(rootPrefix)
            ) return
            if (!visitedDirectories.add(canonicalDirectory.path)) return

            val children = canonicalDirectory.listFiles() ?: return

            for (child in children) {
                val canonicalChild = runCatching { child.canonicalFile }.getOrNull()
                    ?: continue
                if (
                    canonicalChild.path != root.path &&
                    !canonicalChild.path.startsWith(rootPrefix)
                ) continue
                if (totalFiles >= 10000) break

                if (child.isDirectory) {
                    if (child.name.lowercase() !in ignoredDirectories) {
                        visit(canonicalChild, depth + 1)
                    }
                } else if (canonicalChild.isFile) {
                    totalFiles++

                    val language = ProjectLanguageDetector.detectFile(canonicalChild)

                    if (language.name == "Unknown") {
                        unknownFiles++
                    } else {
                        supportedFiles++

                        filesByLanguage
                            .getOrPut(language.name) {
                                mutableListOf()
                            }
                            .add(canonicalChild)
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
