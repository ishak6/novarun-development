package com.mangalocalrunnerplus

/**
 * Caps runtime console output while retaining a clear indication that text was
 * truncated. The marker is included inside the configured maximum length.
 */
internal class BoundedRuntimeOutput(
    private val maxChars: Int = 48_000
) {
    private val content = StringBuilder()
    private var truncated = false

    init {
        require(maxChars >= TRUNCATION_MARKER.length) {
            "Output limit must be large enough for the truncation marker."
        }
    }

    fun appendLine(value: String) {
        if (truncated) return
        val line = value + System.lineSeparator()
        val contentLimit = maxChars - TRUNCATION_MARKER.length
        val remaining = contentLimit - content.length
        if (line.length <= remaining) {
            content.append(line)
        } else {
            if (remaining > 0) content.append(line.take(remaining))
            truncated = true
        }
    }

    fun render(): String =
        if (truncated) content.toString() + TRUNCATION_MARKER else content.toString()

    companion object {
        private const val TRUNCATION_MARKER = "\n[Output truncated at configured limit.]"
    }
}
