
package com.mangalocalrunnerplus

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebPreviewScreen(
    projectDirectory: File,
    onBack: () -> Unit = {}
) {
    val indexFile = remember(projectDirectory) {
        runCatching {
            val root = projectDirectory.canonicalFile
            val index = File(root, "index.html").canonicalFile

            if (
                index.path.startsWith(root.path + File.separator) &&
                index.isFile
            ) {
                index
            } else {
                null
            }
        }.getOrNull()
    }

    var webView by remember {
        mutableStateOf<WebView?>(null)
    }

    var loadError by remember(projectDirectory) {
        mutableStateOf<String?>(null)
    }

    var reloadKey by remember {
        mutableIntStateOf(0)
    }

    DisposableEffect(projectDirectory) {
        onDispose {
            webView?.let { view ->
                view.stopLoading()
                view.loadUrl("about:blank")
                view.removeAllViews()
                view.destroy()
            }
            webView = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090B10))
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Web Preview",
                color = Color.White,
                fontSize = 22.sp,
                style = MaterialTheme.typography.headlineSmall
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF202938)
                    )
                ) {
                    Text("Back")
                }

                Spacer(Modifier.padding(horizontal = 3.dp))

                Button(
                    onClick = {
                        loadError = null
                        reloadKey++
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0A84FF)
                    ),
                    enabled = indexFile != null
                ) {
                    Text("Refresh")
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = indexFile?.name ?: "index.html not found",
            color = Color(0xFF9AA6B5),
            fontSize = 12.sp
        )

        Spacer(Modifier.height(10.dp))

        if (indexFile == null) {

            Text(
                text = "No valid index.html was found in the project root.",
                color = Color(0xFFFF9F0A),
                fontSize = 14.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Place an index.html file in the project's main folder to preview it.",
                color = Color(0xFF9AA6B5),
                fontSize = 13.sp
            )

        } else {

            loadError?.let { error ->

                Text(
                    text = "Preview error: $error",
                    color = Color(0xFFFF453A),
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(8.dp))
            }

            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                factory = { context ->

                    WebView(context).apply {

                        webView = this

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true

                            allowFileAccess = true
                            allowContentAccess = false

                            allowFileAccessFromFileURLs = false
                            allowUniversalAccessFromFileURLs = false

                            mixedContentMode =
                                WebSettings.MIXED_CONTENT_NEVER_ALLOW

                            javaScriptCanOpenWindowsAutomatically = false
                            setSupportMultipleWindows(false)
                        }

                        webViewClient = object : WebViewClient() {

                            override fun onPageStarted(
                                view: WebView?,
                                url: String?,
                                favicon: Bitmap?
                            ) {
                                super.onPageStarted(view, url, favicon)
                                loadError = null
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(
                                    view,
                                    request,
                                    error
                                )

                                if (request?.isForMainFrame == true) {
                                    loadError =
                                        error?.description?.toString()
                                            ?: "Unable to load page"
                                }
                            }
                        }

                        webChromeClient = WebChromeClient()

                        tag = reloadKey

                        loadUrl(indexFile.toURI().toString())
                    }
                },
                update = { view ->

                    if (view.tag != reloadKey) {
                        view.tag = reloadKey
                        view.reload()
                    }
                }
            )
        }
    }
}
