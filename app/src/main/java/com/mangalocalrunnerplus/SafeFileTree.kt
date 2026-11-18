package com.mangalocalrunnerplus

import java.io.File
import java.nio.file.Files

/**
 * Deletes files without following symbolic links. Kotlin File.deleteRecursively()
 * may traverse a linked directory on some runtimes, so project deletion uses
 * this explicit no-follow walk.
 */
object SafeFileTree {
    fun deleteRecursivelyNoFollow(target: File): Boolean {
        if (Files.isSymbolicLink(target.toPath())) {
            return target.delete()
        }
        if (!target.exists()) return true

        if (target.isDirectory) {
            val children = target.listFiles() ?: return false
            for (child in children) {
                if (!deleteRecursivelyNoFollow(child)) return false
            }
        }
        return target.delete()
    }
}
