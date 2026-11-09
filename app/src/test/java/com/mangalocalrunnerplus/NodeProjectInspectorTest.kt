package com.mangalocalrunnerplus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class NodeProjectInspectorTest {
    @Test
    fun detectsNpmLockfileScriptsDependenciesAndNativeAddonWarnings() {
        val root = Files.createTempDirectory("novarun-node").toFile()
        try {
            File(root, "package.json").writeText(
                """
                {
                  "name": "sample-web",
                  "version": "1.2.3",
                  "scripts": {"build": "vite build", "postinstall": "node setup.js"},
                  "dependencies": {"react": "^18.0.0", "better-sqlite3": "^11.0.0"},
                  "devDependencies": {"vite": "^6.0.0"}
                }
                """.trimIndent()
            )
            File(root, "package-lock.json").writeText("{}")
            val report = NodeProjectInspector.inspect(File(root, "package.json"), setOf("package.json", "package-lock.json"))
            assertEquals("sample-web", report.packageName)
            assertEquals("1.2.3", report.packageVersion)
            assertEquals("npm (default)", report.packageManager)
            assertEquals("package-lock.json", report.lockfileName)
            assertEquals(2, report.dependencyCount)
            assertEquals(1, report.devDependencyCount)
            assertEquals("npm ci", report.installCommand)
            assertEquals("npm (default) run build", report.buildCommand)
            assertTrue(report.hasNativeAddonIndicators)
            assertTrue(report.hasLifecycleScripts)
            assertTrue(report.warnings.any { it.contains("Native Node modules") })
        } finally { root.deleteRecursively() }
    }

    @Test
    fun infersPnpmFromLockfileAndUsesFrozenInstallCommand() {
        val root = Files.createTempDirectory("novarun-pnpm").toFile()
        try {
            File(root, "package.json").writeText("""{"packageManager":"pnpm@9.0.0","scripts":{"build":"vite build"}}""")
            val report = NodeProjectInspector.inspect(File(root, "package.json"), setOf("package.json", "pnpm-lock.yaml"))
            assertEquals("pnpm", report.packageManager)
            assertEquals("pnpm install --frozen-lockfile", report.installCommand)
            assertEquals("pnpm run build", report.buildCommand)
            assertFalse(report.warnings.any { it.contains("No package lockfile") })
        } finally { root.deleteRecursively() }
    }

    @Test
    fun reportsMissingLockfileAndNoBuildScriptWithoutExecutingAnything() {
        val root = Files.createTempDirectory("novarun-no-lock").toFile()
        try {
            File(root, "package.json").writeText("""{"name":"tiny","dependencies":{"left-pad":"1.0.0"}}""")
            val report = NodeProjectInspector.inspect(File(root, "package.json"), setOf("package.json"))
            assertEquals("npm install", report.installCommand)
            assertEquals(null, report.buildCommand)
            assertTrue(report.warnings.any { it.contains("No package lockfile") })
        } finally { root.deleteRecursively() }
    }
}
