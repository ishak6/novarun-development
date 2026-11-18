package com.mangalocalrunnerplus

import java.io.File
import java.io.IOException
import java.nio.file.Files

object ProjectFileNamePolicy {
    fun validate(rawName: String): String {
        val name = rawName.trim()
        require(name.isNotEmpty()) { "Name cannot be empty." }
        require(name.length <= 255) { "File or folder name must be 255 characters or fewer." }
        require(name != "." && name != "..") { "Dot path segments are not valid names." }
        require(name.none { Character.isISOControl(it) || it in "/\\:*?\"<>|" }) {
            "Name contains a path separator or unsupported character."
        }
        return name
    }
}

object ProjectFileOperations {
    fun list(projectRoot: File, directory: File): List<File> {
        val safeDirectory = requireInsideProject(projectRoot, directory)
        if (!safeDirectory.exists() || !safeDirectory.isDirectory) {
            throw IOException("This folder is not available.")
        }
        return safeDirectory.listFiles()
            ?.filter { child ->
                !Files.isSymbolicLink(child.toPath()) &&
                    runCatching { requireInsideProject(projectRoot, child); true }.getOrDefault(false)
            }
            ?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
            ?: emptyList()
    }

    fun createFile(projectRoot: File, directory: File, rawName: String): File {
        val safeDirectory = requireInsideProject(projectRoot, directory)
        val name = ProjectFileNamePolicy.validate(rawName)
        val target = requireInsideProject(projectRoot, File(safeDirectory, name))
        if (target.exists()) throw IOException("A file or folder with this name already exists.")
        if (!target.createNewFile()) throw IOException("Could not create the file.")
        return target
    }

    fun createFolder(projectRoot: File, directory: File, rawName: String): File {
        val safeDirectory = requireInsideProject(projectRoot, directory)
        val name = ProjectFileNamePolicy.validate(rawName)
        val target = requireInsideProject(projectRoot, File(safeDirectory, name))
        if (target.exists()) throw IOException("A file or folder with this name already exists.")
        if (!target.mkdir()) throw IOException("Could not create the folder.")
        return target
    }

    fun rename(projectRoot: File, target: File, rawName: String): File {
        val safeTarget = requireInsideProject(projectRoot, target)
        val name = ProjectFileNamePolicy.validate(rawName)
        val parent = safeTarget.parentFile ?: throw IOException("Item has no parent directory.")
        val renamed = requireInsideProject(projectRoot, File(parent, name))
        if (renamed.exists()) throw IOException("A file or folder with this name already exists.")
        if (!safeTarget.renameTo(renamed)) throw IOException("Could not rename this item.")
        return renamed
    }

    fun delete(projectRoot: File, target: File) {
        val root = projectRoot.canonicalFile
        val safeTarget = requireInsideProject(root, target)
        if (safeTarget.canonicalFile == root) {
            throw IOException("The project root cannot be deleted from File Explorer.")
        }
        if (!SafeFileTree.deleteRecursivelyNoFollow(safeTarget)) throw IOException("Could not delete this item.")
    }

    private fun requireInsideProject(projectRoot: File, candidate: File): File {
        if (Files.isSymbolicLink(projectRoot.toPath())) {
            throw IOException("Project root cannot be a symbolic link.")
        }
        val root = projectRoot.canonicalFile
        if (!root.isDirectory) throw IOException("Project root is not available.")

        var cursor: File? = candidate.absoluteFile
        var reachedRoot = false
        while (cursor != null) {
            if (Files.isSymbolicLink(cursor.toPath())) {
                throw IOException("Symbolic links are not allowed in File Explorer.")
            }
            if (cursor.canonicalFile == root) {
                reachedRoot = true
                break
            }
            cursor = cursor.parentFile
        }
        if (!reachedRoot) throw IOException("Item is outside the selected project.")
        val canonical = candidate.canonicalFile
        if (canonical != root && !canonical.toPath().startsWith(root.toPath())) {
            throw IOException("Item is outside the selected project.")
        }
        return candidate
    }
}
