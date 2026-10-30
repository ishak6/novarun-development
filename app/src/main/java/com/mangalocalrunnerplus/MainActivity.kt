package com.mangalocalrunnerplus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        RuntimeAdapterRegistry.register(
            JavaScriptRuntimeAdapter
        )

        window.statusBarColor =
            NovaRunPalette.Background.toArgb()

        window.navigationBarColor =
            NovaRunPalette.Background.toArgb()

        setContent {
            NovaRunTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NovaRunPalette.Background
                ) {
                    NovaRunApp()
                }
            }
        }
    }
}
