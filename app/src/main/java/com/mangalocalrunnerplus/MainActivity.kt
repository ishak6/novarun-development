package com.mangalocalrunnerplus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.toArgb

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        RuntimeAdapterRegistry.register(
            JavaScriptRuntimeAdapter
        )

        window.statusBarColor = Color(0xFF090B10).toArgb()
        window.navigationBarColor = Color(0xFF090B10).toArgb()

        setContent {
            NovaRunTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF090B10)
                ) {
                    NovaRunApp()
                }
            }
        }
    }
}

private val NovaRunColors = darkColorScheme(
    primary = Color(0xFF0A84FF),
    secondary = Color(0xFF8B7CFF),
    background = Color(0xFF090B10),
    surface = Color(0xFF171D28),
    onPrimary = Color.White,
    onBackground = Color(0xFFF5F7FA),
    onSurface = Color(0xFFF5F7FA)
)

@Composable
fun NovaRunTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NovaRunColors,
        content = content
    )
}
