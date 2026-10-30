
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun CodeEditorScreen(
    file: File,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val prefs = remember(context) {
        context.getSharedPreferences("novarun_settings", 0)
    }

    var autoSave by remember {
        mutableStateOf(prefs.getBoolean("auto_save", true))
    }

    var wordWrap by remember {
        mutableStateOf(prefs.getBoolean("word_wrap", true))
    }

    var fontSize by remember {
        mutableIntStateOf(
            prefs.getInt("font_size", 14).coerceIn(10, 24)
        )
    }

    DisposableEffect(prefs) {
        val listener =
            android.content.SharedPreferences.OnSharedPreferenceChangeListener {
                    sharedPreferences, key ->

                when (key) {
                    "auto_save" -> {
                        autoSave = sharedPreferences
                            ?.getBoolean("auto_save", true) ?: true
                    }

                    "word_wrap" -> {
                        wordWrap = sharedPreferences
                            ?.getBoolean("word_wrap", true) ?: true
                    }

                    "font_size" -> {
                        fontSize = sharedPreferences
                            ?.getInt("font_size", 14)
                            ?.coerceIn(10, 24) ?: 14
                    }
                }
            }

        prefs.registerOnSharedPreferenceChangeListener(listener)

        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    var code by remember(file.absolutePath) {
        mutableStateOf(
            try {
                if (file.exists() && file.isFile &&
                    file.length() <= 2_000_000L
                ) {
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
    val extension = file.extension.lowercase()

    val lineCount = remember(code) {
        code.count { it == '\n' } + 1
    }

    fun saveFile(): Boolean {
        return try {
            if (!file.exists() || !file.isFile) {
                throw IllegalStateException("File is unavailable.")
            }

            file.writeText(code)
            savedCode = code
            saveError = false
            true
        } catch (exception: Exception) {
            saveError = true
            message = exception.message ?: "Could not save file."
            false
        }
    }

    LaunchedEffect(code, autoSave, file.absolutePath) {
        if (autoSave && code != savedCode) {
            delay(700)

            withContext(Dispatchers.IO) {
                if (file.exists() && file.isFile) {
                    file.writeText(code)
                } else {
                    throw IllegalStateException("File is unavailable.")
                }
            }

            savedCode = code
            saveError = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NovaRunPalette.Background)
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
                    tint = NovaRunPalette.TextPrimary
                )
            }

            Column(Modifier.weight(1f)) {
                Text(
                    file.name,
                    color = NovaRunPalette.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    when {
                        isModified && autoSave -> "Auto-saving..."
                        isModified -> "Unsaved changes"
                        else -> "Saved"
                    },
                    color = when {
                        isModified -> NovaRunPalette.Warning
                        else -> NovaRunPalette.Success
                    },
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = {
                    if (saveFile()) {
                        message = "File saved successfully."
                    }
                },
                enabled = isModified,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NovaRunPalette.Cyan,
                    contentColor = NovaRunPalette.Background
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
                color = NovaRunPalette.TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "$lineCount lines",
                color = NovaRunPalette.TextSecondary,
                fontSize = 10.sp
            )
        }

        Spacer(Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = NovaRunPalette.Surface
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                NovaRunPalette.Border
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
                            color = NovaRunPalette.TextSecondary,
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize + 7).sp
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                SyntaxCodeField(
                    code = code,
                    onCodeChange = { code = it },
                    extension = extension,
                    fontSizeSp = fontSize,
                    wordWrap = wordWrap,
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
            containerColor = NovaRunPalette.Surface,
            title = {
                Text(
                    if (saveError) "Save Failed" else "Save Complete",
                    color = NovaRunPalette.TextPrimary
                )
            },
            text = {
                Text(
                    message ?: "",
                    color = NovaRunPalette.TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { message = null }) {
                    Text(
                        "OK",
                        color = NovaRunPalette.Cyan
                    )
                }
            }
        )
    }
}
