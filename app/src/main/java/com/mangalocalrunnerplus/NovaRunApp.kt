
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
    var importMessage by remember { mutableStateOf<String?>(null) }
    var importSucceeded by remember { mutableStateOf(false) }
    var selectedProject by remember { mutableStateOf<File?>(null) }

    val launchZipImport = rememberZipImportLauncher { result ->
        result.fold(
            onSuccess = { project ->
                importSucceeded = true
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
        "Home",
        "Workspace",
        "Preview",
        "Runtimes",
        "Settings"
    )

    val icons = listOf(
        Icons.Default.Home,
        Icons.Default.Folder,
        Icons.Default.Visibility,
        Icons.Default.Language,
        Icons.Default.Settings
    )

    Scaffold(
        containerColor = Background,
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF11151D)
            ) {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                icons[index],
                                contentDescription = title
                            )
                        },
                        label = { Text(title) },
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
                    onImport = launchZipImport,
                    onWorkspace = { selectedTab = 1 },
                    onPreview = { selectedTab = 2 }
                )

                1 -> ProjectWorkspaceScreen(
                    onProjectSelected = { project ->
                        selectedProject = project
                    }
                )

                2 -> SimpleScreen(
                    title = "Preview",
                    description =
                        "Your local website preview will appear here.",
                    icon = Icons.Default.Visibility
                )

                3 -> RuntimeStatusRoute()

                else -> SimpleScreen(
                    title = "Settings",
                    description =
                        "Configure NovaRun preferences here.",
                    icon = Icons.Default.Settings
                )
            }
        }
    }

    if (selectedProject != null) {
        AlertDialog(
            onDismissRequest = { selectedProject = null },
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
                TextButton(
                    onClick = { selectedProject = null }
                ) {
                    Text("OK", color = Blue)
                }
            }
        )
    }

    if (importMessage != null) {
        AlertDialog(
            onDismissRequest = { importMessage = null },
            containerColor = CardColor,
            title = {
                Text(
                    if (importSucceeded) {
                        "Import Complete"
                    } else {
                        "Import Failed"
                    },
                    color = Color.White
                )
            },
            text = {
                Text(importMessage ?: "", color = Muted)
            },
            confirmButton = {
                TextButton(
                    onClick = { importMessage = null }
                ) {
                    Text("OK", color = Blue)
                }
            }
        )
    }
}

@Composable
private fun HomeScreen(
    onImport: () -> Unit,
    onWorkspace: () -> Unit,
    onPreview: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "NOVARUN",
                        color = Color.White,
                        fontSize = 23.sp,
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
                        .size(44.dp)
                        .background(CardColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Code,
                        contentDescription = "NovaRun",
                        tint = Blue
                    )
                }
            }
        }

        item {
            Column {
                Text(
                    "Build something",
                    color = Color.White,
                    fontSize = 29.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Run your projects locally.",
                    color = Muted,
                    fontSize = 15.sp
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CardColor
                )
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(
                                    Color(0xFF203D36),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Language,
                                contentDescription = null,
                                tint = Green
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column {
                            Text(
                                "LOCAL ENVIRONMENT",
                                color = Muted,
                                fontSize = 10.sp,
                                letterSpacing = 1.5.sp
                            )

                            Text(
                                "Ready to set up",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {
                        StatusItem("PROJECTS", "Local")
                        StatusItem("SERVERS", "0")
                        StatusItem("RUNTIME", "Setup")
                    }
                }
            }
        }

        item {
            Text(
                "QUICK ACTIONS",
                color = Muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionCard(
                    title = "Import ZIP",
                    subtitle = "Add an existing project",
                    icon = Icons.Default.UploadFile,
                    accent = Blue,
                    modifier = Modifier.weight(1f),
                    onClick = onImport
                )

                ActionCard(
                    title = "Workspace",
                    subtitle = "Browse project files",
                    icon = Icons.Default.Folder,
                    accent = Purple,
                    modifier = Modifier.weight(1f),
                    onClick = onWorkspace
                )
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPreview() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF111C2B)
                )
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Blue,
                        modifier = Modifier.size(28.dp)
                    )

                    Spacer(Modifier.width(14.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            "Live Preview",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "Preview your local website",
                            color = Muted,
                            fontSize = 12.sp
                        )
                    }

                    Text(
                        "OPEN →",
                        color = Blue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item {
            Text(
                "PROJECT WORKSPACE",
                color = Muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onWorkspace() },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CardColor
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = Blue,
                        modifier = Modifier.size(30.dp)
                    )

                    Spacer(Modifier.width(12.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            "Open Workspace",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "View your imported projects",
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

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun StatusItem(
    label: String,
    value: String
) {
    Column {
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
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
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
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardColor
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(
                icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(27.dp)
            )

            Spacer(Modifier.height(18.dp))

            Text(
                title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            Spacer(Modifier.height(4.dp))

            Text(
                subtitle,
                color = Muted,
                fontSize = 11.sp
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
            color = Color.White,
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
