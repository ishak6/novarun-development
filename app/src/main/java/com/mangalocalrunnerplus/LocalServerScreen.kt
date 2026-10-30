
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
fun LocalServerScreen(
    projectDirectory: File?,
    onBack: () -> Unit = {}
) {

    val controller = remember {
        LocalWebServerController()
    }

    val serverState by controller.state.collectAsState()

    DisposableEffect(controller) {
        onDispose {
            controller.stop()
        }
    }

    val statusColor = when (serverState.status) {

        LocalServerStatus.RUNNING ->
            Color(0xFF30D158)

        LocalServerStatus.STARTING ->
            Color(0xFFFF9F0A)

        LocalServerStatus.ERROR ->
            Color(0xFFFF453A)

        LocalServerStatus.STOPPED ->
            Color(0xFF9AA6B5)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090B10))
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
            text = "Run your project on this Android device.",
            color = Color(0xFF9AA6B5),
            fontSize = 14.sp
        )

        Spacer(Modifier.height(24.dp))

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
            }
        }

        Spacer(Modifier.height(16.dp))

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
                    text = serverState.projectName
                        ?: projectDirectory?.name
                        ?: "No project selected",
                    color = Color.White,
                    fontSize = 17.sp
                )

                Spacer(Modifier.height(18.dp))

                Text(
                    text = "LOCAL ADDRESS",
                    color = Color(0xFF9AA6B5),
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = serverState.url ?: "Not available",
                    color = Color(0xFF0A84FF),
                    fontSize = 16.sp
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "This address is accessible only from this device.",
                    color = Color(0xFF9AA6B5),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        Button(
            onClick = {
                projectDirectory?.let {
                    controller.start(it)
                }
            },
            enabled = projectDirectory != null &&
                serverState.status != LocalServerStatus.RUNNING,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0A84FF)
            )
        ) {

            Text(
                text = "Start Server",
                modifier = Modifier.padding(vertical = 5.dp)
            )
        }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = {
                controller.stop()
            },
            enabled = serverState.status == LocalServerStatus.RUNNING ||
                serverState.status == LocalServerStatus.STARTING,
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                text = "Stop Server",
                modifier = Modifier.padding(vertical = 5.dp),
                color = Color.White
            )
        }

        Spacer(Modifier.height(18.dp))

        if (projectDirectory == null) {

            Text(
                text = "Select a project from Workspace before starting the server.",
                color = Color(0xFFFF9F0A),
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.weight(1f))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back", color = Color.White)
        }
    }
}
