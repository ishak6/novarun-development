package com.mangalocalrunnerplus

import java.nio.file.Files
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectImagePreviewTest {
    @Test
    fun recognizesSupportedRasterImagesButNotVectorOrTextFiles() {
        val root = Files.createTempDirectory("novarun-image-types").toFile()
        try {
            val png = java.io.File(root, "logo.png").apply { writeBytes(byteArrayOf(1)) }
            val jpg = java.io.File(root, "photo.JPG").apply { writeBytes(byteArrayOf(1)) }
            val webp = java.io.File(root, "banner.webp").apply { writeBytes(byteArrayOf(1)) }
            val gif = java.io.File(root, "animation.gif").apply { writeBytes(byteArrayOf(1)) }
            val svg = java.io.File(root, "vector.svg").apply { writeText("<svg/>") }
            val html = java.io.File(root, "index.html").apply { writeText("<html/>") }

            assertTrue(ProjectImageFiles.isPreviewable(png))
            assertTrue(ProjectImageFiles.isPreviewable(jpg))
            assertTrue(ProjectImageFiles.isPreviewable(webp))
            assertTrue(ProjectImageFiles.isPreviewable(gif))
            assertFalse(ProjectImageFiles.isPreviewable(svg))
            assertFalse(ProjectImageFiles.isPreviewable(html))
        } finally {
            root.deleteRecursively()
        }
    }
}
