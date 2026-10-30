
package com.mangalocalrunnerplus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import java.io.File

private const val MAX_TIMER_MILLIS =
    7L * 24L * 60L * 60L * 1000L

@Composable
fun LocalServerScreen(
    projectDirectory: File?,
    onBack: () -> Unit = {}
) {

    val context = LocalContext.current

    val serverState by
        LocalServerServiceStateStore.state.collectAsState()

    var selectedTimer by remember {
        mutableStateOf("1h")
    }

    var days by remember {
        mutableStateOf("0")
    }

    var hours by remember {
        mutableStateOf("1")
    }

    var minutes by remember {
        mutableStateOf("0")
    }

    val dayValue = days.toLongOrNull() ?: 0L
    val hourValue = hours.toLongOrNull() ?: 0L
    val minuteValue = minutes.toLongOrNull() ?: 0L

    val customDuration =
        dayValue * 24L * 60L * 60L * 1000L +
        hourValue * 60L * 60L * 1000L +
        minuteValue * 60L * 1000L

    val customTimerValid =
        dayValue >= 0L &&
        hourValue >= 0L &&
        minuteValue >= 0L &&
        customDuration > 0L &&
        customDuration <= MAX_TIMER_MILLIS

    val isUnlimited = selectedTimer == "Unlimited"

    val isRunning =
        serverState.status == LocalServerServiceStatus.RUNNING

    val isStarting =
        serverState.status == LocalServerServiceStatus.STARTING

    val statusColor = when (serverState.status) {

        LocalServerServiceStatus.RUNNING ->
            Color(0xFF30D158)

        LocalServerServiceStatus.STARTING ->
            Color(0xFFFF9F0A)

        LocalServerServiceStatus.ERROR ->
            Color(0xFFFF453A)

        LocalServerServiceStatus.STOPPED ->
            Color(0xFF9AA6B5)
    }

    val selectedProjectName =
        serverState.projectName
            ?: projectDirectory?.name
            ?: "No project selected"

    val remainingText = if (isRunning) {

        if (serverState.durationMillis == 0L) {

            "Unlimited"

        } else {

            val totalSeconds =
                (serverState.remainingMillis / 1000L)
                    .coerceAtLeast(0L)

            val remainingDays = totalSeconds / 86400L
            val remainingHours = (totalSeconds % 86400L) / 3600L
            val remainingMinutes = (totalSeconds % 3600L) / 60L
            val remainingSeconds = totalSeconds % 60L

            if (remainingDays > 0L) {
                "${remainingDays}d ${remainingHours}h ${remainingMinutes}m"
            } else {
                "${remainingHours}h ${remainingMinutes}m ${remainingSeconds}s"
            }
        }

    } else {
        "Not running"
    }

    fun selectPreset(
        label: String,
        day: String,
        hour: String,
        minute: String
    ) {
        selectedTimer = label
        days = day
        hours = hour
        minutes = minute
    }

    fun copyAddress(address: String) {

        val clipboard = context.getSystemService(
            Context.CLIPBOARD_SERVICE
        ) as ClipboardManager

        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                "NovaRun Server Address",
                address
            )
        )

        Toast.makeText(
            context,
            "Address copied",
            Toast.LENGTH_SHORT
        ).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090B10))
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {

        Text(
            text = "Local Server",
            color = Color.White,
            fontSize = 24.sp,
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Run your project on this Android device and share it over your local network.",
            color = Color(0xFF9AA6B5),
            fontSize = 14.sp
        )

        Spacer(Modifier.height(22.dp))

        // SERVER STATUS

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF171D28)
            )
        ) {

            Column(
                modifier = Modifier.padding(18.dp)
            ) {

                Text(
                    text = "SERVER STATUS",
                    color = Color(0xFF9AA6B5),
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = serverState.status.name,
                    color = statusColor,
                    fontSize = 22.sp
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = serverState.message,
                    color = Color.White,
                    fontSize = 14.sp
                )

                if (isRunning) {

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "TIME REMAINING",
                        color = Color(0xFF9AA6B5),
                        fontSize = 11.sp
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = remainingText,
                        color = Color(0xFF30D158),
                        fontSize = 22.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // PROJECT INFORMATION

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF171D28)
            )
        ) {

            Column(
                modifier = Modifier.padding(18.dp)
            ) {

                Text(
                    text = "PROJECT",
                    color = Color(0xFF9AA6B5),
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = selectedProjectName,
                    color = Color.White,
                    fontSize = 17.sp
                )

                Spacer(Modifier.height(18.dp))

                Text(
                    text = "LAN ADDRESS",
                    color = Color(0xFF9AA6B5),
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = serverState.lanUrl ?: "Not available",
                    color = Color(0xFF30D158),
                    fontSize = 16.sp
                )

                Spacer(Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        serverState.lanUrl?.let {
                            copyAddress(it)
                        }
                    },
                    enabled = isRunning &&
                        serverState.lanUrl != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Copy LAN Address",
                        color = Color.White
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "LOCAL ADDRESS",
                    color = Color(0xFF9AA6B5),
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = serverState.localUrl ?: "Not available",
                    color = Color(0xFF0A84FF),
                    fontSize = 16.sp
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "PORT",
                    color = Color(0xFF9AA6B5),
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = if (isRunning) "8080" else "Not active",
                    color = Color.White,
                    fontSize = 16.sp
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Open the LAN address in another phone's browser while both devices are connected to the same Wi-Fi or hotspot.",
                    color = Color(0xFF9AA6B5),
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Security: Other devices on the same network may access files in this selected project. Use trusted networks only.",
                    color = Color(0xFFFF9F0A),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // TIMER SETTINGS

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF171D28)
            )
        ) {

            Column(
                modifier = Modifier.padding(18.dp)
            ) {

                Text(
                    text = "SERVER TIMER",
                    color = Color.White,
                    fontSize = 16.sp
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Choose how long your server should run.",
                    color = Color(0xFF9AA6B5),
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "PRESET TIMER",
                    color = Color(0xFF9AA6B5),
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {

                    FilterChip(
                        selected = selectedTimer == "15m",
                        onClick = {
                            selectPreset("15m", "0", "0", "15")
                        },
                        label = { Text("15m") },
                        enabled = !isRunning && !isStarting
                    )

                    FilterChip(
                        selected = selectedTimer == "30m",
                        onClick = {
                            selectPreset("30m", "0", "0", "30")
                        },
                        label = { Text("30m") },
                        enabled = !isRunning && !isStarting
                    )

                    FilterChip(
                        selected = selectedTimer == "1h",
                        onClick = {
                            selectPreset("1h", "0", "1", "0")
                        },
                        label = { Text("1h") },
                        enabled = !isRunning && !isStarting
                    )
                }

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {

                    FilterChip(
                        selected = selectedTimer == "2h",
                        onClick = {
                            selectPreset("2h", "0", "2", "0")
                        },
                        label = { Text("2h") },
                        enabled = !isRunning && !isStarting
                    )

                    FilterChip(
                        selected = selectedTimer == "4h",
                        onClick = {
                            selectPreset("4h", "0", "4", "0")
                        },
                        label = { Text("4h") },
                        enabled = !isRunning && !isStarting
                    )

                    FilterChip(
                        selected = selectedTimer == "8h",
                        onClick = {
                            selectPreset("8h", "0", "8", "0")
                        },
                        label = { Text("8h") },
                        enabled = !isRunning && !isStarting
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "CUSTOM TIMER",
                    color = Color(0xFF9AA6B5),
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Enter your own time (maximum 7 days).",
                    color = Color(0xFF9AA6B5),
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(10.dp))

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    OutlinedTextField(
                        value = days,
                        onValueChange = {
                            days = it.filter(Char::isDigit).take(2)
                            selectedTimer = "Custom"
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isRunning && !isStarting,
                        label = { Text("Days") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF0A84FF),
                            unfocusedBorderColor = Color(0xFF596273),
                            focusedLabelColor = Color(0xFF0A84FF),
                            unfocusedLabelColor = Color(0xFF9AA6B5)
                        )
                    )

                    OutlinedTextField(
                        value = hours,
                        onValueChange = {
                            hours = it.filter(Char::isDigit).take(3)
                            selectedTimer = "Custom"
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isRunning && !isStarting,
                        label = { Text("Hours") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF0A84FF),
                            unfocusedBorderColor = Color(0xFF596273),
                            focusedLabelColor = Color(0xFF0A84FF),
                            unfocusedLabelColor = Color(0xFF9AA6B5)
                        )
                    )

                    OutlinedTextField(
                        value = minutes,
                        onValueChange = {
                            minutes = it.filter(Char::isDigit).take(3)
                            selectedTimer = "Custom"
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isRunning && !isStarting,
                        label = { Text("Minutes") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF0A84FF),
                            unfocusedBorderColor = Color(0xFF596273),
                            focusedLabelColor = Color(0xFF0A84FF),
                            unfocusedLabelColor = Color(0xFF9AA6B5)
                        )
                    )
                }

                if (
                    selectedTimer == "Custom" &&
                    !customTimerValid
                ) {

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Enter a valid time greater than zero, up to 7 days.",
                        color = Color(0xFFFF9F0A),
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(12.dp))

                FilterChip(
                    selected = isUnlimited,
                    onClick = {
                        selectedTimer = "Unlimited"
                    },
                    label = {
                        Text("Unlimited - Until I stop")
                    },
                    enabled = !isRunning && !isStarting
                )

                Text(
                    text = if (isUnlimited) {
                        "The server will keep running until you press Stop."
                    } else {
                        "The server will automatically stop when the timer ends."
                    },
                    color = Color(0xFF9AA6B5),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {

                val duration = if (isUnlimited) {
                    0L
                } else {
                    customDuration
                }

                projectDirectory?.let { project ->

                    LocalWebServerService.start(
                        context = context,
                        projectDirectory = project,
                        durationMillis = duration
                    )
                }
            },
            enabled = projectDirectory != null &&
                !isRunning &&
                !isStarting &&
                (
                    isUnlimited ||
                    customTimerValid
                ),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0A84FF)
            )
        ) {

            Text(
                text = if (isStarting) {
                    "Starting..."
                } else {
                    "Start Server"
                },
                modifier = Modifier.padding(vertical = 5.dp)
            )
        }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = {
                LocalWebServerService.stop(context)
            },
            enabled = isRunning || isStarting,
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                text = "Stop Server",
                modifier = Modifier.padding(vertical = 5.dp),
                color = Color.White
            )
        }

        Spacer(Modifier.height(16.dp))

        if (projectDirectory == null) {

            Text(
                text = "Select a project from Workspace before starting the server.",
                color = Color(0xFFFF9F0A),
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back", color = Color.White)
        }

        Spacer(Modifier.height(12.dp))
    }
}
