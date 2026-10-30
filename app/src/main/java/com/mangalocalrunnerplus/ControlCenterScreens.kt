
package com.mangalocalrunnerplus

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

private val ControlBackground = Color(0xFF090B10)
private val ControlCard = Color(0xFF171D28)
private val ControlBlue = Color(0xFF0A84FF)
private val ControlMuted = Color(0xFF9AA6B5)

@Composable
fun NovaRunSettingsScreen() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("novarun_settings", 0)
    }

    var port by remember {
        mutableIntStateOf(prefs.getInt("server_port", 8080))
    }
    var autoSave by remember {
        mutableStateOf(prefs.getBoolean("auto_save", true))
    }
    var wordWrap by remember {
        mutableStateOf(prefs.getBoolean("word_wrap", true))
    }
    var keepServer by remember {
        mutableStateOf(prefs.getBoolean("background_server", true))
    }
    var fontSize by remember {
        mutableIntStateOf(prefs.getInt("font_size", 14))
    }
    var theme by remember {
        mutableStateOf(prefs.getString("theme", "Dark") ?: "Dark")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ControlBackground)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Settings", color = Color.White, fontSize = 28.sp)
        Text(
            "Customize NovaRun on this device",
            color = ControlMuted,
            fontSize = 13.sp
        )

        SettingsSectionTitle("APPEARANCE")
        SettingsCard {
            Text("App theme", color = Color.White)
            Text(
                "Theme preference is saved; applying a live theme globally is a separate UI task.",
                color = ControlMuted,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Dark", "Light", "System").forEach { option ->
                    FilterChip(
                        selected = theme == option,
                        onClick = {
                            theme = option
                            prefs.edit().putString("theme", option).apply()
                        },
                        label = { Text(option) }
                    )
                }
            }
        }

        SettingsSectionTitle("LOCAL SERVER")
        SettingsCard {
            Text("Default server port", color = Color.White)
            OutlinedTextField(
                value = port.toString(),
                onValueChange = { value ->
                    val parsed = value.toIntOrNull()
                    if (
                        value.length <= 5 &&
                        value.all(Char::isDigit) &&
                        parsed != null &&
                        parsed in 1024..65535
                    ) {
                        port = parsed
                        prefs.edit().putInt("server_port", parsed).apply()
                    }
                },
                label = { Text("Port 1024–65535") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "The Localhost Control screen uses this port as its default.",
                color = ControlMuted,
                fontSize = 11.sp
            )
        }

        SettingsSwitch(
            "Keep server running in background",
            "The server uses an Android foreground service while running.",
            keepServer
        ) {
            keepServer = it
            prefs.edit().putBoolean("background_server", it).apply()
        }

        SettingsSectionTitle("CODE EDITOR PREFERENCES")
        SettingsSwitch(
            "Auto Save preference",
            "Saved preference. The editor must read this setting to change its behavior.",
            autoSave
        ) {
            autoSave = it
            prefs.edit().putBoolean("auto_save", it).apply()
        }
        SettingsSwitch(
            "Word Wrap preference",
            "Saved preference. The editor must read this setting to apply line wrapping.",
            wordWrap
        ) {
            wordWrap = it
            prefs.edit().putBoolean("word_wrap", it).apply()
        }

        SettingsCard {
            Text("Editor font size: ${fontSize}sp", color = Color.White)
            Slider(
                value = fontSize.toFloat(),
                onValueChange = {
                    fontSize = it.toInt().coerceIn(10, 24)
                    prefs.edit().putInt("font_size", fontSize).apply()
                },
                valueRange = 10f..24f,
                steps = 13
            )
        }

        SettingsSectionTitle("PROJECTS & STORAGE")
        SettingsCard {
            Text("Project storage", color = Color.White)
            Text(
                "NovaRun manages imported projects in its app storage. Use Workspace to browse projects.",
                color = ControlMuted,
                fontSize = 12.sp
            )
        }

        SettingsSectionTitle("ABOUT")
        SettingsCard {
            Text("NOVARUN", color = Color.White, fontSize = 18.sp)
            Text("Local Project Runner", color = ControlBlue)
            Text("Version 1.0 · Debug build", color = ControlMuted)
            Text(
                "Some settings above are saved preferences and need their related editor or theme implementation before they affect every screen.",
                color = ControlMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(title, color = ControlBlue, fontSize = 11.sp)
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ControlCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsSwitch(
    title: String,
    detail: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White)
                Text(detail, color = ControlMuted, fontSize = 11.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
fun NovaRunLocalhostScreen(project: File?) {
    val context = LocalContext.current
    val state by LocalServerServiceStateStore.state.collectAsState()
    val prefs = remember {
        context.getSharedPreferences("novarun_settings", 0)
    }

    var portText by remember {
        mutableStateOf(prefs.getInt("server_port", 8080).toString())
    }
    var autoStopMinutes by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ControlBackground)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Localhost Control", color = Color.White, fontSize = 27.sp)
        Text(
            "Start, stop, monitor and open your local project server.",
            color = ControlMuted,
            fontSize = 13.sp
        )

        SettingsCard {
            Text("SERVER STATUS", color = ControlMuted, fontSize = 11.sp)
            Text(
                state.status.name,
                color = when (state.status) {
                    LocalServerServiceStatus.RUNNING -> Color(0xFF30D158)
                    LocalServerServiceStatus.ERROR -> Color(0xFFFF453A)
                    LocalServerServiceStatus.STARTING -> Color(0xFFFF9F0A)
                    LocalServerServiceStatus.STOPPED -> Color.White
                },
                fontSize = 23.sp
            )
            Text(state.message, color = ControlMuted)
            state.projectName?.let {
                Text("Project: $it", color = Color.White)
            }
            if (
                state.status == LocalServerServiceStatus.RUNNING &&
                state.durationMillis > 0L
            ) {
                Text(
                    "Time left: ${state.remainingMillis / 60000} min",
                    color = Color.White
                )
            }
        }

        SettingsCard {
            Text("Server port", color = Color.White)
            OutlinedTextField(
                value = portText,
                onValueChange = {
                    if (it.length <= 5 && it.all(Char::isDigit)) {
                        portText = it
                    }
                },
                label = { Text("1024–65535") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Auto stop: ${
                    if (autoStopMinutes == 0) "Never"
                    else "$autoStopMinutes minutes"
                }",
                color = Color.White
            )
            Slider(
                value = autoStopMinutes.toFloat(),
                onValueChange = { autoStopMinutes = it.toInt() },
                valueRange = 0f..240f,
                steps = 23
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    enabled =
                        state.status != LocalServerServiceStatus.RUNNING &&
                        state.status != LocalServerServiceStatus.STARTING,
                    onClick = {
                        val parsedPort = portText.toIntOrNull()
                        when {
                            project == null || !project.isDirectory -> {
                                message = "প্রথমে Workspace থেকে একটি Project নির্বাচন করুন।"
                            }
                            parsedPort == null || parsedPort !in 1024..65535 -> {
                                message = "সঠিক Port দিন (1024–65535)।"
                            }
                            else -> {
                                prefs.edit()
                                    .putInt("server_port", parsedPort)
                                    .apply()

                                LocalWebServerService.start(
                                    context = context,
                                    projectDirectory = project,
                                    durationMillis = autoStopMinutes * 60_000L,
                                    port = parsedPort
                                )
                                message = "Server start request পাঠানো হয়েছে।"
                            }
                        }
                    }
                ) {
                    Text("Start Server")
                }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        LocalWebServerService.stop(context)
                        message = "Server stop request পাঠানো হয়েছে।"
                    }
                ) {
                    Text("Stop")
                }
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = state.status != LocalServerServiceStatus.STARTING,
                onClick = {
                    val parsedPort = portText.toIntOrNull()
                    if (project == null || !project.isDirectory) {
                        message = "Restart করার আগে Workspace থেকে Project নির্বাচন করুন।"
                    } else if (parsedPort == null || parsedPort !in 1024..65535) {
                        message = "সঠিক Port দিন (1024–65535)।"
                    } else {
                        LocalWebServerService.stop(context)
                        LocalWebServerService.start(
                            context,
                            project,
                            autoStopMinutes * 60_000L,
                            parsedPort
                        )
                        message = "Restart request পাঠানো হয়েছে।"
                    }
                }
            ) {
                Text("Restart Server")
            }
        }

        state.localUrl?.let {
            LocalhostUrlCard("Localhost URL", it)
        }
        state.lanUrl?.let {
            LocalhostUrlCard("LAN URL", it)
        }

        state.url?.let { url ->
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    runCatching {
                        val viewIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                        )
                        context.startActivity(
                            Intent.createChooser(viewIntent, "Open with browser")
                        )
                    }.onFailure {
                        message = "Browser খুলতে সমস্যা: ${it.message}"
                    }
                }
            ) {
                Text("Open in External Browser")
            }
        }

        message?.let {
            Text(it, color = ControlMuted, fontSize = 12.sp)
        }

        SettingsCard {
            Text("Network note", color = Color.White)
            Text(
                "LAN URL অন্য ডিভাইসে খুলতে হলে দুই ডিভাইস একই Wi-Fi/Hotspot-এ থাকতে হবে। Router client isolation বা firewall থাকলে LAN access বন্ধ থাকতে পারে। এই server-এ এখনো password/token protection নেই; বিশ্বস্ত network ছাড়া LAN sharing ব্যবহার করবেন না।",
                color = ControlMuted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun LocalhostUrlCard(label: String, url: String) {
    val context = LocalContext.current
    SettingsCard {
        Text(label, color = ControlMuted, fontSize = 11.sp)
        Text(url, color = Color.White)
        TextButton(
            onClick = {
                val clipboard = context.getSystemService(
                    android.content.Context.CLIPBOARD_SERVICE
                ) as android.content.ClipboardManager
                clipboard.setPrimaryClip(
                    android.content.ClipData.newPlainText(label, url)
                )
            }
        ) {
            Text("Copy URL")
        }
    }
}
