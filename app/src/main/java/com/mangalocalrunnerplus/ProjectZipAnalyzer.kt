package com.mangalocalrunnerplus

import java.io.File

data class ProjectZipAnalysis(
    val summary: String,
    val nextStep: String,
    val detectedLanguages: List<String>,
    val projectKind: String,
    val previewReady: Boolean,
    val requiresBuild: Boolean,
    val missingRuntimes: List<String>,
    val totalFiles: Int,
    val nodeProject: NodeProjectReport? = null,
    val phpProject: PhpProjectReport? = null
)

/** Read-only inspection of an extracted ZIP project. This never executes project code. */
object ProjectZipAnalyzer {
    private val ignored = setOf(
        ".git", ".gradle", ".idea", "node_modules", "build",
        "dist", "vendor", ".dart_tool", "__pycache__", ".next", ".nuxt", ".cache"
    )

    fun analyze(root: File): ProjectZipAnalysis {
        if (!root.exists() || !root.isDirectory) return result("Invalid project", "Project folder is unavailable.")
        val files = mutableListOf<File>()
        val rootCanonical = runCatching { root.canonicalFile }.getOrNull()
            ?: return result("Invalid project", "Could not inspect project folder.")
        val prefix = rootCanonical.path + File.separator
        val stack = ArrayDeque<Pair<File, Int>>()
        stack.add(rootCanonical to 0)

        while (stack.isNotEmpty() && files.size < 10000) {
            val (dir, depth) = stack.removeLast()
            if (depth > 8) continue
            for (child in dir.listFiles() ?: continue) {
                val canonical = runCatching { child.canonicalFile }.getOrNull() ?: continue
                if (canonical != rootCanonical && !canonical.path.startsWith(prefix)) continue
                if (child.isDirectory) {
                    if (child.name.lowercase() !in ignored) stack.add(canonical to depth + 1)
                } else if (canonical.isFile) {
                    files.add(canonical)
                    if (files.size >= 10000) break
                }
            }
        }

        val names = files.map { it.name.lowercase() }.toSet()
        val relativePaths = files.map { it.relativeTo(rootCanonical).invariantSeparatorsPath.lowercase() }.toSet()
        val extensions = files.map { it.extension.lowercase() }.toSet()
        val languages = ProjectLanguageScanner.scan(rootCanonical).languages.map { it.name }.distinct().sorted()
        val packageFile = files.firstOrNull { it.name.equals("package.json", true) }
        val packageText = packageFile?.let { runCatching { it.readText().take(256 * 1024) }.getOrNull() }.orEmpty()
        val nodeProject = packageFile?.let { NodeProjectInspector.inspect(it, names) }
        val hasBuildScript = nodeProject?.scripts?.containsKey("build") == true
        val hasVite = "vite.config.js" in names || "vite.config.ts" in names ||
            packageText.contains("\"vite\"") || packageText.contains("\"vite\":")
        val hasReact = packageText.contains("\"react\"") || packageText.contains("\"react-dom\"")
        val hasVue = packageText.contains("\"vue\"")
        val hasAngular = packageText.contains("\"@angular/core\"")
        val composerFile = files.firstOrNull { it.name.equals("composer.json", true) }
        val phpProject = composerFile?.let { PhpProjectInspector.inspect(it, relativePaths) }
        val hasPhp = "php" in extensions || composerFile != null
        val hasPython = "py" in extensions
        val hasJava = "java" in extensions || "kt" in extensions || "kts" in extensions
        val hasNative = extensions.any { it in setOf("c", "cc", "cpp", "h", "hpp") }
        val hasGo = "go" in extensions
        val hasRust = "rs" in extensions
        val staticEntry = ProjectEntryPointFinder.find(rootCanonical)
        val previewReady = staticEntry != null
        val hasNodeBackendEntry = relativePaths.any { path ->
            path == "server.js" || path.endsWith("/server.js") ||
                path == "app.js" || path.endsWith("/app.js") ||
                path.endsWith("/backend/server.js")
        }
        val nodeBackendReady = packageFile != null && hasNodeBackendEntry
        val nodeDependenciesMissing = nodeProject != null &&
            (nodeProject.dependencyCount + nodeProject.devDependencyCount) > 0 &&
            !File(packageFile?.parentFile ?: rootCanonical, "node_modules").isDirectory
        val missing = mutableListOf<String>()
        if (nodeDependenciesMissing) missing += "npm dependencies (node_modules missing)"
        if (hasPhp) missing += "PHP runtime"
        if (hasPython && (names.any { it == "requirements.txt" } || files.any { it.name == "manage.py" || it.name == "pyproject.toml" })) {
            missing += "Python web dependencies/framework runtime"
        }
        if (hasJava) missing += "Java/Kotlin compiler + Android/JVM build toolchain"
        if (hasNative) missing += "C/C++ compiler + sysroot/linker"
        if (hasGo) missing += "Go compiler"
        if (hasRust) missing += "Rust toolchain"
        if (extensions.any { it == "rb" }) missing += "Ruby runtime"

        val kind = when {
            hasAngular -> "Angular web app"
            hasReact && hasVite -> "React + Vite"
            hasReact -> "React web app"
            hasVue && hasVite -> "Vue + Vite"
            hasVue -> "Vue web app"
            nodeBackendReady -> "Node.js backend"
            packageFile != null -> "Node.js web/project"
            previewReady -> "Static website"
            phpProject?.framework != null -> phpProject.framework + " PHP project"
            hasPhp -> "PHP website/backend"
            hasPython -> "Python project"
            hasJava -> "Java/Kotlin project"
            hasNative -> "C/C++ project"
            hasGo -> "Go project"
            hasRust -> "Rust project"
            else -> "Mixed / general project"
        }
        val languageSummary = languages.take(4).joinToString().ifBlank { "file types unknown" }
        val summary = kind + " • " + languageSummary
        val next = when {
            nodeBackendReady -> "Node.js backend detected. Start it from Localhost → Node Backend Runner. This project uses built-in SQLite and declares no npm dependencies."
            previewReady && packageFile != null -> "Static output found. Use Preview for static files or Localhost → Node Backend Runner for the full backend."
            previewReady -> "Static output found — ready for preview."
            packageFile != null && hasBuildScript ->
                "Build script detected. Command: " + (nodeProject?.buildCommand ?: "npm run build") + ". Node.js/npm runtime is required; build output not found."
            packageFile != null ->
                "package.json found. " + (nodeProject?.installCommand ?: "Install dependencies") + " and configure a build script; Node.js/npm runtime is required."
            hasPhp -> "PHP project detected" + (phpProject?.entryPoint?.let { " • entry: $it" } ?: "") + ". PHP CLI runtime is not installed in NovaRun yet."
            hasJava || hasNative || hasGo || hasRust -> "Source detected. Required compiler/toolchain is not installed in NovaRun yet."
            hasPython -> "Python source detected. Run supported .py scripts; web frameworks may need extra packages."
            else -> "No ready web entry point found. Add index.html or build static output first."
        }
        return ProjectZipAnalysis(
            summary, next, languages, kind, previewReady,
            packageFile != null && !previewReady && !nodeBackendReady &&
                (hasBuildScript || hasVite || hasReact || hasVue || hasAngular),
            missing.distinct(), files.size, nodeProject, phpProject
        )
    }

    private fun result(kind: String, next: String) =
        ProjectZipAnalysis(kind, next, emptyList(), kind, false, false, emptyList(), 0)
}
