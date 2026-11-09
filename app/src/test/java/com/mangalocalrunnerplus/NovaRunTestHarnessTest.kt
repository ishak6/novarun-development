package com.mangalocalrunnerplus

import java.io.File
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class NovaRunTestHarnessTest {
    private fun fixture() = Files.createTempDirectory("novarun-harness").toFile()

    @Test fun detectsStaticWebsite() {
        val root = fixture()
        try {
            File(root, "index.html").writeText("<html></html>")
            val report = NovaRunTestHarness.inspectProject(root)
            assertTrue(report.successful)
            assertTrue(report.checks.any { it.name == "Website preview" && it.details.contains("index.html") })
        } finally { root.deleteRecursively() }
    }

    @Test fun detectsNestedWebsite() {
        val root = fixture()
        try {
            val site = File(root, "wrapper/site/public")
            assertTrue(site.mkdirs())
            File(site, "index.html").writeText("<html></html>")
            assertTrue(ProjectPreviewValidator.validate(root).isValid)
        } finally { root.deleteRecursively() }
    }

    @Test fun rejectsTraversalAndAbsolutePaths() {
        val root = fixture()
        try {
            File(root, "main.py").writeText("print('ok')")
            assertFalse(RuntimeExecutionValidator.validate(root, "../secret.txt").valid)
            assertFalse(RuntimeExecutionValidator.validate(root, "/etc/passwd").valid)
        } finally { root.deleteRecursively() }
    }

    @Test fun inspectsNodeWithoutRunningScripts() {
        val root = fixture()
        try {
            val file = File(root, "package.json")
            file.writeText("{\"name\":\"demo\",\"scripts\":{\"build\":\"vite build\",\"postinstall\":\"node setup.js\"},\"devDependencies\":{\"vite\":\"^6\"}}")
            val report = NodeProjectInspector.inspect(file, setOf("package.json", "package-lock.json"))
            assertEquals("npm", report.packageManager)
            assertEquals("npm run build", report.buildCommand)
            assertTrue(report.hasLifecycleScripts)
        } finally { root.deleteRecursively() }
    }

    @Test fun inspectsComposerWithoutRunningScripts() {
        val root = fixture()
        try {
            val file = File(root, "composer.json")
            file.writeText("{\"require\":{\"php\":\"^8.2\",\"laravel/framework\":\"^11\"},\"scripts\":{\"post-install-cmd\":\"php setup.php\"}}")
            val report = PhpProjectInspector.inspect(file, setOf("composer.json", "public/index.php"))
            assertEquals("Laravel", report.framework)
            assertEquals("public/index.php", report.entryPoint)
            assertTrue(report.warnings.any { it.contains("explicit approval") })
        } finally { root.deleteRecursively() }
    }

    @Test fun unintegratedRuntimesAreNotMarkedReady() {
        assertEquals(RuntimeStatus.AVAILABLE_FOR_EXECUTION, RuntimeCapabilityManager.getCapability("Node.js").status)
        listOf("npm", "PHP", "Java", "Kotlin", "C", "C++").forEach {
            assertEquals(RuntimeStatus.ADAPTER_PLANNED, RuntimeCapabilityManager.getCapability(it).status)
        }
    }


    @Test fun detectsRunnableNodeBackendZipLayout() {
        val root = fixture()
        try {
            val nested = File(root, "clothing_store_secure")
            val public = File(nested, "public")
            assertTrue(public.mkdirs())
            File(nested, "server.js").writeText("require('node:http').createServer().listen(3000)")
            File(nested, "package.json").writeText("""{"engines":{"node":">=22.13.0"},"scripts":{"start":"node server.js"}}""")
            File(public, "index.html").writeText("<html><body>Store</body></html>")
            File(nested, "data").mkdirs()
            File(nested, "data/.gitkeep").writeText("")
            val analysis = ProjectZipAnalyzer.analyze(root)
            assertEquals("Node.js backend", analysis.projectKind)
            assertFalse(analysis.requiresBuild)
            assertTrue(analysis.missingRuntimes.isEmpty())
            assertTrue(analysis.nextStep.contains("Localhost"))
        } finally { root.deleteRecursively() }
    }

    @Test fun reportCountsStatuses() {
        val report = HarnessReport(listOf(
            HarnessCheck("a", HarnessStatus.PASS, "ok"),
            HarnessCheck("b", HarnessStatus.FAIL, "failed"),
            HarnessCheck("c", HarnessStatus.SKIPPED, "not applicable")
        ), 1L, 2L)
        assertEquals(1, report.passed)
        assertEquals(1, report.failed)
        assertEquals(1, report.skipped)
        assertFalse(report.successful)
    }
}
