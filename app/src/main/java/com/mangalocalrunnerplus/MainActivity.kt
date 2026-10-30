
package com.mangalocalrunnerplus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        RuntimeAdapterRegistry.register(JavaScriptRuntimeAdapter)

        setContent {
            NovaRunTheme(context = this) {
                SideEffect {
                    window.statusBarColor =
                        NovaRunPalette.Background.toArgb()

                    window.navigationBarColor =
                        NovaRunPalette.Background.toArgb()

                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility =
                        if (NovaRunPalette.isDark) 0
                        else (
                            android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                            android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                        )
                }

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
