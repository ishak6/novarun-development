package com.mangalocalrunnerplus

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectFileOperationsTest {
    @Test
    fun createsListsRenamesAndDeletesFilesAndFolders() {
        val temp = Files.createTempDirectory("novarun-file-ops-").toFile()
        val root = File(temp, "project").apply { mkdirs() }
        try {
            val folder = ProjectFileOperations.createFolder(root, root, "assets")
            val file = ProjectFileOperations.createFile(root, folder, "index.html")
            file.writeText("<html></html>")
            assertTrue(ProjectFileOperations.list(root, root).contains(folder))
            assertTrue(ProjectFileOperations.list(root, folder).contains(file))

            val renamed = ProjectFileOperations.rename(root, file, "main.html")
            assertFalse(file.exists())
            assertTrue(renamed.isFile)
            ProjectFileOperations.delete(root, folder)
            assertFalse(folder.exists())
        } finally {
            temp.deleteRecursively()
        }
    }

    @Test
    fun permitsHiddenConfigurationFiles() {
        val temp = Files.createTempDirectory("novarun-file-hidden-").toFile()
        val root = File(temp, "project").apply { mkdirs() }
        try {
            val file = ProjectFileOperations.createFile(root, root, ".env")
            assertTrue(file.isFile)
        } finally {
            temp.deleteRecursively()
        }
    }

    @Test
    fun rejectsTraversalControlCharactersAndUnsafeNames() {
        listOf("../outside", "..", "bad/name", "bad\\name", "bad\nname", "bad:name", "bad|name")
            .forEach { name ->
                assertTrue("Expected unsafe name to be rejected: $name",
                    runCatching { ProjectFileNamePolicy.validate(name) }.isFailure)
            }
        assertTrue(runCatching { ProjectFileNamePolicy.validate("x".repeat(256)) }.isFailure)
    }

    @Test
    fun rejectsSymlinksAndNeverDeletesExternalTargets() {
        val temp = Files.createTempDirectory("novarun-file-symlink-").toFile()
        val root = File(temp, "project").apply { mkdirs() }
        val outside = File(temp, "outside").apply { mkdirs() }
        File(outside, "keep.txt").writeText("safe")
        try {
            val link = File(root, "external")
            Files.createSymbolicLink(link.toPath(), outside.toPath())
            assertTrue(runCatching { ProjectFileOperations.list(root, root) }.isSuccess)
            assertFalse(ProjectFileOperations.list(root, root).any { it.name == "external" })
            assertTrue(runCatching { ProjectFileOperations.delete(root, link) }.isFailure)
            assertTrue(File(outside, "keep.txt").exists())
        } finally {
            temp.deleteRecursively()
        }
    }

    @Test
    fun recursiveDeleteDoesNotFollowNestedExternalSymlinks() {
        val temp = Files.createTempDirectory("novarun-file-nested-link-").toFile()
        val root = File(temp, "project").apply { mkdirs() }
        val outside = File(temp, "outside").apply { mkdirs() }
        val nested = File(root, "nested").apply { mkdirs() }
        File(outside, "keep.txt").writeText("safe")
        try {
            Files.createSymbolicLink(File(nested, "external").toPath(), outside.toPath())
            ProjectFileOperations.delete(root, nested)
            assertFalse(nested.exists())
            assertTrue(File(outside, "keep.txt").exists())
        } finally {
            temp.deleteRecursively()
        }
    }

    @Test
    fun refusesDeletingProjectRootAndOutsideDirectories() {
        val temp = Files.createTempDirectory("novarun-file-root-").toFile()
        val root = File(temp, "project").apply { mkdirs() }
        val outside = File(temp, "outside").apply { mkdirs() }
        try {
            assertTrue(runCatching { ProjectFileOperations.delete(root, root) }.isFailure)
            assertTrue(runCatching { ProjectFileOperations.createFile(root, outside, "bad.txt") }.isFailure)
            assertTrue(root.isDirectory)
            assertTrue(outside.isDirectory)
        } finally {
            temp.deleteRecursively()
        }
    }

    @Test
    fun validatesUnicodeFileNamesWithoutBreakingThem() {
        assertEquals("বাংলা-ফাইল.txt", ProjectFileNamePolicy.validate("বাংলা-ফাইল.txt"))
    }
}
