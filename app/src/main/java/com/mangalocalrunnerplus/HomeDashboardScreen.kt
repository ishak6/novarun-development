
package com.mangalocalrunnerplus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

@Composable
fun HomeDashboardScreen(
    onImport: () -> Unit,
    onWorkspace: () -> Unit,
    onPreview: () -> Unit,
    onLocalhost: () -> Unit
) {
    val context = LocalContext.current

    var projectCount by remember { mutableIntStateOf(0) }
    var projects by remember { mutableStateOf(emptyList<java.io.File>()) }

    val serverState by LocalServerServiceStateStore.state
        .collectAsState()

    val previewRuntimeCount =
        RuntimeCapabilityManager.getPreviewableLanguages().size

    LaunchedEffect(Unit) {
        while (true) {
            projects = ProjectRepository.getProjects(context)
            projectCount = projects.size
            delay(1500)
        }
    }

    val serverLabel = when (serverState.status) {
        LocalServerServiceStatus.RUNNING -> "Running"
        LocalServerServiceStatus.STARTING -> "Starting"
        LocalServerServiceStatus.ERROR -> "Error"
        LocalServerServiceStatus.STOPPED -> "Stopped"
    }

    val serverColor = when (serverState.status) {
        LocalServerServiceStatus.RUNNING -> Color(0xFF7CFC9A)
        LocalServerServiceStatus.STARTING -> Color(0xFFFFC857)
        LocalServerServiceStatus.ERROR -> Color(0xFFFF6B6B)
        LocalServerServiceStatus.STOPPED -> Color(0xFF9AA4B2)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaRunPalette.Background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NovaRunBrandLogo(
                    markSize = 46.dp,
                    showTagline = true
                )

                Text(
                    "BUILD • RUN • MANAGE",
                    color = NovaRunPalette.Lime,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        item {
            Column {
                Text(
                    "Build something",
                    color = NovaRunPalette.TextPrimary,
                    fontSize = 29.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Your local development dashboard.",
                    color = NovaRunPalette.TextSecondary,
                    fontSize = 14.sp
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = NovaRunPalette.Surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "ENVIRONMENT OVERVIEW",
                        color = NovaRunPalette.TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DashboardMetric(
                            label = "PROJECTS",
                            value = projectCount.toString(),
                            detail = "Saved locally",
                            accent = NovaRunPalette.Cyan
                        )

                        DashboardMetric(
                            label = "SERVER",
                            value = serverLabel,
                            detail = serverState.projectName ?: "No active project",
                            accent = serverColor
                        )

                        DashboardMetric(
                            label = "PREVIEW",
                            value = previewRuntimeCount.toString(),
                            detail = "Web technologies",
                            accent = NovaRunPalette.Lime
                        )
                    }

                    HorizontalDivider(
                        color = NovaRunPalette.TextSecondary.copy(alpha = 0.2f)
                    )

                    Text(
                        text = serverState.message,
                        color = NovaRunPalette.TextSecondary,
                        fontSize = 12.sp
                    )

                    if (serverState.status == LocalServerServiceStatus.RUNNING) {
                        Text(
                            text = serverState.url ?: "Server is running",
                            color = NovaRunPalette.Cyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        item {
            Text(
                "QUICK ACTIONS",
                color = NovaRunPalette.TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.3.sp
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardAction(
                    title = "Import ZIP",
                    subtitle = "Import a project",
                    icon = Icons.Default.UploadFile,
                    accent = NovaRunPalette.Cyan,
                    modifier = Modifier.weight(1f),
                    onClick = onImport
                )

                DashboardAction(
                    title = "Workspace",
                    subtitle = "Manage your files",
                    icon = Icons.Default.Folder,
                    accent = NovaRunPalette.Purple,
                    modifier = Modifier.weight(1f),
                    onClick = onWorkspace
                )
            }
        }

        item {
            DashboardWideAction(
                title = "Live Preview",
                subtitle = "Open and preview your website project",
                icon = Icons.Default.Visibility,
                accent = NovaRunPalette.Cyan,
                onClick = onPreview
            )
        }

        item {
            DashboardWideAction(
                title = "Localhost Control",
                subtitle = "Start or manage your local server",
                icon = Icons.Default.Language,
                accent = NovaRunPalette.Lime,
                onClick = onLocalhost
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "YOUR PROJECTS",
                    color = NovaRunPalette.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )

                TextButton(onClick = onWorkspace) {
                    Text("View all", color = NovaRunPalette.Cyan)
                }
            }
        }

        if (projects.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = NovaRunPalette.Surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            tint = NovaRunPalette.TextSecondary,
                            modifier = Modifier.size(34.dp)
                        )

                        Spacer(Modifier.height(10.dp))

                        Text(
                            "No projects yet",
                            color = NovaRunPalette.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "Create a project in Workspace or import a ZIP file.",
                            color = NovaRunPalette.TextSecondary,
                            fontSize = 12.sp
                        )

                        Spacer(Modifier.height(8.dp))

                        Button(onClick = onWorkspace) {
                            Text("Open Workspace")
                        }
                    }
                }
            }
        } else {
            items(
                items = projects.take(5),
                key = { it.absolutePath }
            ) { project ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onWorkspace() },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = NovaRunPalette.Surface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Code,
                            contentDescription = null,
                            tint = NovaRunPalette.Cyan
                        )

                        Spacer(Modifier.width(12.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                project.name,
                                color = NovaRunPalette.TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                ProjectRepository.formatSize(
                                    ProjectRepository.getProjectSize(project)
                                ),
                                color = NovaRunPalette.TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Text(
                            "OPEN",
                            color = NovaRunPalette.Cyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DashboardMetric(
    label: String,
    value: String,
    detail: String,
    accent: Color
) {
    Column(
        modifier = Modifier.widthIn(max = 110.dp)
    ) {
        Text(
            label,
            color = NovaRunPalette.TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(6.dp))

        Text(
            value,
            color = accent,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )

        Spacer(Modifier.height(3.dp))

        Text(
            detail,
            color = NovaRunPalette.TextSecondary,
            fontSize = 9.sp,
            maxLines = 2
        )
    }
}

@Composable
private fun DashboardAction(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = NovaRunPalette.Surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(27.dp)
            )

            Spacer(Modifier.height(14.dp))

            Text(
                title,
                color = NovaRunPalette.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Text(
                subtitle,
                color = NovaRunPalette.TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun DashboardWideAction(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = NovaRunPalette.Surface
        )
    ) {
        Row(
            modifier = Modifier.padding(17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(29.dp)
            )

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = NovaRunPalette.TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    subtitle,
                    color = NovaRunPalette.TextSecondary,
                    fontSize = 11.sp
                )
            }

            Text(
                "→",
                color = accent,
                fontSize = 22.sp
            )
        }
    }
}
