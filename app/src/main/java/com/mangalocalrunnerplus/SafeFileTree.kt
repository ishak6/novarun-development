package com.mangalocalrunnerplus

import java.io.File
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

/**
 * Deletes files without following symbolic links. walkFileTree's default
 * behavior visits links as files; it never traverses the linked directory.
 */
object SafeFileTree {
    fun deleteRecursivelyNoFollow(target: File): Boolean {
        val root = target.toPath()
        if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) return true

        return try {
            Files.walkFileTree(root, object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attributes: BasicFileAttributes): FileVisitResult {
                    Files.deleteIfExists(file)
                    return FileVisitResult.CONTINUE
                }

                override fun postVisitDirectory(directory: Path, error: java.io.IOException?): FileVisitResult {
                    if (error != null) throw error
                    Files.deleteIfExists(directory)
                    return FileVisitResult.CONTINUE
                }
            })
            true
        } catch (_: Exception) {
            false
        }
    }
}
