
package com.mangalocalrunnerplus

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

object CodeSyntaxHighlighter {

    private val KeywordColor = Color(0xFFC792EA)
    private val StringColor = Color(0xFFC3E88D)
    private val NumberColor = Color(0xFFF78C6C)
    private val CommentColor = Color(0xFF697A91)
    private val TagColor = Color(0xFFF07178)
    private val AttributeColor = Color(0xFFFFCB6B)
    private val DefaultColor = Color(0xFFE6EDF3)

    private val keywords = setOf(
        "fun", "val", "var", "class", "object",
        "if", "else", "when", "for", "while",
        "return", "import", "package", "public",
        "private", "protected", "static", "void",
        "int", "float", "double", "boolean",
        "const", "let", "function", "async",
        "await", "new", "this", "true", "false",
        "null", "def", "print", "from", "as",
        "try", "catch", "throw", "extends"
    )

    fun highlight(
        source: String,
        extension: String
    ): AnnotatedString {

        val ext = extension.lowercase().removePrefix(".")

        return buildAnnotatedString {
            append(source)

            addStyle(
                SpanStyle(color = DefaultColor),
                0,
                source.length
            )

            if (source.isEmpty()) return@buildAnnotatedString

            when (ext) {
                "html", "htm", "xml" -> highlightHtml(source)
                "css", "scss" -> highlightCss(source)
                "js", "jsx", "ts", "tsx" -> highlightScript(source)
                "kt", "kts", "java" -> highlightScript(source)
                "py", "rb", "sh" -> highlightScript(source)
                "json" -> highlightJson(source)
            }
        }
    }

    private fun AnnotatedString.Builder.highlightHtml(source: String) {
        val tagRegex = Regex("</?\\s*[A-Za-z][A-Za-z0-9:-]*")
        val attributeRegex = Regex("\\b[A-Za-z_:][-A-Za-z0-9_:.]*(?=\\s*=)")

        tagRegex.findAll(source).forEach {
            addStyle(
                SpanStyle(color = TagColor, fontWeight = FontWeight.Bold),
                it.range.first,
                it.range.last + 1
            )
        }

        attributeRegex.findAll(source).forEach {
            addStyle(
                SpanStyle(color = AttributeColor),
                it.range.first,
                it.range.last + 1
            )
        }

        highlightStrings(source)
    }

    private fun AnnotatedString.Builder.highlightCss(source: String) {
        val propertyRegex = Regex("[A-Za-z-]+(?=\\s*:)")
        val selectorRegex = Regex("[.#][A-Za-z_-][A-Za-z0-9_-]*")

        propertyRegex.findAll(source).forEach {
            addStyle(
                SpanStyle(color = AttributeColor),
                it.range.first,
                it.range.last + 1
            )
        }

        selectorRegex.findAll(source).forEach {
            addStyle(
                SpanStyle(color = TagColor),
                it.range.first,
                it.range.last + 1
            )
        }

        highlightNumbers(source)
        highlightComments(source, "/*", "*/")
    }

    private fun AnnotatedString.Builder.highlightScript(source: String) {
        val identifierRegex = Regex("\\b[A-Za-z_][A-Za-z0-9_]*\\b")

        identifierRegex.findAll(source).forEach {
            if (it.value in keywords) {
                addStyle(
                    SpanStyle(
                        color = KeywordColor,
                        fontWeight = FontWeight.Bold
                    ),
                    it.range.first,
                    it.range.last + 1
                )
            }
        }

        highlightStrings(source)
        highlightNumbers(source)
        highlightComments(source, "/*", "*/")
        highlightLineComments(source)
    }

    private fun AnnotatedString.Builder.highlightJson(source: String) {
        val keyRegex = Regex("\"[^\"]+\"(?=\\s*:)")

        keyRegex.findAll(source).forEach {
            addStyle(
                SpanStyle(color = AttributeColor),
                it.range.first,
                it.range.last + 1
            )
        }

        highlightStrings(source)
        highlightNumbers(source)
    }

    private fun AnnotatedString.Builder.highlightStrings(source: String) {
        val regex = Regex("\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'")

        regex.findAll(source).forEach {
            addStyle(
                SpanStyle(color = StringColor),
                it.range.first,
                it.range.last + 1
            )
        }
    }

    private fun AnnotatedString.Builder.highlightNumbers(source: String) {
        val regex = Regex("\\b\\d+(\\.\\d+)?\\b")

        regex.findAll(source).forEach {
            addStyle(
                SpanStyle(color = NumberColor),
                it.range.first,
                it.range.last + 1
            )
        }
    }

    private fun AnnotatedString.Builder.highlightComments(
        source: String,
        startMarker: String,
        endMarker: String
    ) {
        var position = 0

        while (true) {
            val start = source.indexOf(startMarker, position)
            if (start < 0) break

            val endPosition = source.indexOf(
                endMarker,
                start + startMarker.length
            )

            val end = if (endPosition < 0) {
                source.length
            } else {
                endPosition + endMarker.length
            }

            addStyle(
                SpanStyle(color = CommentColor),
                start,
                end
            )

            position = end
        }
    }

    private fun AnnotatedString.Builder.highlightLineComments(source: String) {
        source.lines().forEachIndexed { lineIndex, line ->
            val start = line.indexOf("//")
            if (start >= 0) {
                val absoluteStart =
                    source.lines().take(lineIndex).sumOf { it.length + 1 } + start

                val absoluteEnd = absoluteStart +
                    line.length - start

                addStyle(
                    SpanStyle(color = CommentColor),
                    absoluteStart,
                    absoluteEnd
                )
            }
        }
    }
}
