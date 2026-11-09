package com.mangalocalrunnerplus

import java.io.File

data class PhpProjectReport(
    val packageName: String?,
    val framework: String?,
    val phpVersionConstraint: String?,
    val lockfileName: String?,
    val dependencyCount: Int,
    val devDependencyCount: Int,
    val requiredExtensions: List<String>,
    val scripts: Map<String, String>,
    val entryPoint: String?,
    val warnings: List<String>
)

/** Read-only Composer/PHP project inspection. It never runs Composer or PHP scripts. */
object PhpProjectInspector {
    fun inspect(composerFile: File, projectFileNames: Set<String>): PhpProjectReport {
        val source = runCatching { composerFile.readText(Charsets.UTF_8).take(256 * 1024) }.getOrDefault("")
        val require = readStringMap(readObject(source, "require"))
        val requireDev = readStringMap(readObject(source, "require-dev"))
        val all = require.keys + requireDev.keys
        val framework = when {
            "laravel/framework" in all -> "Laravel"
            "symfony/framework-bundle" in all -> "Symfony"
            "slim/slim" in all -> "Slim"
            "cakephp/cakephp" in all -> "CakePHP"
            "codeigniter4/framework" in all -> "CodeIgniter 4"
            else -> null
        }
        val extensions = all.filter { it.startsWith("ext-") }.sorted()
        val entry = when {
            "public/index.php" in projectFileNames -> "public/index.php"
            "index.php" in projectFileNames -> "index.php"
            "artisan" in projectFileNames -> "artisan"
            else -> null
        }
        val lockfile = if ("composer.lock" in projectFileNames) "composer.lock" else null
        val scripts = readStringMap(readObject(source, "scripts"))
        val warnings = buildList {
            if (lockfile == null) add("composer.lock is missing; dependency resolution may not be reproducible.")
            if (extensions.isNotEmpty()) add("Required PHP extensions must be present in the selected PHP build: " + extensions.joinToString())
            if (scripts.isNotEmpty()) add("Composer scripts are declared; they must require explicit approval before execution.")
            if (entry == null) add("No conventional PHP web entry point was found.")
        }
        return PhpProjectReport(
            readTopLevelString(source, "name").ifBlank { null },
            framework,
            require["php"],
            lockfile,
            require.keys.count { it != "php" && !it.startsWith("ext-") },
            requireDev.keys.count { it != "php" && !it.startsWith("ext-") },
            extensions,
            scripts,
            entry,
            warnings
        )
    }

    private fun readTopLevelString(source: String, key: String): String {
        val pattern = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
        return pattern.find(source)?.groupValues?.get(1)?.let(::decodeJsonString).orEmpty()
    }

    private fun readObject(source: String, key: String): String {
        val match = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*\\{").find(source) ?: return ""
        val start = source.indexOf('{', match.range.first)
        var depth = 0
        var inString = false
        var escaped = false
        for (index in start until source.length) {
            val ch = source[index]
            if (inString) {
                if (escaped) escaped = false
                else if (ch == '\\') escaped = true
                else if (ch == '"') inString = false
            } else {
                when (ch) {
                    '"' -> inString = true
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) return source.substring(start + 1, index)
                    }
                }
            }
        }
        return ""
    }

    private fun readStringMap(source: String): Map<String, String> {
        if (source.isBlank()) return emptyMap()
        val pattern = Regex("\"((?:\\\\.|[^\"\\\\])*)\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
        return pattern.findAll(source).associate {
            decodeJsonString(it.groupValues[1]) to decodeJsonString(it.groupValues[2])
        }
    }

    private fun decodeJsonString(value: String): String = value
        .replace("\\\"", "\"")
        .replace("\\\\", "\\")
        .replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\t", "\t")
}
