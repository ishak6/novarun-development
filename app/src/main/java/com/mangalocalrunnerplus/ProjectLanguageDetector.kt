
package com.mangalocalrunnerplus

import java.io.File

data class LanguageInfo(
    val name: String,
    val extension: String,
    val category: String,
    val isProgrammingLanguage: Boolean = true
)

object ProjectLanguageDetector {

    private val languages = mapOf(
        "html" to LanguageInfo("HTML", "html", "Web"),
        "htm" to LanguageInfo("HTML", "htm", "Web"),
        "css" to LanguageInfo("CSS", "css", "Web"),
        "js" to LanguageInfo("JavaScript", "js", "Web"),
        "jsx" to LanguageInfo("JavaScript JSX", "jsx", "Web"),
        "ts" to LanguageInfo("TypeScript", "ts", "Web"),
        "tsx" to LanguageInfo("TypeScript JSX", "tsx", "Web"),

        "php" to LanguageInfo("PHP", "php", "Backend"),
        "py" to LanguageInfo("Python", "py", "Backend"),
        "rb" to LanguageInfo("Ruby", "rb", "Backend"),

        "java" to LanguageInfo("Java", "java", "JVM"),
        "kt" to LanguageInfo("Kotlin", "kt", "JVM"),
        "kts" to LanguageInfo("Kotlin Script", "kts", "JVM"),

        "go" to LanguageInfo("Go", "go", "Backend"),
        "cs" to LanguageInfo("C#", "cs", ".NET"),
        "dart" to LanguageInfo("Dart", "dart", "Mobile"),
        "c" to LanguageInfo("C", "c", "Systems"),
        "h" to LanguageInfo("C Header", "h", "Systems"),
        "cpp" to LanguageInfo("C++", "cpp", "Systems"),
        "cc" to LanguageInfo("C++", "cc", "Systems"),
        "hpp" to LanguageInfo("C++ Header", "hpp", "Systems"),
        "rs" to LanguageInfo("Rust", "rs", "Systems"),
        "swift" to LanguageInfo("Swift", "swift", "Mobile"),
        "lua" to LanguageInfo("Lua", "lua", "Scripting"),
        "pl" to LanguageInfo("Perl", "pl", "Scripting"),
        "hs" to LanguageInfo("Haskell", "hs", "Functional"),
        "r" to LanguageInfo("R", "r", "Data Science"),
        "jl" to LanguageInfo("Julia", "jl", "Data Science"),

        "sql" to LanguageInfo(
            "SQL",
            "sql",
            "Database",
            isProgrammingLanguage = false
        ),

        "sh" to LanguageInfo("Bash", "sh", "Scripting"),
        "bash" to LanguageInfo("Bash", "bash", "Scripting"),

        "json" to LanguageInfo(
            "JSON",
            "json",
            "Data Format",
            isProgrammingLanguage = false
        ),

        "xml" to LanguageInfo(
            "XML",
            "xml",
            "Data Format",
            isProgrammingLanguage = false
        ),

        "yaml" to LanguageInfo(
            "YAML",
            "yaml",
            "Configuration",
            isProgrammingLanguage = false
        ),

        "yml" to LanguageInfo(
            "YAML",
            "yml",
            "Configuration",
            isProgrammingLanguage = false
        ),

        "md" to LanguageInfo(
            "Markdown",
            "md",
            "Documentation",
            isProgrammingLanguage = false
        ),

        "txt" to LanguageInfo(
            "Plain Text",
            "txt",
            "Text",
            isProgrammingLanguage = false
        )
    )

    fun detectFile(file: File): LanguageInfo {
        val extension = file.extension.lowercase()

        return languages[extension]
            ?: LanguageInfo(
                name = "Unknown",
                extension = extension,
                category = "Unsupported",
                isProgrammingLanguage = false
            )
    }

    fun detectProject(projectDirectory: File): List<LanguageInfo> {
        if (!projectDirectory.exists() || !projectDirectory.isDirectory) {
            return emptyList()
        }

        val foundLanguages = linkedMapOf<String, LanguageInfo>()

        fun scan(directory: File, depth: Int) {
            if (depth > 5 || foundLanguages.size >= 30) return

            val children = directory.listFiles() ?: return

            for (child in children) {
                if (foundLanguages.size >= 30) return

                if (child.isDirectory) {
                    if (child.name !in ignoredDirectories) {
                        scan(child, depth + 1)
                    }
                } else if (child.isFile) {
                    val language = detectFile(child)

                    if (language.name != "Unknown") {
                        foundLanguages.putIfAbsent(
                            language.name,
                            language
                        )
                    }
                }
            }
        }

        scan(projectDirectory, 0)

        return foundLanguages.values.toList()
    }

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
}
