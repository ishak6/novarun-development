package com.mangalocalrunnerplus

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeEditorFilePolicyTest {
    @Test
    fun loadsUtf8SourceWithinLimit() {
        val file = File.createTempFile("novarun-editor-", ".txt")
        try {
            file.writeText("বাংলা source\nhello", Charsets.UTF_8)
            val result = CodeEditorFilePolicy.load(file)
            assertTrue(result.isSuccess)
            assertEquals("বাংলা source\nhello", result.getOrThrow())
        } finally {
            file.delete()
        }
    }

    @Test
    fun rejectsBinaryAssetsInsteadOfOpeningThemAsEditableText() {
        val file = File.createTempFile("novarun-editor-image-", ".png")
        try {
            file.writeBytes(byteArrayOf(0, 1, 2, 3))
            assertTrue(CodeEditorFilePolicy.load(file).isFailure)
        } finally {
            file.delete()
        }
    }

    @Test
    fun acceptsCommonWebAndConfigurationFiles() {
        val html = File.createTempFile("novarun-editor-", ".html")
        val config = File.createTempFile("novarun-editor-", ".json")
        try {
            html.writeText("<html></html>")
            config.writeText("{}")
            assertTrue(CodeEditorFilePolicy.load(html).isSuccess)
            assertTrue(CodeEditorFilePolicy.load(config).isSuccess)
        } finally {
            html.delete()
            config.delete()
        }
    }

    @Test
    fun rejectsOversizedFileBeforeReading() {
        val file = File.createTempFile("novarun-editor-large-", ".txt")
        try {
            file.writeBytes(ByteArray((CodeEditorFilePolicy.MAX_FILE_BYTES + 1).toInt()))
            assertTrue(CodeEditorFilePolicy.load(file).isFailure)
        } finally {
            file.delete()
        }
    }

    @Test
    fun rejectsOversizedUtf8SnapshotByEncodedByteCount() {
        val error = runCatching {
            CodeEditorFilePolicy.encodeSnapshot("é".repeat(1_000_001))
        }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun encodesValidUtf8Snapshot() {
        val bytes = CodeEditorFilePolicy.encodeSnapshot("বাংলা")
        assertFalse(bytes.isEmpty())
        assertEquals("বাংলা", bytes.toString(Charsets.UTF_8))
    }
}
