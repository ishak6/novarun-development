
package com.mangalocalrunnerplus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility

private val Background = Color(0xFF090B10)
private val CardColor = Color(0xFF171D28)
private val Muted = Color(0xFF9AA6B5)
private val Blue = Color(0xFF0A84FF)
private val Purple = Color(0xFF8B7CFF)
private val Green = Color(0xFF30D158)

@Composable
fun NovaRunApp() {
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf("Home", "Workspace", "Preview", "Settings")

    Scaffold(
        containerColor = Background,
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF11151D)
            ) {
                val icons = listOf(
                    Icons.Default.Home,
                    Icons.Default.Folder,
                    Icons.Default.Visibility,
                    Icons.Default.Settings
                )

                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = icons[index],
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
        when (selectedTab) {
            0 -> HomeScreen(
                modifier = Modifier.padding(padding),
                onWorkspace = { selectedTab = 1 },
                onPreview = { selectedTab = 2 }
            )
            1 -> SimpleScreen(
                "Workspace",
                "Your project workspace will appear here.",
                Icons.Default.Folder,
                Modifier.padding(padding)
            )
            2 -> SimpleScreen(
                "Preview",
                "Your local website preview will appear here.",
                Icons.Default.Visibility,
                Modifier.padding(padding)
            )
            else -> SimpleScreen(
                "Settings",
                "Configure NovaRun preferences here.",
                Icons.Default.Settings,
                Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun HomeScreen(
    modifier: Modifier = Modifier,
    onWorkspace: () -> Unit,
    onPreview: () -> Unit
) {
    LazyColumn(
        modifier = modifier
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
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
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
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatusItem("PROJECTS", "0")
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
                    onClick = onWorkspace
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
                "RECENT PROJECTS",
                color = Muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CardColor
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
                        tint = Muted,
                        modifier = Modifier.size(34.dp)
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        "No projects yet",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        "Import a ZIP project to get started.",
                        color = Muted,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun StatusItem(label: String, value: String) {
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
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
