package com.mangalocalrunnerplus

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class RuntimeExecutionValidatorTest {
    @Test
    fun acceptsReadableProjectRelativeFile() {
        val root = Files.createTempDirectory("novarun-runtime-valid").toFile()
        try {
            File(root, "src").mkdirs()
            File(root, "src/main.py").writeText("print('ok')")
            val result = RuntimeExecutionValidator.validate(root, "src/main.py")
            assertTrue(result.valid)
            assertTrue(result.entryFile!!.canonicalPath.startsWith(root.canonicalPath + File.separator))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectsParentTraversal() {
        val parent = Files.createTempDirectory("novarun-runtime-traversal").toFile()
        try {
            val root = File(parent, "project").apply { mkdirs() }
            File(parent, "outside.py").writeText("print('outside')")
            assertFalse(RuntimeExecutionValidator.validate(root, "../outside.py").valid)
        } finally {
            parent.deleteRecursively()
        }
    }

    @Test
    fun rejectsAbsoluteEntryPaths() {
        val root = Files.createTempDirectory("novarun-runtime-absolute").toFile()
        try {
            val outside = File(root.parentFile, "outside.py")
            outside.writeText("print('outside')")
            assertFalse(RuntimeExecutionValidator.validate(root, outside.absolutePath).valid)
        } finally {
            root.deleteRecursively()
            File(root.parentFile, "outside.py").delete()
        }
    }

    @Test
    fun rejectsSymlinkEscapingProject() {
        val parent = Files.createTempDirectory("novarun-runtime-link").toFile()
        try {
            val root = File(parent, "project").apply { mkdirs() }
            File(parent, "outside.py").writeText("print('outside')")
            Files.createSymbolicLink(File(root, "escape.py").toPath(), File(parent, "outside.py").toPath())
            assertFalse(RuntimeExecutionValidator.validate(root, "escape.py").valid)
        } finally {
            parent.deleteRecursively()
        }
    }

    @Test
    fun rejectsEmptyAndOverlongPaths() {
        val root = Files.createTempDirectory("novarun-runtime-path").toFile()
        try {
            assertFalse(RuntimeExecutionValidator.validate(root, " ").valid)
            assertFalse(RuntimeExecutionValidator.validate(root, "a".repeat(1025)).valid)
        } finally {
            root.deleteRecursively()
        }
    }
}
