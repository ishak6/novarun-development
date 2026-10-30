
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
    modifier: Modifier = Modifier
) {
    val highlighted = remember(code, extension) {
        CodeSyntaxHighlighter.highlight(code, extension)
    }

    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    val editorTextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        lineHeight = 19.sp
    )

    Box(
        modifier = modifier
            .background(
                Color(0xFF111722),
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
                .horizontalScroll(horizontalScroll),
            textStyle = editorTextStyle.copy(
                color = Color.Transparent
            ),
            cursorBrush = SolidColor(Color(0xFF0A84FF)),
            decorationBox = { innerTextField ->
                Box {
                    Text(
                        text = highlighted,
                        style = editorTextStyle
                    )

                    if (code.isEmpty()) {
                        Text(
                            text = "Write your code here...",
                            style = editorTextStyle.copy(
                                color = Color(0xFF9AA6B5)
                            )
                        )
                    }

                    innerTextField()
                }
            }
        )
    }
}
