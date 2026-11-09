package com.mangalocalrunnerplus

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

private val ControlBackground
    get() = NovaRunPalette.Background

private val ControlCard
    get() = NovaRunPalette.Surface

private val ControlBlue
    get() = NovaRunPalette.Cyan

private val ControlMuted
    get() = NovaRunPalette.TextSecondary

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

    var maxZipEntries by remember { mutableStateOf(prefs.getInt("zip_max_entries", 2000).toString()) }
    var maxZipTotalMb by remember { mutableStateOf(prefs.getInt("zip_max_total_mb", 200).toString()) }
    var maxZipSingleMb by remember { mutableStateOf(prefs.getInt("zip_max_single_mb", 64).toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ControlBackground)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        NovaRunBrandLogo(
            markSize = 42.dp,
            showTagline = false
        )

        Text(
            "SETTINGS",
            color = NovaRunPalette.TextPrimary,
            fontSize = 28.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
        )

        Text(
            "Customize NovaRun on this device",
            color = ControlMuted,
            fontSize = 13.sp
        )

        SettingsSectionTitle("APPEARANCE")

        SettingsCard {
            Text(
                "App theme",
                color = NovaRunPalette.TextPrimary
            )

            Text(
                "Choose your saved theme preference.",
                color = ControlMuted,
                fontSize = 11.sp
            )

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Dark", "Light", "System").forEach { option ->
                    FilterChip(
                        selected = theme == option,
                        onClick = {
                            theme = option
                            prefs.edit()
                                .putString("theme", option)
                                .apply()
                        },
                        label = {
                            Text(option)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NovaRunPalette.Cyan,
                            selectedLabelColor = NovaRunPalette.Background
                        )
                    )
                }
            }
        }

        SettingsSectionTitle("LOCAL SERVER")

        SettingsCard {
            Text(
                "Default server port",
                color = NovaRunPalette.TextPrimary
            )

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

                        prefs.edit()
                            .putInt("server_port", parsed)
                            .apply()
                    }
                },
                label = {
                    Text("Port 1024–65535")
                },
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
            title = "Keep server running in background",
            detail = "When off, an active server stops as soon as NovaRun leaves the foreground.",
            checked = keepServer
        ) {
            keepServer = it

            prefs.edit()
                .putBoolean("background_server", it)
                .apply()
        }

        SettingsSectionTitle("CODE EDITOR PREFERENCES")

        SettingsSwitch(
            title = "Auto Save",
            detail = "Automatically save edits after a short pause.",
            checked = autoSave
        ) {
            autoSave = it

            prefs.edit()
                .putBoolean("auto_save", it)
                .apply()
        }

        SettingsSwitch(
            title = "Word Wrap",
            detail = "Wrap long source-code lines inside the editor.",
            checked = wordWrap
        ) {
            wordWrap = it

            prefs.edit()
                .putBoolean("word_wrap", it)
                .apply()
        }

        SettingsCard {
            Text(
                "Editor font size: ${fontSize}sp",
                color = NovaRunPalette.TextPrimary
            )

            Slider(
                value = fontSize.toFloat(),
                onValueChange = {
                    fontSize = it.toInt().coerceIn(10, 24)

                    prefs.edit()
                        .putInt("font_size", fontSize)
                        .apply()
                },
                valueRange = 10f..24f,
                steps = 13,
                colors = SliderDefaults.colors(
                    thumbColor = NovaRunPalette.Lime,
                    activeTrackColor = NovaRunPalette.Cyan
                )
            )
        }

        SettingsSectionTitle("PROJECTS & STORAGE")

        SettingsCard {
            Text(
                "Project storage",
                color = NovaRunPalette.TextPrimary
            )

            Text(
                "NovaRun manages imported projects in its app storage. Use Workspace to browse projects.",
                color = ControlMuted,
                fontSize = 12.sp
            )
        }

        SettingsSectionTitle("FULL FREEDOM · ZIP IMPORT")

        SettingsCard {
            Text("Import limits", color = NovaRunPalette.TextPrimary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Text("Set 0 for no configured cap. Android storage availability and ZIP path safety still apply.", color = ControlMuted, fontSize = 11.sp)

            OutlinedTextField(
                value = maxZipEntries,
                onValueChange = { value ->
                    if (value.length <= 8 && value.all(Char::isDigit)) {
                        maxZipEntries = value
                        value.toIntOrNull()?.let { prefs.edit().putInt("zip_max_entries", it.coerceAtLeast(0)).apply() }
                    }
                },
                label = { Text("Maximum ZIP entries (0 = unlimited)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = maxZipTotalMb,
                onValueChange = { value ->
                    if (value.length <= 8 && value.all(Char::isDigit)) {
                        maxZipTotalMb = value
                        value.toIntOrNull()?.let { prefs.edit().putInt("zip_max_total_mb", it.coerceAtLeast(0)).apply() }
                    }
                },
                label = { Text("Maximum extracted total MB (0 = unlimited)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = maxZipSingleMb,
                onValueChange = { value ->
                    if (value.length <= 8 && value.all(Char::isDigit)) {
                        maxZipSingleMb = value
                        value.toIntOrNull()?.let { prefs.edit().putInt("zip_max_single_mb", it.coerceAtLeast(0)).apply() }
                    }
                },
                label = { Text("Maximum single file MB (0 = unlimited)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text("Current: ${maxZipEntries.ifBlank { "2000" }} entries · ${maxZipTotalMb.ifBlank { "200" }} MB total · ${maxZipSingleMb.ifBlank { "64" }} MB/file", color = NovaRunPalette.Cyan, fontSize = 11.sp)
            OutlinedButton(
                onClick = {
                    maxZipEntries = "2000"
                    maxZipTotalMb = "200"
                    maxZipSingleMb = "64"
                    prefs.edit().putInt("zip_max_entries", 2000).putInt("zip_max_total_mb", 200).putInt("zip_max_single_mb", 64).apply()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Restore recommended defaults") }
        }

        SettingsSectionTitle("ABOUT")

        SettingsCard {
            Text(
                "NOVARUN",
                color = NovaRunPalette.Cyan,
                fontSize = 20.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
            )

            Text(
                "Local Project Runner",
                color = NovaRunPalette.Lime
            )

            Text(
                "Version 1.0 · Debug build",
                color = ControlMuted
            )

            Text(
                "Theme and editor preferences are stored locally on this device.",
                color = ControlMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        title,
        color = ControlBlue,
        fontSize = 11.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        letterSpacing = 1.5.sp
    )
}

@Composable
private fun SettingsCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ControlCard
        ),
        shape = RectangleShape,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            NovaRunPalette.Border
        )
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
            Column(
                Modifier.weight(1f)
            ) {
                Text(
                    title,
                    color = NovaRunPalette.TextPrimary
                )

                Text(
                    detail,
                    color = ControlMuted,
                    fontSize = 11.sp
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NovaRunPalette.Background,
                    checkedTrackColor = NovaRunPalette.Lime
                )
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
        mutableStateOf(
            prefs.getInt("server_port", 8080).toString()
        )
    }

    var autoStopMinutes by remember {
        mutableIntStateOf(0)
    }

    var message by remember {
        mutableStateOf<String?>(null)
    }

    var showAccessPassword by remember {
        mutableStateOf(false)
    }

    var lanPasswordEnabled by remember {
        mutableStateOf(prefs.getBoolean("lan_password_enabled", true))
    }

    fun copyToClipboard(value: String, label: String) {
        val clipboard = context.getSystemService(
            android.content.Context.CLIPBOARD_SERVICE
        ) as? android.content.ClipboardManager

        if (clipboard == null) {
            android.widget.Toast.makeText(
                context,
                "Clipboard unavailable",
                android.widget.Toast.LENGTH_LONG
            ).show()
            return
        }

        runCatching {
            clipboard.setPrimaryClip(
                android.content.ClipData.newPlainText("NovaRun $label", value)
            )
        }.onSuccess {
            android.widget.Toast.makeText(
                context,
                "$label copied",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }.onFailure {
            android.widget.Toast.makeText(
                context,
                "Copy failed: ${it.localizedMessage ?: "clipboard error"}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ControlBackground)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        NovaRunBrandLogo(
            markSize = 42.dp,
            showTagline = false
        )

        Text(
            "LOCALHOST CONTROL",
            color = NovaRunPalette.TextPrimary,
            fontSize = 25.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
        )

        Text(
            "Start, stop, monitor and open your local project server.",
            color = ControlMuted,
            fontSize = 13.sp
        )

        SettingsCard {
            Text(
                "SERVER STATUS",
                color = ControlMuted,
                fontSize = 11.sp
            )

            Text(
                state.status.name,
                color = when (state.status) {
                    LocalServerServiceStatus.RUNNING ->
                        NovaRunPalette.Lime

                    LocalServerServiceStatus.ERROR ->
                        NovaRunPalette.Error

                    LocalServerServiceStatus.STARTING ->
                        NovaRunPalette.Warning

                    LocalServerServiceStatus.STOPPED ->
                        NovaRunPalette.TextPrimary
                },
                fontSize = 23.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )

            Text(
                state.message,
                color = ControlMuted
            )

            state.projectName?.let {
                Text(
                    "Project: $it",
                    color = NovaRunPalette.TextPrimary
                )
            }

            if (
                state.status == LocalServerServiceStatus.RUNNING &&
                state.durationMillis > 0L
            ) {
                Text(
                    "Time left: ${state.remainingMillis / 60000} min",
                    color = NovaRunPalette.Lime
                )
            }
        }

        LanCredentialsSettingsCard()

        SettingsCard {
            Text(
                "Server port",
                color = NovaRunPalette.TextPrimary
            )

            OutlinedTextField(
                value = portText,
                onValueChange = {
                    if (
                        it.length <= 5 &&
                        it.all(Char::isDigit)
                    ) {
                        portText = it
                    }
                },
                label = {
                    Text("1024–65535")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("LAN password protection", color = NovaRunPalette.TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = lanPasswordEnabled, onClick = { lanPasswordEnabled = true; prefs.edit().putBoolean("lan_password_enabled", true).apply() }, enabled = state.status != LocalServerServiceStatus.RUNNING && state.status != LocalServerServiceStatus.STARTING, label = { Text("Password ON") })
                FilterChip(selected = !lanPasswordEnabled, onClick = { lanPasswordEnabled = false; prefs.edit().putBoolean("lan_password_enabled", false).apply() }, enabled = state.status != LocalServerServiceStatus.RUNNING && state.status != LocalServerServiceStatus.STARTING, label = { Text("Password OFF") })
            }
            Text(if (lanPasswordEnabled) "LAN access requires your saved custom username and password." else "WARNING: Anyone on the same network can access this project without a password.", color = if (lanPasswordEnabled) NovaRunPalette.Lime else NovaRunPalette.Warning, fontSize = 11.sp)

            Text(
                "Auto stop: ${
                    if (autoStopMinutes == 0) {
                        "Never"
                    } else {
                        "$autoStopMinutes minutes"
                    }
                }",
                color = NovaRunPalette.TextPrimary
            )

            Slider(
                value = autoStopMinutes.toFloat(),
                onValueChange = {
                    autoStopMinutes = it.toInt()
                },
                valueRange = 0f..240f,
                steps = 23,
                colors = SliderDefaults.colors(
                    thumbColor = NovaRunPalette.Lime,
                    activeTrackColor = NovaRunPalette.Cyan
                )
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NovaRunPalette.Cyan,
                        contentColor = NovaRunPalette.Background
                    ),
                    shape = RectangleShape,
                    onClick = {
                        val parsedPort = portText.toIntOrNull()

                        when {
                            project == null || !project.isDirectory -> {
                                message =
                                    "প্রথমে Workspace থেকে একটি Project নির্বাচন করুন।"
                            }

                            parsedPort == null ||
                                parsedPort !in 1024..65535 -> {
                                message =
                                    "সঠিক Port দিন (1024–65535)।"
                            }

                            else -> {
                                prefs.edit()
                                    .putInt("server_port", parsedPort)
                                    .apply()

                                LocalWebServerService.start(
                                    context = context,
                                    projectDirectory = project,
                                    durationMillis =
                                        autoStopMinutes * 60_000L,
                                    port = parsedPort,
                                    authenticationEnabled = lanPasswordEnabled
                                )

                                message =
                                    "Server start request পাঠানো হয়েছে।"
                            }
                        }
                    }
                ) {
                    Text("Start Server")
                }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    shape = RectangleShape,
                    onClick = {
                        LocalWebServerService.stop(context)

                        message =
                            "Server stop request পাঠানো হয়েছে।"
                    }
                ) {
                    Text("Stop")
                }
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled =
                    state.status != LocalServerServiceStatus.STARTING,
                shape = RectangleShape,
                onClick = {
                    val parsedPort = portText.toIntOrNull()

                    if (project == null || !project.isDirectory) {
                        message =
                            "Restart করার আগে Workspace থেকে Project নির্বাচন করুন।"
                    } else if (
                        parsedPort == null ||
                        parsedPort !in 1024..65535
                    ) {
                        message =
                            "সঠিক Port দিন (1024–65535)।"
                    } else {
                        LocalWebServerService.stop(context)

                        LocalWebServerService.start(
                            context,
                            project,
                            autoStopMinutes * 60_000L,
                            parsedPort,
                            lanPasswordEnabled
                        )

                        message =
                            "Restart request পাঠানো হয়েছে।"
                    }
                }
            ) {
                Text("Restart Server")
            }
        }

        state.localUrl?.let {
            LocalhostUrlCard(
                "Localhost URL",
                it
            )
        }

        state.lanUrl?.let {
            LocalhostUrlCard(
                "LAN URL",
                it
            )
        }

        if (state.status == LocalServerServiceStatus.RUNNING && state.lanUrl != null) {
            SettingsCard {
                Text(
                    "LAN ACCESS PASSWORD",
                    color = NovaRunPalette.Cyan,
                    fontSize = 11.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )

                Text(
                    if (!state.authenticationEnabled) "Password protection is OFF" else state.accessPassword?.let {
                        if (showAccessPassword) it else "••••••••••••••••••••••••"
                    } ?: "Password unavailable — stop and restart the server",
                    color = NovaRunPalette.TextPrimary,
                    fontSize = 14.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        enabled = state.accessPassword != null,
                        onClick = { showAccessPassword = !showAccessPassword }
                    ) {
                        Text(if (showAccessPassword) "Hide Password" else "Show Password")
                    }

                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = state.accessPassword != null,
                        onClick = {
                            state.accessPassword?.let {
                                copyToClipboard(it, "Password")
                            }
                        }
                    ) {
                        Text("Copy Password")
                    }
                }

                Text(
                    "Browser username: ${state.accessUsername ?: "not available"}",
                    color = ControlMuted,
                    fontSize = 12.sp
                )

                Text(
                    if (state.authenticationEnabled) "LAN authentication: ON" else "LAN authentication: OFF — no password required",
                    color = if (state.authenticationEnabled) NovaRunPalette.Lime else NovaRunPalette.Warning,
                    fontSize = 12.sp
                )
            }
        }

        state.url?.let { url ->
            Button(
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NovaRunPalette.Lime,
                    contentColor = NovaRunPalette.Background
                ),
                onClick = {
                    runCatching {
                        val viewIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                        )

                        context.startActivity(
                            Intent.createChooser(
                                viewIntent,
                                "Open with browser"
                            )
                        )
                    }.onFailure {
                        message =
                            "Browser খুলতে সমস্যা: ${it.message}"
                    }
                }
            ) {
                Text("Open in External Browser")
            }
        }

        message?.let {
            Text(
                it,
                color = ControlMuted,
                fontSize = 12.sp
            )
        }

        SettingsCard {
            Text(
                "NETWORK NOTE",
                color = NovaRunPalette.Cyan,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )

            Text(
                "LAN URL অন্য ডিভাইসে খুলতে হলে দুই ডিভাইস একই Wi-Fi/Hotspot-এ থাকতে হবে। Router client isolation বা firewall থাকলে LAN access বন্ধ থাকতে পারে। LAN server-এ Basic Authentication চালু থাকে: username novarun এবং উপরের session password দিতে হবে। HTTP হওয়ায় একই নেটওয়ার্কে traffic encrypted নয়—শুধু বিশ্বস্ত private network-এ ব্যবহার করুন।",
                color = ControlMuted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun LocalhostUrlCard(
    label: String,
    url: String
) {
    val context = LocalContext.current

    SettingsCard {
        Text(
            label.uppercase(),
            color = NovaRunPalette.Cyan,
            fontSize = 11.sp
        )

        Text(
            url,
            color = NovaRunPalette.TextPrimary
        )

        TextButton(
            onClick = {
                val clipboard = context.getSystemService(
                    android.content.Context.CLIPBOARD_SERVICE
                ) as android.content.ClipboardManager

                clipboard.setPrimaryClip(
                    android.content.ClipData.newPlainText(
                        label,
                        url
                    )
                )
            }
        ) {
            Text(
                "COPY URL",
                color = NovaRunPalette.Lime
            )
        }
    }
}
