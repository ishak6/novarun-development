
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RuntimeConsoleScreen() {

    val logs by RuntimeConsoleLogger.logEntries.collectAsState()

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
                text = "Runtime Console",
                color = Color.White,
                fontSize = 23.sp,
                style = MaterialTheme.typography.headlineSmall
            )

            Button(
                onClick = {
                    RuntimeConsoleLogger.clear()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF202938)
                )
            ) {
                Text("Clear")
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "${logs.size} log entries",
            color = Color(0xFF9AA6B5),
            fontSize = 12.sp
        )

        Spacer(Modifier.height(14.dp))

        if (logs.isEmpty()) {

            Text(
                text = "No console output yet.",
                color = Color(0xFF9AA6B5)
            )

        } else {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                items(
                    items = logs,
                    key = { it.id }
                ) { entry ->

                    val logColor = when (entry.level) {

                        RuntimeLogLevel.INFO ->
                            Color(0xFF0A84FF)

                        RuntimeLogLevel.OUTPUT ->
                            Color(0xFF30D158)

                        RuntimeLogLevel.WARNING ->
                            Color(0xFFFF9F0A)

                        RuntimeLogLevel.ERROR ->
                            Color(0xFFFF453A)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF171D28)
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {

                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {

                            Text(
                                text = "${RuntimeConsoleLogger.formatTimestamp(entry.timestamp)}  ${entry.level}",
                                color = logColor,
                                fontSize = 11.sp
                            )

                            Spacer(Modifier.height(5.dp))

                            Text(
                                text = entry.message,
                                color = Color(0xFFF5F7FA),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
