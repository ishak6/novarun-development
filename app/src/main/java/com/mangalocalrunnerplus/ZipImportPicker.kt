package com.mangalocalrunnerplus

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun rememberZipImportLauncher(
    onResult: (Result<File>) -> Unit
): () -> Unit {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var importJob by remember { mutableStateOf<Job?>(null) }
    var isImporting by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->

        if (uri != null && importJob?.isActive != true) {
            importJob = scope.launch {
                isImporting = true
                val importContext = currentCoroutineContext()
                try {
                    val result = try {
                        val project = withContext(Dispatchers.IO) {
                            ZipProjectImporter.importZip(context, uri) {
                                importContext.ensureActive()
                            }
                        }
                        Result.success(project)
                    } catch (cancelled: CancellationException) {
                        // Never turn cancellation into a user-visible import failure.
                        throw cancelled
                    } catch (error: Exception) {
                        Result.failure(error)
                    }
                    onResult(result)
                } finally {
                    isImporting = false
                    importJob = null
                }
            }
        }
    }

    if (isImporting) {
        AlertDialog(
            onDismissRequest = { importJob?.cancel() },
            title = { Text("Importing ZIP project") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.width(16.dp))
                    Text("Extracting files safely. You can cancel; incomplete files will be removed.")
                }
            },
            confirmButton = {
                TextButton(onClick = { importJob?.cancel() }) {
                    Text("Cancel import")
                }
            }
        )
    }

    return remember(launcher, isImporting) {
        {
            if (!isImporting) {
                launcher.launch(
                    arrayOf(
                        "application/zip",
                        "application/x-zip-compressed",
                        "application/octet-stream"
                    )
                )
            }
        }
    }
}
