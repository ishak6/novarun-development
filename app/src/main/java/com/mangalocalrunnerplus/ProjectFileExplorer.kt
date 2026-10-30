
package com.mangalocalrunnerplus

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

private val ExplorerBackground = Color(0xFF090B10)
private val ExplorerCard = Color(0xFF171D28)
private val ExplorerBlue = Color(0xFF0A84FF)
private val ExplorerMuted = Color(0xFF9AA6B5)

@Composable
fun ProjectFileExplorer(
    projectRoot: File,
    onFileSelected: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentDirectory by remember(projectRoot) {
        mutableStateOf(projectRoot)
    }

    var files by remember {
        mutableStateOf<List<File>>(emptyList())
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    fun refreshFiles() {
        try {
            val root = projectRoot.canonicalFile
            val current = currentDirectory.canonicalFile

            if (current != root && !current.path.startsWith(root.path + File.separator)) {
                currentDirectory = root
                errorMessage = "Invalid folder location."
                return
            }

            if (!current.exists() || !current.isDirectory) {
                errorMessage = "This folder is not available."
                files = emptyList()
                return
            }

            files = current.listFiles()
                ?.filter { it.exists() }
                ?.sortedWith(
                    compareBy<File> { !it.isDirectory }
                        .thenBy { it.name.lowercase() }
                )
                ?: emptyList()

            errorMessage = null
        } catch (exception: Exception) {
            files = emptyList()
            errorMessage = exception.message ?: "Could not read this folder."
        }
    }

    LaunchedEffect(currentDirectory) {
        refreshFiles()
    }

    val canGoBack = currentDirectory.canonicalPath != projectRoot.canonicalPath

    BackHandler(enabled = canGoBack) {
        currentDirectory.parentFile?.let { parent ->
            currentDirectory = parent
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ExplorerBackground)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (canGoBack) {
                            currentDirectory.parentFile?.let {
                                currentDirectory = it
                            }
                        }
                    },
                    enabled = canGoBack
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Go back",
                        tint = if (canGoBack) Color.White else ExplorerMuted
                    )
                }

                Column {
                    Text(
                        "File Explorer",
                        color = Color.White,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        projectRoot.name,
                        color = ExplorerMuted,
                        fontSize = 12.sp
                    )
                }
            }

            IconButton(onClick = { refreshFiles() }) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = ExplorerBlue
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = ExplorerCard
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = currentDirectory.relativeTo(projectRoot).path
                    .ifEmpty { "Project Root" },
                color = ExplorerMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(14.dp)
            )
        }

        Spacer(Modifier.height(14.dp))

        errorMessage?.let { message ->
            Text(
                message,
                color = Color(0xFFFF453A),
                fontSize = 13.sp
            )
        }

        if (files.isEmpty() && errorMessage == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "This folder is empty",
                    color = ExplorerMuted,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = files,
                    key = { it.absolutePath }
                ) { file ->
                    FileExplorerItem(
                        file = file,
                        onClick = {
                            if (file.isDirectory) {
                                currentDirectory = file
                            } else {
                                onFileSelected(file)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FileExplorerItem(
    file: File,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = ExplorerCard
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 13.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (file.isDirectory) {
                    Icons.Default.Folder
                } else {
                    Icons.Default.InsertDriveFile
                },
                contentDescription = null,
                tint = if (file.isDirectory) {
                    ExplorerBlue
                } else {
                    ExplorerMuted
                },
                modifier = Modifier.size(25.dp)
            )

            Spacer(Modifier.width(13.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    file.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    if (file.isDirectory) {
                        "Folder"
                    } else {
                        "${file.length()} bytes"
                    },
                    color = ExplorerMuted,
                    fontSize = 11.sp
                )
            }

            Text(
                if (file.isDirectory) "›" else "OPEN",
                color = ExplorerBlue,
                fontSize = if (file.isDirectory) 24.sp else 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
