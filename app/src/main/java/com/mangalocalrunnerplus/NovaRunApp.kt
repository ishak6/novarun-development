
package com.mangalocalrunnerplus

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

private val Background
    get() = NovaRunPalette.Background

private val CardColor
    get() = NovaRunPalette.Surface

private val Muted
    get() = NovaRunPalette.TextSecondary

private val Blue
    get() = NovaRunPalette.Cyan

@Composable
fun NovaRunApp() {
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var importMessage by remember { mutableStateOf<String?>(null) }
    var importSucceeded by remember { mutableStateOf(false) }
    var selectedProject by remember { mutableStateOf<File?>(null) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var showProjectDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    val launchZipImport = rememberZipImportLauncher { result ->
        result.fold(
            onSuccess = { project ->
                importSucceeded = true
                selectedProject = project
                importMessage =
                    "Project imported successfully:\n${project.name}"
                selectedTab = 1
            },
            onFailure = { error ->
                importSucceeded = false
                importMessage =
                    error.message ?: "Could not import ZIP project."
            }
        )
    }

    BackHandler {
        when {
            importMessage != null -> {
                importMessage = null
            }

            showProjectDialog -> {
                showProjectDialog = false
            }

            showExitDialog -> {
                showExitDialog = false
            }

            selectedTab == 10 -> {
                selectedTab = 9
            }

            selectedTab == 9 -> {
                selectedTab = 1
            }

            selectedTab == 2 || selectedTab == 3 -> {
                selectedTab = 1
            }

            selectedTab in 5..8 -> {
                selectedTab = 4
            }

            selectedTab == 1 || selectedTab == 4 -> {
                selectedTab = 0
            }

            selectedTab == 0 -> {
                showExitDialog = true
            }

            else -> {
                selectedTab = 0
            }
        }
    }

    val tabs = listOf(
        "Home",
        "Workspace",
        "Preview",
        "Localhost",
        "More"
    )

    val icons = listOf(
        Icons.Default.Home,
        Icons.Default.Folder,
        Icons.Default.Visibility,
        Icons.Default.Language,
        Icons.Default.MoreHoriz
    )

    Scaffold(
        containerColor = Background,
        bottomBar = {
            NavigationBar(
                containerColor = NovaRunPalette.Surface
            ) {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                        },
                        icon = {
                            Icon(
                                icons[index],
                                contentDescription = title
                            )
                        },
                        label = {
                            Text(title)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Blue,
                            selectedTextColor = Blue,
                            unselectedIconColor = Muted,
                            unselectedTextColor = Muted,
                            indicatorColor =
                                NovaRunPalette.SurfaceElevated
                        )
                    )
                }
            }
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {

                0 -> HomeDashboardScreen(
                    onImport = launchZipImport,
                    onWorkspace = {
                        selectedTab = 1
                    },
                    onPreview = {
                        selectedTab = 2
                    },
                    onLocalhost = {
                        selectedTab = 3
                    }
                )

                1 -> ProjectWorkspaceScreen(
                    onProjectSelected = { project ->
                        selectedProject = project
                        showProjectDialog = true
                    }
                )

                2 -> {
                    val project = selectedProject

                    if (project != null) {
                        WebPreviewScreen(
                            projectDirectory = project,
                            onBack = {
                                selectedTab = 1
                            }
                        )
                    } else {
                        SimpleScreen(
                            title = "No Project Selected",
                            description =
                                "Open Workspace and select a project before starting Preview.",
                            icon = Icons.Default.Visibility
                        )
                    }
                }

                3 -> {
                    NovaRunLocalhostScreen(
                        project = selectedProject
                    )
                }

                4 -> MoreScreen(
                    onRuntimes = {
                        selectedTab = 5
                    },
                    onConsole = {
                        selectedTab = 6
                    },
                    onTools = {
                        selectedTab = 7
                    },
                    onSettings = {
                        selectedTab = 8
                    }
                )

                5 -> RuntimeStatusRoute()

                6 -> RuntimeConsoleScreen()

                7 -> DeveloperToolsScreen()

                8 -> NovaRunSettingsScreen()

                9 -> {
                    val project = selectedProject

                    if (project != null) {
                        ProjectFileExplorer(
                            projectRoot = project,
                            onFileSelected = { file ->
                                selectedFile = file
                                selectedTab = 10
                            }
                        )
                    } else {
                        SimpleScreen(
                            title = "No Project Selected",
                            description =
                                "Select a project before opening File Explorer.",
                            icon = Icons.Default.Folder
                        )
                    }
                }

                10 -> {
                    val file = selectedFile

                    if (
                        file != null &&
                        file.exists() &&
                        file.isFile
                    ) {
                        CodeEditorScreen(
                            file = file,
                            onBack = {
                                selectedTab = 9
                            }
                        )
                    } else {
                        SimpleScreen(
                            title = "File Not Available",
                            description =
                                "The selected file no longer exists.",
                            icon = Icons.Default.Code
                        )
                    }
                }

                else -> SimpleScreen(
                    title = "Unknown Screen",
                    description =
                        "This screen is not available.",
                    icon = Icons.Default.Settings
                )
            }
        }
    }

    if (showProjectDialog && selectedProject != null) {
        AlertDialog(
            onDismissRequest = {
                showProjectDialog = false
            },
            containerColor = CardColor,
            title = {
                Text(
                    "Selected Project",
                    color = NovaRunPalette.TextPrimary
                )
            },
            text = {
                Text(
                    selectedProject?.name ?: "",
                    color = Muted
                )
            },
            confirmButton = {
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    TextButton(
                        onClick = {
                            showProjectDialog = false
                            selectedTab = 9
                        }
                    ) {
                        Text("Open Files", color = Blue)
                    }

                    TextButton(
                        onClick = {
                            showProjectDialog = false
                            selectedTab = 2
                        }
                    ) {
                        Text("Preview", color = Blue)
                    }

                    TextButton(
                        onClick = {
                            showProjectDialog = false
                            selectedTab = 3
                        }
                    ) {
                        Text("Localhost", color = Blue)
                    }

                    TextButton(
                        onClick = {
                            showProjectDialog = false
                        }
                    ) {
                        Text("OK", color = Blue)
                    }
                }
            }
        )
    }

    if (importMessage != null) {
        AlertDialog(
            onDismissRequest = {
                importMessage = null
            },
            containerColor = CardColor,
            title = {
                Text(
                    if (importSucceeded) {
                        "Import Complete"
                    } else {
                        "Import Failed"
                    },
                    color = NovaRunPalette.TextPrimary
                )
            },
            text = {
                Text(
                    importMessage ?: "",
                    color = Muted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        importMessage = null
                    }
                ) {
                    Text("OK", color = Blue)
                }
            }
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = {
                showExitDialog = false
            },
            containerColor = CardColor,
            title = {
                Text(
                    "Exit NovaRun?",
                    color = NovaRunPalette.TextPrimary
                )
            },
            text = {
                Text(
                    "Are you sure you want to close NovaRun?",
                    color = Muted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        (context as? Activity)?.finish()
                    }
                ) {
                    Text(
                        "Exit",
                        color = Color(0xFFFF6B6B)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                    }
                ) {
                    Text("Cancel", color = Blue)
                }
            }
        )
    }
}

@Composable
private fun MoreScreen(
    onRuntimes: () -> Unit,
    onConsole: () -> Unit,
    onTools: () -> Unit,
    onSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "More",
            color = NovaRunPalette.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Additional NovaRun features",
            color = Muted,
            fontSize = 13.sp
        )

        MoreAction(
            title = "Runtimes",
            subtitle = "Runtime availability and status",
            icon = Icons.Default.Language,
            onClick = onRuntimes
        )

        MoreAction(
            title = "Console",
            subtitle = "View runtime console",
            icon = Icons.Default.Terminal,
            onClick = onConsole
        )

        MoreAction(
            title = "Developer Tools",
            subtitle = "JavaScript, SQLite and API Tester",
            icon = Icons.Default.Code,
            onClick = onTools
        )

        MoreAction(
            title = "Settings",
            subtitle = "Configure NovaRun preferences",
            icon = Icons.Default.Settings,
            onClick = onSettings
        )
    }
}

@Composable
private fun MoreAction(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardColor
        )
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Blue,
                modifier = Modifier.size(28.dp)
            )

            Spacer(Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    color = NovaRunPalette.TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    subtitle,
                    color = Muted,
                    fontSize = 12.sp
                )
            }

            Text(
                "→",
                color = Blue,
                fontSize = 22.sp
            )
        }
    }
}

@Composable
private fun SimpleScreen(
    title: String,
    description: String,
    icon: ImageVector
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Blue,
            modifier = Modifier.size(48.dp)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            title,
            color = NovaRunPalette.TextPrimary,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            description,
            color = Muted,
            fontSize = 14.sp
        )
    }
}
