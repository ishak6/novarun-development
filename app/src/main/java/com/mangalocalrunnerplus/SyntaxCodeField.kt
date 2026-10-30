
package com.mangalocalrunnerplus

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()
    val safeFontSize = fontSizeSp.coerceIn(10, 24)

    val editorTextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = safeFontSize.sp,
        lineHeight = (safeFontSize + 7).sp,
        color = NovaRunPalette.TextPrimary
    )

    Box(
        modifier = modifier
            .background(
                NovaRunPalette.Background,
                RoundedCornerShape(10.dp)
            )
            .padding(4.dp)
    ) {
        TextField(
            value = code,
            onValueChange = onCodeChange,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScroll)
                .then(
                    if (wordWrap) Modifier
                    else Modifier.horizontalScroll(horizontalScroll)
                ),
            textStyle = editorTextStyle,
            placeholder = {
                Text(
                    text = "Write your code here...",
                    style = editorTextStyle.copy(
                        color = NovaRunPalette.TextSecondary
                    )
                )
            },
            maxLines = Int.MAX_VALUE,
            colors = TextFieldDefaults.colors(
                focusedTextColor = NovaRunPalette.TextPrimary,
                unfocusedTextColor = NovaRunPalette.TextPrimary,
                disabledTextColor = NovaRunPalette.TextSecondary,
                cursorColor = NovaRunPalette.Cyan,
                focusedContainerColor = NovaRunPalette.Background,
                unfocusedContainerColor = NovaRunPalette.Background,
                disabledContainerColor = NovaRunPalette.Background,
                focusedIndicatorColor =
                    androidx.compose.ui.graphics.Color.Transparent,
                unfocusedIndicatorColor =
                    androidx.compose.ui.graphics.Color.Transparent,
                disabledIndicatorColor =
                    androidx.compose.ui.graphics.Color.Transparent
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}
