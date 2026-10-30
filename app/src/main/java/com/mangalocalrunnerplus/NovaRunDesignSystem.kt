
package com.mangalocalrunnerplus

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

object NovaRunPalette {
    var isDark by mutableStateOf(true)
        private set

    fun setDarkMode(value: Boolean) {
        isDark = value
    }

    val Background: Color
        get() = if (isDark) Color(0xFF09090D) else Color(0xFFF4F6FA)

    val Surface: Color
        get() = if (isDark) Color(0xFF15151D) else Color(0xFFFFFFFF)

    val SurfaceElevated: Color
        get() = if (isDark) Color(0xFF20202B) else Color(0xFFE9EDF4)

    val Cyan = Color(0xFF00BFD6)
    val Lime = Color(0xFF9BC900)
    val Purple = Color(0xFF8950D8)
    val Pink = Color(0xFFE83C91)

    val TextPrimary: Color
        get() = if (isDark) Color(0xFFF5F5F7) else Color(0xFF171923)

    val TextSecondary: Color
        get() = if (isDark) Color(0xFF9B9BA8) else Color(0xFF626879)

    val Border: Color
        get() = if (isDark) Color(0xFF454552) else Color(0xFFD5DAE3)

    val Success = Color(0xFF36B85A)
    val Warning = Color(0xFFE6A52D)
    val Error = Color(0xFFE04452)
}

@Composable
fun NovaRunTheme(
    context: Context,
    content: @Composable () -> Unit
) {
    val prefs = remember(context) {
        context.getSharedPreferences("novarun_settings", Context.MODE_PRIVATE)
    }

    var selectedTheme by remember {
        mutableStateOf(prefs.getString("theme", "Dark") ?: "Dark")
    }

    DisposableEffect(prefs) {
        val listener =
            android.content.SharedPreferences.OnSharedPreferenceChangeListener {
                    sharedPreferences, key ->
                if (key == "theme") {
                    selectedTheme =
                        sharedPreferences?.getString("theme", "Dark") ?: "Dark"
                }
            }

        prefs.registerOnSharedPreferenceChangeListener(listener)

        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val systemDark = isSystemInDarkTheme()
    val dark = when (selectedTheme) {
        "Light" -> false
        "System" -> systemDark
        else -> true
    }

    SideEffect {
        NovaRunPalette.setDarkMode(dark)
    }

    val colors = if (dark) {
        darkColorScheme(
            primary = NovaRunPalette.Cyan,
            onPrimary = Color(0xFF00191D),
            secondary = NovaRunPalette.Lime,
            onSecondary = Color(0xFF182000),
            tertiary = NovaRunPalette.Purple,
            background = Color(0xFF09090D),
            onBackground = Color(0xFFF5F5F7),
            surface = Color(0xFF15151D),
            onSurface = Color(0xFFF5F5F7),
            surfaceVariant = Color(0xFF20202B),
            onSurfaceVariant = Color(0xFF9B9BA8),
            outline = Color(0xFF454552),
            error = Color(0xFFE04452)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF007F91),
            onPrimary = Color.White,
            secondary = Color(0xFF638500),
            onSecondary = Color.White,
            tertiary = Color(0xFF7040B5),
            background = Color(0xFFF4F6FA),
            onBackground = Color(0xFF171923),
            surface = Color.White,
            onSurface = Color(0xFF171923),
            surfaceVariant = Color(0xFFE9EDF4),
            onSurfaceVariant = Color(0xFF626879),
            outline = Color(0xFFD5DAE3),
            error = Color(0xFFB3261E)
        )
    }

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
