package com.mangalocalrunnerplus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ProjectPreviewValidatorTest {

    @Test
    fun acceptsIndexHtmlInProjectRoot() {
        val root = Files.createTempDirectory("novarun-preview-root").toFile()
        try {
            val index = File(root, "index.html")
            index.writeText("<html>root</html>")
            val result = ProjectPreviewValidator.validate(root)
            assertTrue(result.isValid)
            assertEquals(root.canonicalFile, result.projectRoot)
            assertEquals(index.canonicalFile, result.entryFile)
        } finally { root.deleteRecursively() }
    }

    @Test
    fun prefersDistOutputOverRootIndex() {
        val root = Files.createTempDirectory("novarun-preview-dist").toFile()
        try {
            File(root, "index.html").writeText("<html>source</html>")
            val dist = File(root, "dist")
            dist.mkdirs()
            val builtIndex = File(dist, "index.html")
            builtIndex.writeText("<html>built</html>")
            val result = ProjectPreviewValidator.validate(root)
            assertTrue(result.isValid)
            assertEquals(dist.canonicalFile, result.projectRoot)
            assertEquals(builtIndex.canonicalFile, result.entryFile)
            assertTrue(result.message.contains("dist"))
        } finally { root.deleteRecursively() }
    }

    @Test
    fun findsWebsiteInsideUnstructuredNestedFolder() {
        val root = Files.createTempDirectory("novarun-preview-nested").toFile()
        try {
            val site = File(root, "downloaded/source/site")
            site.mkdirs()
            val index = File(site, "index.html")
            index.writeText("<html><link rel='stylesheet' href='assets/site.css'></html>")
            File(site, "assets").mkdirs()
            File(site, "assets/site.css").writeText("body{}")
            File(root, ".gitkeep").writeText("")
            val result = ProjectPreviewValidator.validate(root)
            assertTrue(result.isValid)
            assertEquals(site.canonicalFile, result.projectRoot)
            assertEquals(index.canonicalFile, result.entryFile)
        } finally { root.deleteRecursively() }
    }

    @Test
    fun ignoresDependencyFoldersWhenFindingEntryPoint() {
        val root = Files.createTempDirectory("novarun-preview-deps").toFile()
        try {
            val dependency = File(root, "node_modules/example")
            dependency.mkdirs()
            File(dependency, "index.html").writeText("<html>dependency</html>")
            val result = ProjectPreviewValidator.validate(root)
            assertFalse(result.isValid)
        } finally { root.deleteRecursively() }
    }

    @Test
    fun acceptsBuildAndOutOutputDirectories() {
        listOf("build", "out").forEach { outputName ->
            val root = Files.createTempDirectory("novarun-preview-$outputName").toFile()
            try {
                val output = File(root, outputName)
                output.mkdirs()
                File(output, "index.html").writeText("<html></html>")
                val result = ProjectPreviewValidator.validate(root)
                assertTrue("$outputName should be previewable", result.isValid)
                assertEquals(output.canonicalFile, result.projectRoot)
            } finally { root.deleteRecursively() }
        }
    }

    @Test
    fun acceptsCommonHtmlHomePageNames() {
        listOf("index.htm", "default.html", "home.html", "main.html", "start.html").forEach { name ->
            val root = Files.createTempDirectory("novarun-preview-$name").toFile()
            try {
                val page = File(root, name)
                page.writeText("<html>home</html>")
                val result = ProjectPreviewValidator.validate(root)
                assertTrue("$name should be previewable", result.isValid)
                assertEquals(root.canonicalFile, result.projectRoot)
                assertEquals(page.canonicalFile, result.entryFile)
            } finally {
                root.deleteRecursively()
            }
        }
    }

    @Test
    fun rejectsMissingIndexHtml() {
        val root = Files.createTempDirectory("novarun-preview-missing").toFile()
        try {
            File(root, "README.md").writeText("# project")
            val result = ProjectPreviewValidator.validate(root)
            assertFalse(result.isValid)
            assertTrue(result.message.contains("index.html"))
        } finally { root.deleteRecursively() }
    }

    @Test
    fun rejectsNonDirectoryAndMissingProjectPaths() {
        val parent = Files.createTempDirectory("novarun-preview-invalid").toFile()
        try {
            val file = File(parent, "project.zip")
            file.writeText("not a directory")
            assertFalse(ProjectPreviewValidator.validate(file).isValid)
            val missing = File(parent, "missing")
            val result = ProjectPreviewValidator.validate(missing)
            assertFalse(result.isValid)
            assertTrue(result.message.contains("does not exist"))
        } finally { parent.deleteRecursively() }
    }

    @Test
    fun rejectsIndexHtmlSymlinkEscapingProjectRoot() {
        val parent = Files.createTempDirectory("novarun-preview-symlink").toFile()
        try {
            val root = File(parent, "project").apply { mkdirs() }
            val outsideIndex = File(parent, "outside.html").apply { writeText("<html>outside</html>") }
            Files.createSymbolicLink(File(root, "index.html").toPath(), outsideIndex.toPath())
            assertFalse(ProjectPreviewValidator.validate(root).isValid)
        } finally { parent.deleteRecursively() }
    }

    @Test
    fun rejectsOutputDirectorySymlinkEscapingProjectRoot() {
        val parent = Files.createTempDirectory("novarun-preview-dist-link").toFile()
        try {
            val root = File(parent, "project").apply { mkdirs() }
            val outside = File(parent, "outside").apply { mkdirs() }
            File(outside, "index.html").writeText("<html>outside</html>")
            Files.createSymbolicLink(File(root, "dist").toPath(), outside.toPath())
            assertFalse(ProjectPreviewValidator.validate(root).isValid)
        } finally { parent.deleteRecursively() }
    }
}
