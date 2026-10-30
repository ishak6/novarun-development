package com.mangalocalrunnerplus

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.NoteAdd
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
private val ExplorerRed = Color(0xFFFF453A)

@Composable
fun ProjectFileExplorer(
    projectRoot: File,
    onFileSelected: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentDirectory by remember(projectRoot) {
        mutableStateOf(projectRoot)
    }
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var dialogMode by remember { mutableStateOf<String?>(null) }
    var inputName by remember { mutableStateOf("") }
    var targetFile by remember { mutableStateOf<File?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }

    fun isInsideProject(file: File): Boolean {
        return try {
            val root = projectRoot.canonicalFile
            val candidate = file.canonicalFile
            candidate == root ||
                candidate.path.startsWith(root.path + File.separator)
        } catch (_: Exception) {
            false
        }
    }

    fun refreshFiles() {
        try {
            if (!isInsideProject(currentDirectory)) {
                currentDirectory = projectRoot
                errorMessage = "Invalid folder location."
                return
            }

            if (!currentDirectory.exists() || !currentDirectory.isDirectory) {
                files = emptyList()
                errorMessage = "This folder is not available."
                return
            }

            files = currentDirectory.listFiles()
                ?.filter { it.exists() }
                ?.sortedWith(
                    compareBy<File> { !it.isDirectory }
                        .thenBy { it.name.lowercase() }
                ) ?: emptyList()

            errorMessage = null
        } catch (exception: Exception) {
            files = emptyList()
            errorMessage = exception.message ?: "Could not read this folder."
        }
    }

    LaunchedEffect(currentDirectory, refreshKey) {
        refreshFiles()
    }

    val canGoBack = currentDirectory.canonicalPath !=
        projectRoot.canonicalPath

    BackHandler(enabled = canGoBack) {
        currentDirectory.parentFile?.let {
            currentDirectory = it
        }
    }

    fun showCreateFile() {
        inputName = ""
        targetFile = null
        dialogMode = "create_file"
    }

    fun showCreateFolder() {
        inputName = ""
        targetFile = null
        dialogMode = "create_folder"
    }

    fun showRename(file: File) {
        inputName = file.name
        targetFile = file
        dialogMode = "rename"
    }

    fun showDelete(file: File) {
        targetFile = file
        dialogMode = "delete"
    }

    fun performAction() {
        val name = inputName.trim()

        if (dialogMode != "delete") {
            if (name.isBlank()) {
                errorMessage = "Name cannot be empty."
                return
            }

            if (
                name == "." ||
                name == ".." ||
                name.contains("/") ||
                name.contains("\\") ||
                name.contains(":")
            ) {
                errorMessage = "Enter a valid file or folder name."
                return
            }
        }

        try {
            when (dialogMode) {
                "create_file" -> {
                    val newFile = File(currentDirectory, name)

                    if (!isInsideProject(newFile)) {
                        errorMessage = "Invalid file location."
                        return
                    }

                    if (newFile.exists()) {
                        errorMessage = "A file or folder with this name already exists."
                        return
                    }

                    if (!newFile.createNewFile()) {
                        errorMessage = "Could not create the file."
                        return
                    }
                }

                "create_folder" -> {
                    val newFolder = File(currentDirectory, name)

                    if (!isInsideProject(newFolder)) {
                        errorMessage = "Invalid folder location."
                        return
                    }

                    if (newFolder.exists()) {
                        errorMessage = "A file or folder with this name already exists."
                        return
                    }

                    if (!newFolder.mkdir()) {
                        errorMessage = "Could not create the folder."
                        return
                    }
                }

                "rename" -> {
                    val oldFile = targetFile ?: return
                    val renamedFile = File(oldFile.parentFile, name)

                    if (!isInsideProject(oldFile) || !isInsideProject(renamedFile)) {
                        errorMessage = "Invalid file location."
                        return
                    }

                    if (renamedFile.exists()) {
                        errorMessage = "A file or folder with this name already exists."
                        return
                    }

                    if (!oldFile.renameTo(renamedFile)) {
                        errorMessage = "Could not rename this item."
                        return
                    }
                }

                "delete" -> {
                    val file = targetFile ?: return

                    if (
                        !isInsideProject(file) ||
                        file.canonicalFile == projectRoot.canonicalFile
                    ) {
                        errorMessage = "This item cannot be deleted."
                        return
                    }

                    if (!file.deleteRecursively()) {
                        errorMessage = "Could not delete this item."
                        return
                    }
                }
            }

            errorMessage = null
            refreshKey++
            dialogMode = null
            targetFile = null
        } catch (exception: Exception) {
            errorMessage = exception.message ?: "Operation failed."
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
            verticalAlignment = Alignment.CenterVertically
        ) {
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

            Column(Modifier.weight(1f)) {
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

            IconButton(onClick = { refreshKey++ }) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = ExplorerBlue
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = ExplorerCard
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = currentDirectory.relativeTo(projectRoot).path
                    .ifEmpty { "Project Root" },
                color = ExplorerMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(14.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { showCreateFile() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.NoteAdd, contentDescription = null)
                Spacer(Modifier.width(5.dp))
                Text("New File")
            }

            OutlinedButton(
                onClick = { showCreateFolder() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                Spacer(Modifier.width(5.dp))
                Text("New Folder")
            }
        }

        Spacer(Modifier.height(12.dp))

        errorMessage?.let {
            Text(
                it,
                color = ExplorerRed,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(8.dp))
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
                    color = ExplorerMuted
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
                        onOpen = {
                            if (file.isDirectory) {
                                currentDirectory = file
                            } else {
                                onFileSelected(file)
                            }
                        },
                        onRename = { showRename(file) },
                        onDelete = { showDelete(file) }
                    )
                }
            }
        }
    }

    if (dialogMode != null) {
        val isDelete = dialogMode == "delete"

        val title = when (dialogMode) {
            "create_file" -> "Create New File"
            "create_folder" -> "Create New Folder"
            "rename" -> "Rename"
            "delete" -> "Delete Item?"
            else -> "NovaRun"
        }

        AlertDialog(
            onDismissRequest = { dialogMode = null },
            containerColor = ExplorerCard,
            title = {
                Text(title, color = Color.White)
            },
            text = {
                if (isDelete) {
                    Text(
                        "Delete ${targetFile?.name}? This action cannot be undone.",
                        color = ExplorerMuted
                    )
                } else {
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ExplorerBlue,
                            unfocusedBorderColor = ExplorerMuted,
                            cursorColor = ExplorerBlue
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { performAction() }) {
                    Text(
                        if (isDelete) "Delete" else "Save",
                        color = if (isDelete) ExplorerRed else ExplorerBlue
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { dialogMode = null }) {
                    Text("Cancel", color = ExplorerMuted)
                }
            }
        )
    }
}

@Composable
private fun FileExplorerItem(
    file: File,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = ExplorerCard
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpen() }
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (file.isDirectory) {
                        Icons.Default.Folder
                    } else {
                        Icons.Default.InsertDriveFile
                    },
                    contentDescription = null,
                    tint = if (file.isDirectory) ExplorerBlue else ExplorerMuted,
                    modifier = Modifier.size(25.dp)
                )

                Spacer(Modifier.width(12.dp))

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

            IconButton(onClick = onRename) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Rename",
                    tint = ExplorerMuted
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = ExplorerRed
                )
            }
        }
    }
}
