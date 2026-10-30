
package com.mangalocalrunnerplus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

private val Background = Color(0xFF090B10)
private val CardColor = Color(0xFF171D28)
private val Muted = Color(0xFF9AA6B5)
private val Blue = Color(0xFF0A84FF)
private val Purple = Color(0xFF8B7CFF)
private val Green = Color(0xFF30D158)

@Composable
fun NovaRunApp() {

    var selectedTab by remember { mutableIntStateOf(0) }

    var importMessage by remember {
        mutableStateOf<String?>(null)
    }

    var importSucceeded by remember {
        mutableStateOf(false)
    }

    var selectedProject by remember {
        mutableStateOf<File?>(null)
    }

    var showProjectDialog by remember {
        mutableStateOf(false)
    }

    val serverState by LocalServerServiceStateStore.state.collectAsState()

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

    val tabs = listOf(
        "Home" to Icons.Default.Home,
        "Workspace" to Icons.Default.Folder,
        "Preview" to Icons.Default.Visibility,
        "Localhost" to Icons.Default.Language,
        "Runtimes" to Icons.Default.Code,
        "Tools" to Icons.Default.Terminal,
        "Settings" to Icons.Default.Settings
    )

    Scaffold(
        containerColor = Background,
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF11151D)
            ) {
                tabs.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                        },
                        icon = {
                            Icon(
                                item.second,
                                contentDescription = item.first
                            )
                        },
                        label = {
                            Text(
                                item.first,
                                fontSize = 9.sp,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Blue,
                            selectedTextColor = Blue,
                            unselectedIconColor = Muted,
                            unselectedTextColor = Muted,
                            indicatorColor = Color(0xFF202938)
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

                0 -> HomeScreen(
                    selectedProject = selectedProject,
                    serverStatus = serverState.status.name,
                    onImport = launchZipImport,
                    onWorkspace = { selectedTab = 1 },
                    onPreview = { selectedTab = 2 },
                    onLocalhost = { selectedTab = 3 },
                    onRuntimes = { selectedTab = 4 },
                    onTools = { selectedTab = 5 },
                    onSettings = { selectedTab = 6 }
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
                            onBack = { selectedTab = 1 }
                        )
                    } else {
                        SimpleScreen(
                            title = "No Project Selected",
                            description = "Open Workspace and select a project before starting Preview.",
                            icon = Icons.Default.Visibility
                        )
                    }
                }

                3 -> NovaRunLocalhostScreen(
                    project = selectedProject
                )

                4 -> RuntimeStatusRoute()

                5 -> DeveloperToolsScreen()

                6 -> NovaRunSettingsScreen()

                else -> SimpleScreen(
                    title = "Unknown Screen",
                    description = "This screen is not available.",
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
                Text("Selected Project", color = Color.White)
            },
            text = {
                Text(
                    selectedProject?.name ?: "",
                    color = Muted
                )
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.End
                ) {
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
                    if (importSucceeded) "Import Complete"
                    else "Import Failed",
                    color = Color.White
                )
            },
            text = {
                Text(importMessage ?: "", color = Muted)
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
}

@Composable
private fun HomeScreen(
    selectedProject: File?,
    serverStatus: String,
    onImport: () -> Unit,
    onWorkspace: () -> Unit,
    onPreview: () -> Unit,
    onLocalhost: () -> Unit,
    onRuntimes: () -> Unit,
    onTools: () -> Unit,
    onSettings: () -> Unit
) {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "NOVARUN",
                        color = Color.White,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "LOCAL PROJECT RUNNER",
                        color = Muted,
                        fontSize = 10.sp,
                        letterSpacing = 2.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(CardColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Code,
                        contentDescription = "NovaRun",
                        tint = Blue,
                        modifier = Modifier.size(27.dp)
                    )
                }
            }
        }

        item {
            Column {
                Text(
                    "Build something.",
                    color = Color.White,
                    fontSize = 29.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Code, run and preview projects on your phone.",
                    color = Muted,
                    fontSize = 14.sp
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CardColor
                )
            ) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Language,
                            contentDescription = null,
                            tint = Green,
                            modifier = Modifier.size(25.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "LOCAL ENVIRONMENT",
                                color = Muted,
                                fontSize = 10.sp,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                "NovaRun Control Panel",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFF303847))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatusItem(
                            "PROJECT",
                            selectedProject?.name ?: "None"
                        )
                        StatusItem(
                            "SERVER",
                            serverStatus
                        )
                        StatusItem(
                            "BUILD",
                            "Debug"
                        )
                    }
                }
            }
        }

        item {
            SectionTitle("QUICK ACTIONS")
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionCard(
                    title = "Import ZIP",
                    subtitle = "Add project",
                    icon = Icons.Default.UploadFile,
                    accent = Blue,
                    modifier = Modifier.weight(1f),
                    onClick = onImport
                )

                ActionCard(
                    title = "Workspace",
                    subtitle = "Project files",
                    icon = Icons.Default.Folder,
                    accent = Purple,
                    modifier = Modifier.weight(1f),
                    onClick = onWorkspace
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionCard(
                    title = "Localhost",
                    subtitle = "Start / Stop server",
                    icon = Icons.Default.Language,
                    accent = Green,
                    modifier = Modifier.weight(1f),
                    onClick = onLocalhost
                )

                ActionCard(
                    title = "Live Preview",
                    subtitle = "Open website",
                    icon = Icons.Default.Visibility,
                    accent = Blue,
                    modifier = Modifier.weight(1f),
                    onClick = onPreview
                )
            }
        }

        item {
            SectionTitle("DEVELOPMENT TOOLS")
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionCard(
                    title = "Runtimes",
                    subtitle = "Runtime status",
                    icon = Icons.Default.Code,
                    accent = Purple,
                    modifier = Modifier.weight(1f),
                    onClick = onRuntimes
                )

                ActionCard(
                    title = "Developer Tools",
                    subtitle = "JS / SQLite / API",
                    icon = Icons.Default.Terminal,
                    accent = Blue,
                    modifier = Modifier.weight(1f),
                    onClick = onTools
                )
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSettings() },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF20202F)
                )
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = Purple,
                        modifier = Modifier.size(28.dp)
                    )

                    Spacer(Modifier.width(14.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            "App Settings",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Appearance, editor and server preferences",
                            color = Muted,
                            fontSize = 11.sp
                        )
                    }

                    Text("OPEN  →", color = Blue, fontSize = 11.sp)
                }
            }
        }

        item {
            if (selectedProject == null) {
                Text(
                    "Tip: Import a ZIP project or open Workspace and select a project before running its server.",
                    color = Color(0xFFFF9F0A),
                    fontSize = 12.sp
                )
            } else {
                Text(
                    "Selected project: ${selectedProject.name}",
                    color = Green,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        color = Muted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp
    )
}

@Composable
private fun StatusItem(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier.widthIn(max = 110.dp)
    ) {
        Text(
            label,
            color = Muted,
            fontSize = 9.sp,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(5.dp))
        Text(
            value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable {
            onClick()
        },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardColor
        )
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(14.dp)
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
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(4.dp))

            Text(
                subtitle,
                color = Muted,
                fontSize = 10.sp
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
            color 
