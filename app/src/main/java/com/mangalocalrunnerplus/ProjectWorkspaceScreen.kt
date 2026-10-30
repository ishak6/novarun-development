
package com.mangalocalrunnerplus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

private val WorkspaceBackground = Color(0xFF090B10)
private val WorkspaceCard = Color(0xFF171D28)
private val WorkspaceMuted = Color(0xFF9AA6B5)
private val WorkspaceBlue = Color(0xFF0A84FF)

@Composable
fun ProjectWorkspaceScreen(
    onProjectSelected: (File) -> Unit
) {
    val context = LocalContext.current

    var projects by remember { mutableStateOf(emptyList<File>()) }
    var searchText by remember { mutableStateOf("") }
    var projectToDelete by remember { mutableStateOf<File?>(null) }

    fun refreshProjects() {
        projects = ProjectRepository.getProjects(context)
    }

    LaunchedEffect(Unit) {
        refreshProjects()
    }

    val filteredProjects = projects.filter {
        it.name.contains(searchText, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WorkspaceBackground)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Workspace",
                    color = Color.White,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "${projects.size} local projects",
                    color = WorkspaceMuted,
                    fontSize = 13.sp
                )
            }

            IconButton(onClick = { refreshProjects() }) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh projects",
                    tint = WorkspaceBlue
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("Search projects", color = WorkspaceMuted)
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = WorkspaceMuted
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = WorkspaceBlue,
                unfocusedBorderColor = WorkspaceCard,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = WorkspaceBlue
            )
        )

        Spacer(Modifier.height(20.dp))

        if (filteredProjects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = WorkspaceMuted,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        if (projects.isEmpty()) {
                            "No projects yet"
                        } else {
                            "No matching projects"
                        },
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        "Import a ZIP project from Home.",
                        color = WorkspaceMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = filteredProjects,
                    key = { it.absolutePath }
                ) { project ->
                    ProjectCard(
                        project = project,
                        onOpen = { onProjectSelected(project) },
                        onDelete = { projectToDelete = project }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
    }

    projectToDelete?.let { project ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            containerColor = WorkspaceCard,
            title = {
                Text("Delete project?", color = Color.White)
            },
            text = {
                Text(
                    "This permanently deletes ${project.name} from NovaRun.",
                    color = WorkspaceMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        ProjectRepository.deleteProject(
                            context,
                            project.name
                        )
                        projectToDelete = null
                        refreshProjects()
                    }
                ) {
                    Text("Delete", color = Color(0xFFFF453A))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { projectToDelete = null }
                ) {
                    Text("Cancel", color = WorkspaceBlue)
                }
            }
        )
    }
}

@Composable
private fun ProjectCard(
    project: File,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val size = ProjectRepository.formatSize(
        ProjectRepository.getProjectSize(project)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = WorkspaceCard
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = WorkspaceBlue,
                modifier = Modifier.size(32.dp)
            )

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    project.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    size,
                    color = WorkspaceMuted,
                    fontSize = 11.sp
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete project",
                    tint = Color(0xFFFF453A)
                )
            }
        }
    }
}
