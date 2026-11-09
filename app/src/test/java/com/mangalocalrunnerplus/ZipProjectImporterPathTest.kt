package com.mangalocalrunnerplus

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Test

class ZipProjectImporterPathTest {

    @Test
    fun acceptsNestedRelativePath() {
        assertEquals(
            "src${java.io.File.separator}main${java.io.File.separator}Main.kt",
            ZipProjectImporter.validateEntryName("src/main/Main.kt")
        )
    }

    @Test(expected = IOException::class)
    fun rejectsParentDirectoryTraversal() {
        ZipProjectImporter.validateEntryName("../outside.txt")
    }

    @Test(expected = IOException::class)
    fun rejectsWindowsStyleTraversal() {
        ZipProjectImporter.validateEntryName("..\\outside.txt")
    }

    @Test(expected = IOException::class)
    fun rejectsAbsolutePath() {
        ZipProjectImporter.validateEntryName("/etc/passwd")
    }

    @Test(expected = IOException::class)
    fun rejectsDriveQualifiedPath() {
        ZipProjectImporter.validateEntryName("C:/Windows/system.ini")
    }

    @Test(expected = IOException::class)
    fun rejectsEmptyPath() {
        ZipProjectImporter.validateEntryName("///")
    }
}
