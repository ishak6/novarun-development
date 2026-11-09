package com.mangalocalrunnerplus

import java.io.File

enum class HarnessStatus { PASS, FAIL, SKIPPED }

data class HarnessCheck(val name: String, val status: HarnessStatus, val details: String)

data class HarnessReport(val checks: List<HarnessCheck>, val startedAtMillis: Long, val finishedAtMillis: Long) {
    val passed: Int get() = checks.count { it.status == HarnessStatus.PASS }
    val failed: Int get() = checks.count { it.status == HarnessStatus.FAIL }
    val skipped: Int get() = checks.count { it.status == HarnessStatus.SKIPPED }
    val successful: Boolean get() = failed == 0
}

/** Read-only diagnostics. Imported project code is never executed by this harness. */
object NovaRunTestHarness {
    fun inspectProject(projectRoot: File): HarnessReport {
        val started = System.currentTimeMillis()
        val checks = mutableListOf<HarnessCheck>()
        fun check(name: String, action: () -> String) {
            checks += try {
                action().let { details -> HarnessCheck(name, if (details.startsWith("SKIP:")) HarnessStatus.SKIPPED else HarnessStatus.PASS, details) }
            } catch (error: Throwable) {
                HarnessCheck(name, HarnessStatus.FAIL, error.message ?: error.javaClass.simpleName)
            }
        }
        check("Project root") {
            require(projectRoot.exists() && projectRoot.isDirectory && projectRoot.canRead()) {
                "Project folder is missing, unreadable, or not a directory."
            }
            "Readable project directory."
        }
        if (projectRoot.exists() && projectRoot.isDirectory) {
            check("Project scan") {
                val analysis = ProjectZipAnalyzer.analyze(projectRoot)
                require(analysis.totalFiles >= 0) { "Invalid project file count." }
                "Scanned ${analysis.totalFiles} files; languages: ${analysis.detectedLanguages.joinToString().ifBlank { "unknown" }}."
            }
            check("Website preview") {
                val result = ProjectPreviewValidator.validate(projectRoot)
                if (result.isValid) "Preview ready: ${result.entryFile?.name}."
                else "SKIP: preview not applicable or not ready: ${result.message}"
            }
            check("Path traversal protection") {
                val result = RuntimeExecutionValidator.validate(projectRoot, "../outside.txt")
                require(!result.valid) { "Traversal path was unexpectedly accepted." }
                "Traversal path rejected."
            }
            check("Node metadata") {
                val packageFile = projectRoot.walkTopDown().maxDepth(8)
                    .firstOrNull { it.isFile && it.name.equals("package.json", true) }
                if (packageFile == null) "SKIP: package.json not present."
                else {
                    val names = projectRoot.walkTopDown().maxDepth(8).filter { it.isFile }
                        .map { it.name.lowercase() }.toSet()
                    val report = NodeProjectInspector.inspect(packageFile, names)
                    "Read-only inspection: ${report.packageManager}; ${report.scripts.size} scripts; no scripts executed."
                }
            }
            check("PHP Composer metadata") {
                val composer = projectRoot.walkTopDown().maxDepth(8)
                    .firstOrNull { it.isFile && it.name.equals("composer.json", true) }
                if (composer == null) "SKIP: composer.json not present."
                else {
                    val names = projectRoot.walkTopDown().maxDepth(8).filter { it.isFile }
                        .map { it.relativeTo(projectRoot).invariantSeparatorsPath.lowercase() }.toSet()
                    val report = PhpProjectInspector.inspect(composer, names)
                    "Read-only inspection: ${report.framework ?: "unknown"}; no Composer scripts executed."
                }
            }
        }
        check("Runtime capability honesty") {
            val planned = listOf("npm", "PHP", "Java", "Kotlin", "C", "C++")
            val falselyReady = planned.filter {
                RuntimeCapabilityManager.getCapability(it).status == RuntimeStatus.AVAILABLE_FOR_EXECUTION
            }
            require(falselyReady.isEmpty()) { "Unintegrated runtimes marked ready: ${falselyReady.joinToString()}." }
            require(RuntimeCapabilityManager.getCapability("Node.js").status == RuntimeStatus.AVAILABLE_FOR_EXECUTION) {
                "Embedded Node.js runner is not represented as integrated."
            }
            "Node.js is integrated; npm installation and other toolchains remain marked as planned."
        }
        return HarnessReport(checks, started, System.currentTimeMillis())
    }
}
