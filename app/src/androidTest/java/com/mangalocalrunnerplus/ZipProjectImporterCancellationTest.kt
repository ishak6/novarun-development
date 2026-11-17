package com.mangalocalrunnerplus

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.concurrent.CancellationException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ZipProjectImporterCancellationTest {

    @Test
    fun cancellationDuringExtractionRemovesTemporaryProjectDirectory() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val projectsDirectory = File(context.filesDir, "projects").apply {
            check(isDirectory || mkdirs())
        }
        val before = projectsDirectory.listFiles()?.map { it.name }?.toSet().orEmpty()
        val archive = createArchive()
        val cancellationMessage = "cancelled by instrumentation test"
        var cancellationChecks = 0

        try {
            ZipProjectImporter.importZip(
                context = context,
                input = ByteArrayInputStream(archive),
                displayName = "cancel-${UUID.randomUUID()}.zip",
                limits = ZipImportLimits(),
                cancellationCheck = {
                    cancellationChecks += 1
                    if (cancellationChecks == 4) {
                        throw CancellationException(cancellationMessage)
                    }
                }
            )
            fail("Expected ZIP extraction to be cancelled.")
        } catch (cancelled: CancellationException) {
            assertEquals(cancellationMessage, cancelled.message)
        }

        assertTrue("Cancellation must happen after extraction has started.", cancellationChecks >= 4)
        val after = projectsDirectory.listFiles()?.map { it.name }?.toSet().orEmpty()
        assertEquals("Cancelled import must not leave temporary or final project directories.", before, after)
    }

    private fun createArchive(): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            zip.putNextEntry(ZipEntry("store/index.html"))
            zip.write(ByteArray(64 * 1024) { 'A'.code.toByte() })
            zip.closeEntry()
        }
        return bytes.toByteArray()
    }
}
