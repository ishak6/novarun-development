
package com.mangalocalrunnerplus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

private val WorkspaceBackground
    get() = NovaRunPalette.Background

private val WorkspaceCard
    get() = NovaRunPalette.Surface

private val WorkspaceMuted
    get() = NovaRunPalette.TextSecondary

private val WorkspaceBlue
    get() = NovaRunPalette.Cyan

@Composable
fun ProjectWorkspaceScreen(
    onProjectSelected: (File) -> Unit
) {
    val context = LocalContext.current

    var projects by remember { mutableStateOf(emptyList<File>()) }
    var searchText by remember { mutableStateOf("") }
    var projectToDelete by remember { mutableStateOf<File?>(null) }
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    fun refreshProjects() {
        projects = ProjectRepository.getProjects(context)
    }

    fun createProject() {
        val name = newProjectName.trim()

        when {
            name.isBlank() -> {
                message = "প্রজেক্টের নাম লিখুন।"
            }

            name.length > 50 -> {
                message = "প্রজেক্টের নাম ৫০ অক্ষরের মধ্যে রাখুন।"
            }

            !name.matches(Regex("[a-zA-Z0-9 _.-]+")) -> {
                message = "শুধু English letter, number, space, dot, underscore ও hyphen ব্যবহার করুন।"
            }

            else -> {
                val root = File(context.filesDir, "projects")
                if (!root.exists()) root.mkdirs()

                val project = File(root, name)

                if (project.exists()) {
                    message = "এই নামে একটি Project আগে থেকেই আছে।"
                } else {
                    val created = project.mkdirs()

                    if (created) {
                        File(project, "index.html").writeText(
                            """<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>$name</title>
</head>
<body>
    <h1>Welcome to $name</h1>
    <p>Your project is ready.</p>
</body>
</html>"""
                        )

                        showNewProjectDialog = false
                        newProjectName = ""
                        refreshProjects()
                        message = "নতুন Project তৈরি হয়েছে: $name"
                    } else {
                        message = "Project তৈরি করা যায়নি। আবার চেষ্টা করুন।"
                    }
                }
            }
        }
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Workspace",
                    color = NovaRunPalette.TextPrimary,
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

        Spacer(Modifier.height(14.dp))

        Button(
            onClick = {
                newProjectName = ""
                showNewProjectDialog = true
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = WorkspaceBlue,
                contentColor = WorkspaceBackground
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("New Project", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(14.dp))

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
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = WorkspaceBlue,
                unfocusedBorderColor = NovaRunPalette.Border,
                focusedTextColor = NovaRunPalette.TextPrimary,
                unfocusedTextColor = NovaRunPalette.TextPrimary,
                cursorColor = WorkspaceBlue
            )
        )

        Spacer(Modifier.height(16.dp))

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
                        color = NovaRunPalette.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        "Create a New Project or import a ZIP from Home.",
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

    if (showNewProjectDialog) {
        AlertDialog(
            onDismissRequest = { showNewProjectDialog = false },
            containerColor = WorkspaceCard,
            title = {
                Text(
                    "Create New Project",
                    color = NovaRunPalette.TextPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = newProjectName,
                    onValueChange = { newProjectName = it },
                    label = { Text("Project name") },
                    placeholder = { Text("MyWebsite") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = NovaRunPalette.TextPrimary,
                        unfocusedTextColor = NovaRunPalette.TextPrimary,
                        focusedBorderColor = WorkspaceBlue,
                        unfocusedBorderColor = NovaRunPalette.Border
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { createProject() }) {
                    Text("Create", color = WorkspaceBlue)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showNewProjectDialog = false }
                ) {
                    Text("Cancel", color = WorkspaceMuted)
                }
            }
        )
    }

    projectToDelete?.let { project ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            containerColor = WorkspaceCard,
            title = {
                Text(
                    "Delete project?",
                    color = NovaRunPalette.TextPrimary
                )
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
                    Text("Delete", color = NovaRunPalette.Error)
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

    message?.let { text ->
        AlertDialog(
            onDismissRequest = { message = null },
            containerColor = WorkspaceCard,
            title = {
                Text("NovaRun", color = NovaRunPalette.TextPrimary)
            },
            text = {
                Text(text, color = WorkspaceMuted)
            },
            confirmButton = {
                TextButton(onClick = { message = null }) {
                    Text("OK", color = WorkspaceBlue)
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
        shape = RoundedCornerShape(14.dp),
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
                    color = NovaRunPalette.TextPrimary,
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
                    tint = NovaRunPalette.Error
                )
            }
        }
    }
}
