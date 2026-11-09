package com.mangalocalrunnerplus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ProjectZipAnalyzerTest {
    @Test
    fun detectsStaticWebsiteAsPreviewReady() {
        val root = Files.createTempDirectory("novarun-static").toFile()
        try {
            File(root, "index.html").writeText("<html></html>")
            File(root, "style.css").writeText("body {}")
            val result = ProjectZipAnalyzer.analyze(root)
            assertTrue(result.previewReady)
            assertEquals("Static website", result.projectKind)
            assertTrue(result.detectedLanguages.contains("HTML"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun detectsWebsiteWhenIndexIsNestedInUnstructuredZipLayout() {
        val root = Files.createTempDirectory("novarun-nested-site").toFile()
        try {
            val site = File(root, "downloaded/project/site")
            site.mkdirs()
            File(site, "index.html").writeText("<html></html>")
            File(site, "style.css").writeText("body {}")
            File(root, ".gitkeep").writeText("")
            val result = ProjectZipAnalyzer.analyze(root)
            assertTrue(result.previewReady)
            assertEquals("Static website", result.projectKind)
            assertTrue(result.detectedLanguages.contains("HTML"))
            assertTrue(result.detectedLanguages.contains("CSS"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun detectsReactViteAndReportsMissingNpmDependenciesWhenNoBuildOutput() {
        val root = Files.createTempDirectory("novarun-vite").toFile()
        try {
            File(root, "package.json").writeText(
                """{"scripts":{"build":"vite build"},"dependencies":{"react":"^18.0.0","vite":"^5.0.0"}}"""
            )
            File(root, "src").mkdirs()
            File(root, "src/App.jsx").writeText("export default function App() {}")
            val result = ProjectZipAnalyzer.analyze(root)
            assertEquals("React + Vite", result.projectKind)
            assertFalse(result.previewReady)
            assertTrue(result.requiresBuild)
            assertTrue(result.missingRuntimes.any { it.startsWith("npm dependencies") })
            assertEquals("npm run build", result.nodeProject?.buildCommand)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun detectsBuildScriptWhenPackageJsonUsesWhitespaceFormatting() {
        val root = Files.createTempDirectory("novarun-build-script").toFile()
        try {
            File(root, "package.json").writeText(
                """
                {
                  "scripts": {
                    "build" : "vite build"
                  },
                  "devDependencies": {
                    "vite": "^5.0.0"
                  }
                }
                """.trimIndent()
            )
            val result = ProjectZipAnalyzer.analyze(root)
            assertTrue(result.requiresBuild)
            assertTrue(result.nextStep.startsWith("Build script detected."))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun recognizesExistingViteDistOutputAsPreviewReady() {
        val root = Files.createTempDirectory("novarun-dist").toFile()
        try {
            File(root, "package.json").writeText("""{"scripts":{"build":"vite build"}}""")
            val dist = File(root, "dist")
            dist.mkdirs()
            File(dist, "index.html").writeText("<html></html>")
            val result = ProjectZipAnalyzer.analyze(root)
            assertTrue(result.previewReady)
            assertFalse(result.requiresBuild)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun analyzerRecognizesComposerProjectWithNestedPublicEntryPoint() {
        val root = Files.createTempDirectory("novarun-laravel").toFile()
        try {
            File(root, "composer.json").writeText(
                """{"name":"demo/app","require":{"php":"^8.2","laravel/framework":"^11.0"}}"""
            )
            File(root, "composer.lock").writeText("{}")
            File(root, "public").mkdirs()
            File(root, "public/index.php").writeText("<?php")
            val result = ProjectZipAnalyzer.analyze(root)
            assertEquals("Laravel PHP project", result.projectKind)
            assertTrue(result.missingRuntimes.any { it.startsWith("PHP runtime") })
            assertEquals("public/index.php", result.phpProject?.entryPoint)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun identifiesPhpAndNativeCompilerRequirements() {
        val root = Files.createTempDirectory("novarun-mixed").toFile()
        try {
            File(root, "index.php").writeText("<?php echo 'hi';")
            File(root, "main.cpp").writeText("int main() { return 0; }")
            val result = ProjectZipAnalyzer.analyze(root)
            assertTrue(result.missingRuntimes.any { it.startsWith("PHP runtime") })
            assertTrue(result.missingRuntimes.any { it.startsWith("C/C++ compiler") })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun ignoresUppercaseDependencyAndBuildDirectoriesDuringAnalysis() {
        val root = Files.createTempDirectory("novarun-uppercase-ignore").toFile()
        try {
            File(root, "index.html").writeText("<html></html>")
            File(root, "NODE_MODULES/fake-package").mkdirs()
            File(root, "NODE_MODULES/fake-package/package.json")
                .writeText("""{"dependencies":{"react":"*","vite":"*"}}""")
            File(root, "DIST/generated").mkdirs()
            File(root, "DIST/generated/app.js").writeText("generated();")
            val result = ProjectZipAnalyzer.analyze(root)
            assertEquals("Static website", result.projectKind)
            assertTrue(result.previewReady)
            assertFalse(result.detectedLanguages.contains("JavaScript"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun scannerIgnoresUppercaseDependencyDirectories() {
        val root = Files.createTempDirectory("novarun-uppercase-scan").toFile()
        try {
            File(root, "main.py").writeText("print('ok')")
            File(root, "NODE_MODULES/pkg/index.js").apply {
                parentFile.mkdirs()
                writeText("module.exports = 1")
            }
            File(root, ".GIT/hidden.py").apply {
                parentFile.mkdirs()
                writeText("print('hidden')")
            }
            val result = ProjectLanguageScanner.scan(root)
            assertEquals(1, result.totalFiles)
            assertTrue(result.languages.any { it.name == "Python" })
            assertFalse(result.languages.any { it.name == "JavaScript" })
        } finally {
            root.deleteRecursively()
        }
    }
}
