
package com.mangalocalrunnerplus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SyntaxCodeField(
    code: String,
    onCodeChange: (String) -> Unit,
    extension: String,
    modifier: Modifier = Modifier,
    fontSizeSp: Int = 14,
    wordWrap: Boolean = true
) {
    val highlighted = remember(code, extension) {
        CodeSyntaxHighlighter.highlight(code, extension)
    }

    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    val editorTextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = fontSizeSp.coerceIn(10, 24).sp,
        lineHeight = (fontSizeSp.coerceIn(10, 24) + 7).sp
    )

    Box(
        modifier = modifier
            .background(
                NovaRunPalette.Background,
                RoundedCornerShape(10.dp)
            )
            .padding(8.dp)
    ) {
        BasicTextField(
            value = code,
            onValueChange = onCodeChange,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScroll)
                .then(
                    if (wordWrap) {
                        Modifier
                    } else {
                        Modifier.horizontalScroll(horizontalScroll)
                    }
                ),
            textStyle = editorTextStyle.copy(
                color = Color.Transparent
            ),
            cursorBrush = SolidColor(NovaRunPalette.Cyan),
            softWrap = wordWrap,
            decorationBox = { innerTextField ->
                Box {
                    Text(
                        text = highlighted,
                        style = editorTextStyle.copy(
                            color = NovaRunPalette.TextPrimary
                        )
                    )

                    if (code.isEmpty()) {
                        Text(
                            text = "Write your code here...",
                            style = editorTextStyle.copy(
                                color = NovaRunPalette.TextSecondary
                            )
                        )
                    }

                    innerTextField()
                }
            }
        )
    }
}
