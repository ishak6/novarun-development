package com.mangalocalrunnerplus

import java.io.File

object CodeEditorFilePolicy {
    const val MAX_FILE_BYTES = 2_000_000L

    fun load(file: File): Result<String> = runCatching {
        if (!file.exists() || !file.isFile) {
            throw IllegalStateException("File is not available.")
        }
        if (file.length() > MAX_FILE_BYTES) {
            throw IllegalStateException(
                "File is larger than the supported 2 MB editor limit. Open it in a lighter external tool or split the file."
            )
        }
        file.readText(Charsets.UTF_8)
    }

    fun encodeSnapshot(content: String): ByteArray {
        val bytes = content.toByteArray(Charsets.UTF_8)
        require(bytes.size.toLong() <= MAX_FILE_BYTES) {
            "Edited content exceeds the 2 MB editor limit."
        }
        return bytes
    }
}
