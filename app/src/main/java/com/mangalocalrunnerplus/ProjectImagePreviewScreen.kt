package com.mangalocalrunnerplus

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ProjectImageFiles {
    private val rasterExtensions = setOf("png", "jpg", "jpeg", "webp", "gif", "bmp")

    fun isPreviewable(file: File): Boolean =
        file.isFile && file.extension.lowercase() in rasterExtensions
}

@Composable
fun ProjectImagePreviewScreen(
    file: File,
    onBack: () -> Unit
) {
    val imageResult by produceState<Result<Bitmap?>?>(null, file.absolutePath, file.length(), file.lastModified()) {
        value = withContext(Dispatchers.IO) {
            runCatching { decodeProjectImage(file) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaRunPalette.Background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Text(
                    "${formatImageSize(file.length())} • ${file.extension.uppercase()}",
                    color = NovaRunPalette.TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NovaRunPalette.Surface),
            contentAlignment = Alignment.Center
        ) {
            when {
                imageResult == null -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = NovaRunPalette.Cyan
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Loading image…", color = NovaRunPalette.TextSecondary, fontSize = 12.sp)
                    }
                }
                imageResult?.getOrNull() == null -> {
                    Text(
                        "Image could not be decoded. The file may be damaged or use an unsupported format.",
                        modifier = Modifier.padding(24.dp),
                        color = NovaRunPalette.Warning,
                        fontSize = 13.sp
                    )
                }
                else -> {
                    Image(
                        bitmap = imageResult!!.getOrNull()!!.asImageBitmap(),
                        contentDescription = file.name,
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

private fun decodeProjectImage(file: File): Bitmap? {
    if (!file.isFile || !file.canRead() || file.length() <= 0L) return null

    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val maxDimension = 1800
        var sampleSize = 1
        while (
            bounds.outWidth / sampleSize > maxDimension ||
            bounds.outHeight / sampleSize > maxDimension
        ) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        BitmapFactory.decodeFile(file.absolutePath, options)
    }.getOrNull()
}

private fun formatImageSize(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format("%.1f MB", bytes / (1024f * 1024f))
    bytes >= 1024L -> String.format("%.0f KB", bytes / 1024f)
    else -> "$bytes bytes"
}
