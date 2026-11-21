package com.mangalocalrunnerplus

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.ByteArrayInputStream

object ProjectImageFiles {
    private val commonRasterExtensions = setOf("png", "jpg", "jpeg", "webp", "gif", "bmp")

    /**
     * HEIF/HEIC decoding is available from Android 9; AVIF decoding is available
     * from Android 12. SVG is rendered in a locked-down, script-disabled WebView.
     */
    fun isPreviewable(file: File, sdkInt: Int = Build.VERSION.SDK_INT): Boolean {
        if (!file.isFile) return false
        return when (file.extension.lowercase()) {
            in commonRasterExtensions, "svg" -> true
            "heic", "heif" -> sdkInt >= Build.VERSION_CODES.P
            "avif" -> sdkInt >= Build.VERSION_CODES.S
            else -> false
        }
    }

    fun isSvg(file: File): Boolean = file.extension.equals("svg", ignoreCase = true)
}

@Composable
fun ProjectImagePreviewScreen(
    file: File,
    onBack: () -> Unit
) {
    val isSvg = ProjectImageFiles.isSvg(file)
    val imageResult by produceState<Result<Bitmap?>?>(null, file.absolutePath, file.length(), file.lastModified()) {
        value = withContext(Dispatchers.IO) {
            runCatching { if (isSvg) null else decodeProjectImage(file) }
        }
    }
    val svgResult by produceState<Result<String?>?>(null, file.absolutePath, file.length(), file.lastModified()) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                if (!isSvg || !file.isFile || !file.canRead() || file.length() !in 1..(8L * 1024L * 1024L)) {
                    null
                } else {
                    file.readText(Charsets.UTF_8)
                }
            }
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
                isSvg && svgResult == null -> ImageLoading()
                isSvg && svgResult?.getOrNull() == null -> ImageError("SVG could not be read, is empty, or exceeds the 8 MB preview limit.")
                isSvg -> {
                    val svg = svgResult?.getOrNull().orEmpty()
                    AndroidView(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = false
                                settings.domStorageEnabled = false
                                settings.allowFileAccess = false
                                settings.allowContentAccess = false
                                settings.allowFileAccessFromFileURLs = false
                                settings.allowUniversalAccessFromFileURLs = false
                                settings.blockNetworkLoads = true
                                settings.javaScriptCanOpenWindowsAutomatically = false
                                settings.setSupportMultipleWindows(false)
                                webViewClient = object : WebViewClient() {
                                    override fun shouldInterceptRequest(
                                        view: WebView?,
                                        request: android.webkit.WebResourceRequest?
                                    ): WebResourceResponse? = WebResourceResponse(
                                        "text/plain",
                                        "UTF-8",
                                        403,
                                        "Blocked",
                                        emptyMap(),
                                        ByteArrayInputStream(ByteArray(0))
                                    )
                                }
                                loadDataWithBaseURL(
                                    "https://novarun.invalid/",
                                    svg,
                                    "image/svg+xml",
                                    "UTF-8",
                                    null
                                )
                            }
                        },
                        update = { view ->
                            if (view.tag != file.lastModified()) {
                                view.tag = file.lastModified()
                                view.loadDataWithBaseURL(
                                    "https://novarun.invalid/",
                                    svg,
                                    "image/svg+xml",
                                    "UTF-8",
                                    null
                                )
                            }
                        }
                    )
                }
                imageResult == null -> ImageLoading()
                imageResult?.getOrNull() == null -> ImageError(
                    "Image could not be decoded. The file may be damaged or use an unsupported format."
                )
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

@Composable
private fun ImageLoading() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            color = NovaRunPalette.Cyan
        )
        Spacer(Modifier.height(12.dp))
        Text("Loading image…", color = NovaRunPalette.TextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun ImageError(message: String) {
    Text(
        message,
        modifier = Modifier.padding(24.dp),
        color = NovaRunPalette.Warning,
        fontSize = 13.sp
    )
}

private fun decodeProjectImage(file: File): Bitmap? {
    if (!file.isFile || !file.canRead() || file.length() <= 0L) return null

    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(file)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val width = info.size.width
                val height = info.size.height
                val maxDimension = 1800
                val scale = minOf(1f, maxDimension.toFloat() / maxOf(width, height))
                if (scale < 1f) {
                    decoder.setTargetSize(
                        (width * scale).toInt().coerceAtLeast(1),
                        (height * scale).toInt().coerceAtLeast(1)
                    )
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = false
            }
        } else {
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
        }
    }.getOrNull()
}

private fun formatImageSize(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format("%.1f MB", bytes / (1024f * 1024f))
    bytes >= 1024L -> String.format("%.0f KB", bytes / 1024f)
    else -> "$bytes bytes"
}
