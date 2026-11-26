package com.mangalocalrunnerplus

import java.nio.file.Files
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectImagePreviewTest {
    @Test
    fun recognizesSupportedRasterVectorAndModernImageFormats() {
        val root = Files.createTempDirectory("novarun-image-types").toFile()
        try {
            val supported = listOf(
                "logo.png", "photo.JPG", "photo.jpeg", "banner.webp",
                "animation.gif", "bitmap.bmp", "vector.svg", "photo.heic",
                "photo.heif", "modern.avif"
            )
            supported.forEach { name ->
                val file = java.io.File(root, name).apply { writeBytes(byteArrayOf(1)) }
                assertTrue("$name should be previewable on API 35", ProjectImageFiles.isPreviewable(file, 35))
            }

            val unsupported = listOf("index.html", "notes.txt", "archive.tiff", "icon.ico")
            unsupported.forEach { name ->
                val file = java.io.File(root, name).apply { writeBytes(byteArrayOf(1)) }
                assertFalse("$name must not be routed to the image viewer", ProjectImageFiles.isPreviewable(file, 35))
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun gatesModernFormatsByAndroidDecoderAvailability() {
        val root = Files.createTempDirectory("novarun-image-api-gates").toFile()
        try {
            val heic = java.io.File(root, "photo.heic").apply { writeBytes(byteArrayOf(1)) }
            val avif = java.io.File(root, "photo.avif").apply { writeBytes(byteArrayOf(1)) }
            assertFalse(ProjectImageFiles.isPreviewable(heic, 27))
            assertTrue(ProjectImageFiles.isPreviewable(heic, 29))
            assertFalse(ProjectImageFiles.isPreviewable(avif, 30))
            assertTrue(ProjectImageFiles.isPreviewable(avif, 31))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectsDirectoriesEvenWhenTheirNamesLookLikeImages() {
        val root = Files.createTempDirectory("novarun-image-directory").toFile()
        try {
            val fakeImageDirectory = java.io.File(root, "photos.png").apply { mkdirs() }
            assertFalse(ProjectImageFiles.isPreviewable(fakeImageDirectory, 35))
        } finally {
            root.deleteRecursively()
        }
    }
}
