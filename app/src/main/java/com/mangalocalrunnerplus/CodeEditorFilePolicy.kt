package com.mangalocalrunnerplus

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

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

    /**
     * Saves an editor snapshot atomically and refuses paths or symlinks that
     * escape the selected project. The target must already exist as a regular file.
     */
    @Throws(IOException::class)
    fun save(projectRoot: File, target: File, content: String) {
        val rootPath = projectRoot.toPath().toAbsolutePath().normalize()
        if (Files.isSymbolicLink(rootPath) || !Files.isDirectory(rootPath)) {
            throw IOException("Project root is not a safe, available directory.")
        }
        val root = rootPath.toRealPath()
        val targetPath = target.toPath().toAbsolutePath().normalize()
        if (!targetPath.startsWith(root)) {
            throw IOException("File is outside the selected project.")
        }

        var cursor = rootPath
        val relative = rootPath.relativize(targetPath)
        for (part in relative) {
            cursor = cursor.resolve(part)
            if (Files.isSymbolicLink(cursor)) {
                throw IOException("Symbolic links cannot be edited.")
            }
        }

        val parent = targetPath.parent ?: throw IOException("File location is unavailable.")
        if (!Files.isDirectory(parent) || !parent.toRealPath().startsWith(root)) {
            throw IOException("Parent directory is outside the selected project or unavailable.")
        }
        if (!Files.isRegularFile(targetPath) || !Files.isWritable(targetPath)) {
            throw IOException("File is unavailable or read-only.")
        }
        if (Files.size(targetPath) > MAX_FILE_BYTES) {
            throw IOException("File is larger than the supported 2 MB limit.")
        }

        val bytes = encodeSnapshot(content)
        val temp = parent.resolve(".${targetPath.fileName}.${UUID.randomUUID()}.tmp")
        try {
            FileOutputStream(temp.toFile()).use { output ->
                output.write(bytes)
                output.fd.sync()
            }
            try {
                Files.move(temp, targetPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temp, targetPath, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temp)
        }
    }
}
