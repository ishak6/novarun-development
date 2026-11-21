
package com.mangalocalrunnerplus

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

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
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }

    var backupTarget by remember { mutableStateOf<File?>(null) }
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        val project = backupTarget
        backupTarget = null
        if (uri != null && project != null) {
            scope.launch {
                val result = runCatching {
                    withContext(Dispatchers.IO) {
                        val archive = ProjectBackupManager.createBackup(project, context.cacheDir)
                        try {
                            val output = context.contentResolver.openOutputStream(uri, "w")
                                ?: throw IOException("Could not open the selected backup destination.")
                            output.use { destination ->
                                archive.inputStream().use { input -> input.copyTo(destination) }
                            }
                        } finally {
                            archive.delete()
                        }
                    }
                }
                message = result.fold(
                    onSuccess = { "Backup saved: ${project.name}.zip. Restore it from Workspace > Restore Backup / Import ZIP; NovaRun creates a separate project without overwriting the current one." },
                    onFailure = { "Backup failed: ${it.message ?: "Unknown error"}" }
                )
            }
        } else if (uri == null) {
            message = "Backup cancelled."
        }
    }

    var projects by remember { mutableStateOf(emptyList<File>()) }
    var searchText by remember { mutableStateOf("") }
    var projectToDelete by remember { mutableStateOf<File?>(null) }
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }

    fun refreshProjects() {
        projects = ProjectRepository.getProjects(context)
    }

    val launchRestoreBackup = rememberZipImportLauncher { result ->
        result.fold(
            onSuccess = { restored ->
                refreshProjects()
                message = "Backup restored as a separate project: ${restored.name}. Your existing projects were not overwritten."
            },
            onFailure = { error ->
                message = "Restore failed: ${error.message ?: "Could not import backup ZIP."}"
            }
        )
    }

    fun createProject() {
        val project = try {
            ProjectRepository.createProject(context, newProjectName)
        } catch (error: Exception) {
            message = error.message ?: "Project তৈরি করা যায়নি। আবার চেষ্টা করুন।"
            return
        }

        showNewProjectDialog = false
        newProjectName = ""
        refreshProjects()
        message = "নতুন Project তৈরি হয়েছে: ${project.name}"
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

        OutlinedButton(
            onClick = launchRestoreBackup,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = WorkspaceBlue)
        ) {
            Text("RESTORE BACKUP / IMPORT ZIP", fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(10.dp))

        Text(
            "Backup from a project card. Restore creates a separate project; existing files are never overwritten.",
            color = WorkspaceMuted,
            fontSize = 11.sp
        )

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
                        onBackup = {
                            backupTarget = project
                            backupLauncher.launch(project.name.replace(Regex("[^A-Za-z0-9._-]"), "_") + "_backup.zip")
                        },
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
    onBackup: () -> Unit,
    onDelete: () -> Unit
) {
    val size = ProjectRepository.formatSize(
        ProjectRepository.getProjectSize(project)
    )
    val analysis = remember(project.absolutePath, project.lastModified()) {
        ProjectZipAnalyzer.analyze(project)
    }

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
                Spacer(Modifier.height(4.dp))
                Text(
                    analysis.summary,
                    color = WorkspaceBlue,
                    fontSize = 11.sp
                )
                Text(
                    analysis.nextStep,
                    color = WorkspaceMuted,
                    fontSize = 10.sp,
                    maxLines = 2
                )
                Text(
                    when {
                        analysis.projectKind == "Node.js backend" -> "BACKEND READY • Start Node.js in Localhost"
                        analysis.previewReady -> "PREVIEW READY • Static output detected"
                        analysis.requiresBuild -> "BUILD REQUIRED • Runtime/build tool needed"
                        analysis.missingRuntimes.isNotEmpty() -> "RUNTIME REQUIRED • Cannot execute yet"
                        else -> "SOURCE DETECTED • Choose a supported run target"
                    },
                    color = when {
                        analysis.projectKind == "Node.js backend" -> NovaRunPalette.Success
                        analysis.previewReady -> NovaRunPalette.Success
                        analysis.requiresBuild || analysis.missingRuntimes.isNotEmpty() -> NovaRunPalette.Warning
                        else -> WorkspaceMuted
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                if (analysis.detectedLanguages.isNotEmpty()) {
                    Text(
                        "Languages: " + analysis.detectedLanguages.take(5).joinToString() +
                            if (analysis.detectedLanguages.size > 5) " +" + (analysis.detectedLanguages.size - 5) else "",
                        color = WorkspaceMuted,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
                if (analysis.missingRuntimes.isNotEmpty()) {
                    Text(
                        "Missing: " + analysis.missingRuntimes.joinToString(),
                        color = NovaRunPalette.Warning,
                        fontSize = 10.sp,
                        maxLines = 2
                    )
                }
                Text(
                    "Scanned files: " + analysis.totalFiles,
                    color = WorkspaceMuted,
                    fontSize = 9.sp
                )
                analysis.nodeProject?.let { node ->
                    Text(
                        node.packageManager + " • " + node.dependencyCount + " dependencies • lock: " + (node.lockfileName ?: "missing"),
                        color = WorkspaceMuted,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                    node.buildCommand?.let { command ->
                        Text("Build script: " + command, color = WorkspaceBlue, fontSize = 10.sp, maxLines = 1)
                    }
                    node.warnings.firstOrNull()?.let { warning ->
                        Text(warning, color = NovaRunPalette.Warning, fontSize = 9.sp, maxLines = 2)
                    }
                }
                analysis.phpProject?.let { php ->
                    Text(
                        "Composer • " + (php.framework ?: "PHP") + " • " + php.dependencyCount + " dependencies • lock: " + (php.lockfileName ?: "missing"),
                        color = WorkspaceMuted,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                    php.phpVersionConstraint?.let { version ->
                        Text("PHP requirement: " + version, color = WorkspaceBlue, fontSize = 10.sp, maxLines = 1)
                    }
                    php.entryPoint?.let { entry ->
                        Text("Entry point: " + entry, color = WorkspaceBlue, fontSize = 10.sp, maxLines = 1)
                    }
                    php.warnings.firstOrNull()?.let { warning ->
                        Text(warning, color = NovaRunPalette.Warning, fontSize = 9.sp, maxLines = 2)
                    }
                }
                TextButton(onClick = onBackup) {
                    Text("BACKUP ZIP", color = WorkspaceBlue, fontSize = 11.sp)
                }
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
