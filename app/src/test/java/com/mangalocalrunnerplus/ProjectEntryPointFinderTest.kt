package com.mangalocalrunnerplus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ProjectEntryPointFinderTest {
    @Test
    fun prefersNestedBuildOutputOverUnbuiltRootIndex() {
        val root = Files.createTempDirectory("novarun-entry-build").toFile()
        try {
            File(root, "index.html").writeText("<html>source</html>")
            val built = File(root, "downloaded/site/dist")
            built.mkdirs()
            val builtIndex = File(built, "index.html")
            builtIndex.writeText("<html>built</html>")

            assertEquals(builtIndex.canonicalFile, ProjectEntryPointFinder.find(root))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun recognizesUppercaseNestedOutputDirectory() {
        val root = Files.createTempDirectory("novarun-entry-case").toFile()
        try {
            File(root, "site").mkdirs()
            File(root, "site/index.html").writeText("<html>source</html>")
            val output = File(root, "download/BUILD")
            output.mkdirs()
            val builtIndex = File(output, "index.html")
            builtIndex.writeText("<html>built</html>")

            assertEquals(builtIndex.canonicalFile, ProjectEntryPointFinder.find(root))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun ignoresDependencyDirectoriesCaseInsensitively() {
        val root = Files.createTempDirectory("novarun-entry-ignore").toFile()
        try {
            val dependency = File(root, "Node_Modules/package")
            dependency.mkdirs()
            File(dependency, "index.html").writeText("<html>dependency</html>")

            assertTrue(ProjectEntryPointFinder.find(root) == null)
        } finally {
            root.deleteRecursively()
        }
    }
}
