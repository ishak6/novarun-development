
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun CodeEditorScreen(
    file: File,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
                if (
                    file.exists() &&
                    file.isFile &&
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

    var isSaving by remember {
        mutableStateOf(false)
    }

    var showLeaveDialog by remember {
        mutableStateOf(false)
    }

    val isModified = code != savedCode
    val extension = file.extension.lowercase()

    val lineCount = remember(code) {
        code.count { it == '\n' } + 1
    }

    suspend fun saveSnapshot(snapshot: String): Boolean {
        if (isSaving) return false

        isSaving = true

        return try {
            withContext(Dispatchers.IO) {
                if (!file.exists() || !file.isFile) {
                    throw IllegalStateException(
                        "File is unavailable. It may have been deleted or moved."
                    )
                }

                if (file.length() > 2_000_000L) {
                    throw IllegalStateException(
                        "File is larger than the supported 2 MB limit."
                    )
                }

                val parent = file.parentFile
                    ?: throw IllegalStateException(
                        "File location is unavailable."
                    )

                if (!parent.exists() || !parent.isDirectory) {
                    throw IllegalStateException(
                        "Parent directory is unavailable."
                    )
                }

                file.writeText(snapshot)
            }

            savedCode = snapshot
            saveError = false
            true

        } catch (exception: CancellationException) {
            throw exception

        } catch (exception: Exception) {
            saveError = true
            message = exception.message ?: "Could not save file."
            false

        } finally {
            isSaving = false
        }
    }

    LaunchedEffect(code, autoSave, file.absolutePath) {
        if (autoSave && code != savedCode) {
            delay(700)

            val snapshot = code
            val success = saveSnapshot(snapshot)

            if (success && code != snapshot) {
                // New edits arrived while saving.
                // The next effect saves the newer content.
            }
        }
    }

    fun requestBack() {
        if (isModified) {
            showLeaveDialog = true
        } else {
            onBack()
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
            IconButton(onClick = { requestBack() }) {
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
                        isSaving -> "Saving..."
                        saveError -> "Save failed"
                        isModified && autoSave -> "Auto-saving..."
                        isModified -> "Unsaved changes"
                        else -> "Saved"
                    },
                    color = when {
                        saveError -> NovaRunPalette.Error
                        isModified || isSaving -> NovaRunPalette.Warning
                        else -> NovaRunPalette.Success
                    },
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = {
                    val snapshot = code
                    scope.launch {
                        if (saveSnapshot(snapshot)) {
                            message = "File saved successfully."
                        }
                    }
                },
                enabled = isModified && !isSaving,
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

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = {
                showLeaveDialog = false
            },
            containerColor = NovaRunPalette.Surface,
            title = {
                Text(
                    "Unsaved Changes",
                    color = NovaRunPalette.TextPrimary
                )
            },
            text = {
                Text(
                    "You have unsaved changes. Save before leaving?",
                    color = NovaRunPalette.TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        val snapshot = code
                        scope.launch {
                            if (saveSnapshot(snapshot)) {
                                showLeaveDialog = false
                                onBack()
                            }
                        }
                    }
                ) {
                    Text("Save & Leave", color = NovaRunPalette.Cyan)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showLeaveDialog = false
                            onBack()
                        }
                    ) {
                        Text(
                            "Discard",
                            color = NovaRunPalette.Warning
                        )
                    }

                    TextButton(
                        onClick = {
                            showLeaveDialog = false
                        }
                    ) {
                        Text("Cancel", color = NovaRunPalette.TextSecondary)
                    }
                }
            }
        )
    }

    if (message != null) {
        AlertDialog(
            onDismissRequest = {
                message = null
            },
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
                TextButton(
                    onClick = {
                        message = null
                    }
                ) {
                    Text("OK", color = NovaRunPalette.Cyan)
                }
            }
        )
    }
}
