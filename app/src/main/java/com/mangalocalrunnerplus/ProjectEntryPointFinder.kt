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
    private val supportedEntryNames = listOf("index.html", "index.htm", "default.html", "home.html", "main.html", "start.html")

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
                    if (child.name.lowercase() in supportedEntryNames &&
                        canonicalChild.canRead()
                    ) {
                        candidates += canonicalChild
                    }
                }
            }
        }

        return candidates.minWithOrNull(
            compareBy<File> { candidatePriority(root, it) }
                .thenBy { supportedEntryNames.indexOf(it.name.lowercase()).let { rank -> if (rank < 0) Int.MAX_VALUE else rank } }
                .thenBy { it.relativeTo(root).invariantSeparatorsPath.split('/').size }
                .thenBy { it.relativeTo(root).invariantSeparatorsPath.lowercase() }
        )
    }

    /**
     * Build output folders can be nested under arbitrary ZIP wrapper folders
     * (for example download/site/dist/index.html). Rank the nearest matching
     * output ancestor, case-insensitively, rather than checking only path[0].
     */
    private fun candidatePriority(root: File, entry: File): Int {
        val segments = entry.relativeTo(root).invariantSeparatorsPath
            .split('/')
            .dropLast(1)
            .map { it.lowercase() }
        val outputIndex = segments.indexOfLast { it in preferredOutputDirectories }
        if (outputIndex >= 0) {
            return preferredOutputDirectories.indexOf(segments[outputIndex])
        }
        return if (entry.parentFile == root) 3 else 4
    }

    private const val MAX_DEPTH = 8
    private const val MAX_FILES = 10000
}
