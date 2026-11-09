package com.mangalocalrunnerplus

import java.io.File

data class NodeProjectReport(
    val packageName: String?,
    val packageVersion: String?,
    val packageManager: String,
    val lockfileName: String?,
    val scripts: Map<String, String>,
    val dependencyCount: Int,
    val devDependencyCount: Int,
    val hasNativeAddonIndicators: Boolean,
    val hasLifecycleScripts: Boolean,
    val installCommand: String,
    val buildCommand: String?,
    val warnings: List<String>
)

/** Read-only package.json inspection. Never runs npm, scripts, lifecycle hooks, or downloaded code. */
object NodeProjectInspector {
    fun inspect(packageFile: File, projectFileNames: Set<String>): NodeProjectReport {
        val source = runCatching { packageFile.readText(Charsets.UTF_8).take(256 * 1024) }.getOrDefault("")
        val packageName = readTopLevelString(source, "name").ifBlank { null }
        val packageVersion = readTopLevelString(source, "version").ifBlank { null }
        val declaredManager = readTopLevelString(source, "packageManager")
        val scripts = readStringMap(readObject(source, "scripts"))
        val dependencies = readStringMap(readObject(source, "dependencies"))
        val devDependencies = readStringMap(readObject(source, "devDependencies"))
        val lockfile = when {
            "pnpm-lock.yaml" in projectFileNames -> "pnpm-lock.yaml"
            "yarn.lock" in projectFileNames -> "yarn.lock"
            "bun.lock" in projectFileNames -> "bun.lock"
            "bun.lockb" in projectFileNames -> "bun.lockb"
            "npm-shrinkwrap.json" in projectFileNames -> "npm-shrinkwrap.json"
            "package-lock.json" in projectFileNames -> "package-lock.json"
            else -> null
        }
        val manager = when {
            declaredManager.startsWith("pnpm") -> "pnpm"
            declaredManager.startsWith("yarn") -> "yarn"
            declaredManager.startsWith("bun") -> "bun"
            declaredManager.startsWith("npm") -> "npm"
            lockfile == "pnpm-lock.yaml" -> "pnpm"
            lockfile == "yarn.lock" -> "yarn"
            lockfile == "bun.lock" || lockfile == "bun.lockb" -> "bun"
            else -> "npm"
        }
        val installCommand = when {
            manager == "pnpm" && lockfile != null -> "pnpm install --frozen-lockfile"
            manager == "yarn" && lockfile != null -> "yarn install --immutable"
            manager == "bun" && lockfile != null -> "bun install --frozen-lockfile"
            manager == "npm" && lockfile != null -> "npm ci"
            manager == "pnpm" -> "pnpm install"
            manager == "yarn" -> "yarn install"
            manager == "bun" -> "bun install"
            else -> "npm install"
        }
        val allDependencies = dependencies.keys + devDependencies.keys
        val nativeNames = setOf("node-gyp", "node-addon-api", "bindings", "sharp", "canvas",
            "better-sqlite3", "sqlite3", "bcrypt", "node-sass")
        val nativeIndicators = allDependencies.any { it in nativeNames } ||
            source.contains("node-gyp") || source.contains("node-addon-api")
        val lifecycleScripts = scripts.keys.any { it in setOf("preinstall", "install", "postinstall", "prepare") }
        val warnings = buildList {
            if (lockfile == null) add("No package lockfile found; dependency versions may not be reproducible.")
            if (nativeIndicators) add("Native Node modules detected; Android ABI-compatible binaries and build tooling are required.")
            if (lifecycleScripts) add("Package lifecycle scripts are present; they must require explicit approval before execution.")
            if (dependencies.isEmpty() && devDependencies.isEmpty()) add("No dependencies declared in package.json.")
        }
        val buildCommand = if ("build" in scripts) manager + " run build" else null
        return NodeProjectReport(
            packageName, packageVersion, manager, lockfile, scripts,
            dependencies.size, devDependencies.size, nativeIndicators, lifecycleScripts,
            installCommand, buildCommand, warnings
        )
    }

    private fun readTopLevelString(source: String, key: String): String {
        val prefix = "\"" + Regex.escape(key) + "\"\\s*:\\s*\""
        val match = Regex(prefix + "((?:\\\\.|[^\"\\\\])*)\"").find(source) ?: return ""
        return decodeJsonString(match.groupValues[1])
    }

    private fun readObject(source: String, key: String): String {
        val match = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*\\{").find(source) ?: return ""
        val start = source.indexOf('{', match.range.first)
        var depth = 0
        var inString = false
        var escaped = false
        for (index in start until source.length) {
            val char = source[index]
            if (inString) {
                if (escaped) escaped = false
                else if (char == '\\') escaped = true
                else if (char == '"') inString = false
            } else {
                when (char) {
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
