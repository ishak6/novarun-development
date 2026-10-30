
package com.mangalocalrunnerplus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import java.io.File

private val EditorBackground = Color(0xFF090B10)
private val EditorCard = Color(0xFF171D28)
private val EditorBlue = Color(0xFF0A84FF)
private val EditorMuted = Color(0xFF9AA6B5)

@Composable
fun CodeEditorScreen(
    file: File,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var code by remember(file.absolutePath) {
        mutableStateOf(
            try {
                if (file.exists() && file.isFile && file.length() <= 2_000_000L) {
                    file.readText()
                } else {
                    ""
                }
            } catch (exception: Exception) {
                ""
            }
        )
    }

    var savedCode by remember(file.absolutePath) {
        mutableStateOf(code)
    }

    var message by remember {
        mutableStateOf<String?>(null)
    }

    var saveError by remember {
        mutableStateOf(false)
    }

    val isModified = code != savedCode

    val lineCount = remember(code) {
        code.count { it == '\n' } + 1
    }

    val extension = file.extension.lowercase()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditorBackground)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Column(Modifier.weight(1f)) {
                Text(
                    file.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    if (isModified) "Unsaved changes" else "Saved",
                    color = if (isModified) {
                        Color(0xFFFF9F0A)
                    } else {
                        Color(0xFF30D158)
                    },
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = {
                    try {
                        if (!file.exists() || !file.isFile) {
                            throw IllegalStateException(
                                "File is unavailable."
                            )
                        }

                        file.writeText(code)
                        savedCode = code
                        saveError = false
                        message = "File saved successfully."
                    } catch (exception: Exception) {
                        saveError = true
                        message = exception.message
                            ?: "Could not save file."
                    }
                },
                enabled = isModified,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EditorBlue
                )
            ) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = null
                )

                Spacer(Modifier.width(6.dp))

                Text("Save")
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "SOURCE CODE · ${extension.uppercase().ifEmpty { "TEXT" }}",
                color = EditorMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "$lineCount lines",
                color = EditorMuted,
                fontSize = 10.sp
            )
        }

        Spacer(Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = EditorCard
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .width(38.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.End
                ) {
                    repeat(lineCount) { index ->
                        Text(
                            text = "${index + 1}",
                            color = EditorMuted,
                            fontSize = 12.sp,
                            lineHeight = 19.sp
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                SyntaxCodeField(
                    code = code,
                    onCodeChange = { code = it },
                    extension = extension,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }

    if (message != null) {
        AlertDialog(
            onDismissRequest = { message = null },
            containerColor = EditorCard,
            title = {
                Text(
                    if (saveError) "Save Failed" else "Save Complete",
                    color = Color.White
                )
            },
            text = {
                Text(
                    message ?: "",
                    color = EditorMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { message = null }
                ) {
                    Text("OK", color = EditorBlue)
                }
            }
        )
    }
}
