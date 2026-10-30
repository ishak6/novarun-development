
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    color = if (isModified) Color(0xFFFF9F0A) else Color(0xFF30D158),
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = {
                    try {
                        if (!file.exists() || !file.isFile) {
                            throw IllegalStateException("File is unavailable.")
                        }

                        file.writeText(code)
                        savedCode = code
                        saveError = false
                        message = "File saved successfully."
                    } catch (exception: Exception) {
                        saveError = true
                        message = exception.message ?: "Could not save file."
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

        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = EditorCard
            )
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    "SOURCE CODE",
                    color = EditorMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    textStyle = LocalTextStyle.current.copy(
                        color = Color(0xFFE6EDF3),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    ),
                    placeholder = {
                        Text(
                            "Write your code here...",
                            color = EditorMuted
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EditorBlue,
                        unfocusedBorderColor = Color(0xFF303A49),
                        cursorColor = EditorBlue,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
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
                Text(message ?: "", color = EditorMuted)
            },
            confirmButton = {
                TextButton(onClick = { message = null }) {
                    Text("OK", color = EditorBlue)
                }
            }
        )
    }
}
