package com.mangalocalrunnerplus

import java.io.File

/**
 * Finds a runnable static website entry point without assuming the ZIP has a
 * conventional root layout. This is inspection only; it never executes code.
 */
object ProjectEntryPointFinder {
    private val ignoredDirectories = setOf(
        ".git", ".gradle", ".idea", "node_modules", "vendor",
        ".dart_tool", "__pycache__", ".next", ".nuxt", ".cache"
    )
    private val preferredOutputDirectories = listOf("dist", "build", "out")

    fun find(projectDirectory: File): File? {
        val root = runCatching { projectDirectory.canonicalFile }.getOrNull()
            ?: return null
        if (!root.isDirectory || !root.canRead()) return null

        val rootPrefix = root.path + File.separator
        val candidates = mutableListOf<File>()
        val stack = ArrayDeque<Pair<File, Int>>()
        stack.add(root to 0)
        var visitedFiles = 0
        val visitedDirectories = HashSet<String>()

        while (stack.isNotEmpty() && visitedFiles < MAX_FILES) {
            val (directory, depth) = stack.removeLast()
            if (depth > MAX_DEPTH) continue
            val canonicalDirectory = runCatching { directory.canonicalFile }.getOrNull()
                ?: continue
            if (canonicalDirectory != root && !canonicalDirectory.path.startsWith(rootPrefix)) continue
            if (!visitedDirectories.add(canonicalDirectory.path)) continue

            val children = canonicalDirectory.listFiles()
                ?.sortedBy { it.name.lowercase() }
                ?: continue

            for (child in children) {
                if (visitedFiles >= MAX_FILES) break
                val canonicalChild = runCatching { child.canonicalFile }.getOrNull()
                    ?: continue
                if (canonicalChild != root && !canonicalChild.path.startsWith(rootPrefix)) continue

                if (child.isDirectory) {
                    if (child.name.lowercase() !in ignoredDirectories) {
                        stack.add(canonicalChild to depth + 1)
                    }
                } else if (canonicalChild.isFile) {
                    visitedFiles++
                    if (child.name.equals("index.html", ignoreCase = true) &&
                        canonicalChild.canRead()
                    ) {
                        candidates += canonicalChild
                    }
                }
            }
        }

        return candidates.minWithOrNull(
            compareBy<File> { candidatePriority(root, it) }
                .thenBy { it.relativeTo(root).invariantSeparatorsPath.split('/').size }
                .thenBy { it.relativeTo(root).invariantSeparatorsPath.lowercase() }
        )
    }

    private fun candidatePriority(root: File, entry: File): Int {
        val relative = entry.relativeTo(root).invariantSeparatorsPath
        val firstSegment = relative.substringBefore('/')
        return when {
            firstSegment in preferredOutputDirectories -> preferredOutputDirectories.indexOf(firstSegment)
            entry.parentFile == root -> 3
            else -> 4
        }
    }

    private const val MAX_DEPTH = 8
    private const val MAX_FILES = 10000
}
