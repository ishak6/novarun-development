package com.mangalocalrunnerplus

import java.io.File

object CodeEditorFilePolicy {
    const val MAX_FILE_BYTES = 2_000_000L
    private val supportedExtensions = setOf(
        "html", "htm", "css", "js", "mjs", "cjs", "json", "txt", "xml",
        "py", "md", "markdown", "sql", "csv", "svg", "yml", "yaml",
        "toml", "ini", "properties", "sh", "bash", "gradle", "kt", "kts",
        "java", "c", "h", "cpp", "hpp", "rs", "go", "rb", "php", "log"
    )
    private val supportedNames = setOf(
        "dockerfile", "makefile", "license", "readme", ".gitignore", ".gitattributes",
        ".editorconfig", ".env"
    )

    fun load(file: File): Result<String> = runCatching {
        if (!file.exists() || !file.isFile) {
            throw IllegalStateException("File is not available.")
        }
        if (file.extension.lowercase() !in supportedExtensions &&
            file.name.lowercase() !in supportedNames
        ) {
            throw IllegalStateException("This file type is not supported by the text editor. Binary assets remain available to project preview.")
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
