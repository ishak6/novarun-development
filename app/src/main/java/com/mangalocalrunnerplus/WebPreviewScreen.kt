package com.mangalocalrunnerplus

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebPreviewScreen(
    projectDirectory: File,
    onBack: () -> Unit = {},
    nodeServerUrl: String? = null
) {
    val validation = remember(projectDirectory) {
        ProjectPreviewValidator.validate(projectDirectory)
    }
    val projectRoot = validation.projectRoot

    var previewServer by remember(projectDirectory) {
        mutableStateOf<LocalWebServer?>(null)
    }
    var previewPort by remember(projectDirectory) {
        mutableIntStateOf(0)
    }
    var serverError by remember(projectDirectory) {
        mutableStateOf<String?>(null)
    }
    var webView by remember {
        mutableStateOf<WebView?>(null)
    }
    var loadError by remember(projectDirectory) {
        mutableStateOf<String?>(null)
    }
    var isLoading by remember {
        mutableStateOf(false)
    }
    var progress by remember {
        mutableIntStateOf(0)
    }
    var reloadKey by remember {
        mutableIntStateOf(0)
    }

    // Use loopback HTTP instead of file:// so ES modules, fetch, relative
    // assets and client-side routes run in a normal same-origin context.
    // The preview listener is bound to loopback only and is not LAN-visible.
    LaunchedEffect(projectRoot, nodeServerUrl) {
        if (nodeServerUrl != null) return@LaunchedEffect
        if (projectRoot == null || !validation.isValid) return@LaunchedEffect

        val started = withContext(Dispatchers.IO) {
            var active: Pair<LocalWebServer, Int>? = null
            for (port in 18080..18100) {
                val candidate = LocalWebServer(
                    projectDirectory = projectRoot,
                    port = port,
                    bindToLan = false,
                    defaultDocument = validation.entryFile?.name ?: "index.html"
                )
                if (candidate.start()) {
                    active = candidate to port
                    break
                }
            }
            active
        }

        if (started != null) {
            previewServer = started.first
            previewPort = started.second
            serverError = null
        } else {
            serverError = "Could not start the private preview server. Ports 18080–18100 may be unavailable."
        }
    }

    DisposableEffect(previewServer) {
        onDispose {
            previewServer?.stop()
        }
    }

    DisposableEffect(webView) {
        onDispose {
            webView?.let { view ->
                view.stopLoading()
                view.loadUrl("about:blank")
                view.removeAllViews()
                view.destroy()
            }
        }
    }

    val backendUri = remember(nodeServerUrl) { nodeServerUrl?.let(Uri::parse) }
    val allowedPort = backendUri?.port ?: previewPort
    val previewUrl = nodeServerUrl ?: if (previewServer != null && previewPort > 0) {
        "http://127.0.0.1:$previewPort/"
    } else {
        null
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
                text = "Website Test Preview",
                color = Color.White,
                fontSize = 22.sp,
                style = MaterialTheme.typography.headlineSmall
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF202938))
                ) { Text("Back") }
                Button(
                    onClick = {
                        loadError = null
                        reloadKey++
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF)),
                    enabled = previewUrl != null
                ) { Text("Refresh") }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = validation.entryFile?.relativeTo(projectRoot ?: projectDirectory)?.invariantSeparatorsPath
                ?: if (nodeServerUrl != null) "Node.js backend: $nodeServerUrl" else "Preview entry point unavailable",
            color = Color(0xFF9AA6B5),
            fontSize = 12.sp
        )

        if (nodeServerUrl != null) {
            Button(
                onClick = {
                    val adminLoginUrl = Uri.parse(nodeServerUrl)
                        .buildUpon()
                        .path("/admin/login")
                        .build()
                        .toString()
                    webView?.loadUrl(adminLoginUrl)
                },
                enabled = webView != null,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30D158))
            ) { Text("Open Admin Login") }
            Spacer(Modifier.height(8.dp))
        }

        if (isLoading) {
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = progress / 100f,
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0A84FF),
                trackColor = Color(0xFF202938)
            )
            Text(
                text = "Loading website... $progress%",
                color = Color(0xFF9AA6B5),
                fontSize = 11.sp
            )
        }

        Spacer(Modifier.height(10.dp))

        when {
            !validation.isValid && nodeServerUrl == null -> Text(
                text = validation.message,
                color = Color(0xFFFF9F0A),
                fontSize = 14.sp
            )
            serverError != null -> Text(
                text = serverError.orEmpty(),
                color = Color(0xFFFF9F0A),
                fontSize = 14.sp
            )
            previewUrl == null -> Text(
                text = "Starting private website preview server...",
                color = Color(0xFF9AA6B5),
                fontSize = 14.sp
            )
            else -> {
                Text(
                    text = previewUrl,
                    color = Color(0xFF30D158),
                    fontSize = 11.sp
                )
                loadError?.let { error ->
                    Text(
                        text = "Preview error: $error",
                        color = Color(0xFFFF453A),
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))
                }

                AndroidView(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    factory = { context ->
                        WebView(context).apply {
                            webView = this
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                allowFileAccess = false
                                allowContentAccess = false
                                allowFileAccessFromFileURLs = false
                                allowUniversalAccessFromFileURLs = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                javaScriptCanOpenWindowsAutomatically = false
                                setSupportMultipleWindows(false)
                                safeBrowsingEnabled = true
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(
                                    view: WebView?,
                                    url: String?,
                                    favicon: Bitmap?
                                ) {
                                    super.onPageStarted(view, url, favicon)
                                    loadError = null
                                    isLoading = true
                                    progress = 0
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    progress = 100
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val uri = request?.url ?: return true
                                    return !isAllowedPreviewUrl(uri, allowedPort)
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    super.onReceivedError(view, request, error)
                                    if (request?.isForMainFrame == true) {
                                        isLoading = false
                                        loadError = error?.description?.toString()
                                            ?: "Unable to load website"
                                    }
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    super.onProgressChanged(view, newProgress)
                                    progress = newProgress
                                    if (newProgress >= 100) isLoading = false
                                }
                            }

                            tag = reloadKey
                            loadUrl(previewUrl)
                        }
                    },
                    update = { view ->
                        if (view.tag != reloadKey) {
                            view.tag = reloadKey
                            loadError = null
                            isLoading = true
                            progress = 0
                            view.reload()
                        }
                    }
                )
            }
        }
    }
}

private fun isAllowedPreviewUrl(uri: Uri, allowedPort: Int): Boolean {
    if (allowedPort !in 1..65535 || uri.scheme != "http") return false
    val hostAllowed = uri.host == "127.0.0.1" || uri.host == "localhost"
    return hostAllowed && uri.port == allowedPort
}
