package com.mangalocalrunnerplus

import android.database.sqlite.SQLiteDatabase
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private val ToolBackground = Color(0xFF090B10)
private val ToolCard = Color(0xFF171D28)
private val ToolBlue = Color(0xFF0A84FF)
private val ToolMuted = Color(0xFF9AA6B5)

@Composable
fun DeveloperToolsScreen() {
    var selectedTool by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ToolBackground)
            .padding(16.dp)
    ) {
        Text(
            "Developer Tools",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(14.dp))

        ScrollableTabRow(
            selectedTabIndex = selectedTool,
            containerColor = ToolBackground,
            contentColor = ToolBlue
        ) {
            listOf("JavaScript", "SQLite", "API Tester").forEachIndexed { index, title ->
                Tab(
                    selected = selectedTool == index,
                    onClick = { selectedTool = index },
                    text = { Text(title) }
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        when (selectedTool) {
            0 -> JavaScriptTool()
            1 -> SQLiteTool()
            else -> ApiTesterTool()
        }
    }
}

@Composable
private fun JavaScriptTool() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var script by remember {
        mutableStateOf(
            "var total = 10 + 20;\ntotal;"
        )
    }

    var output by remember {
        mutableStateOf("JavaScript output will appear here.")
    }

    var running by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Embedded JavaScript Engine",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            "Pure JavaScript execution. Node.js and browser APIs are not included.",
            color = ToolMuted
        )

        OutlinedTextField(
            value = script,
            onValueChange = { script = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            label = { Text("JavaScript code") },
            minLines = 6,
            colors = toolFieldColors()
        )

        Button(
            onClick = {
                scope.launch {
                    running = true
                    output = "Running..."

                    try {
                        val result = withContext(Dispatchers.IO) {
                            val project = File(context.filesDir, "js-playground")
                            if (!project.exists()) project.mkdirs()

                            val scriptFile = File(project, "main.js")
                            scriptFile.writeText(script)

                            RuntimeExecutionManager.execute(
                                context = context,
                                language = "JavaScript",
                                request = RuntimeExecutionRequest(
                                    projectDirectory = project,
                                    entryFile = scriptFile.absolutePath
                                )
                            )
                        }

                        output = if (result.success) {
                            result.output
                        } else {
                            result.error
                        }
                    } catch (error: Exception) {
                        output = error.message ?: "Execution failed."
                    }

                    running = false
                }
            },
            enabled = !running,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (running) "Running..." else "Run JavaScript")
        }

        ToolOutputCard(output)
    }
}

@Composable
private fun SQLiteTool() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var projectName by remember { mutableStateOf("js-playground") }
    var query by remember {
        mutableStateOf(
            "CREATE TABLE IF NOT EXISTS notes (id INTEGER PRIMARY KEY, title TEXT);\n" +
            "INSERT INTO notes (title) VALUES ('Hello NovaRun');\n" +
            "SELECT * FROM notes;"
        )
    }

    var output by remember {
        mutableStateOf("SQLite query result will appear here.")
    }

    var running by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Project SQLite Database",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = projectName,
            onValueChange = { projectName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Project folder name") },
            singleLine = true,
            colors = toolFieldColors()
        )

        Text(
            "Database file: project/.novarun/database.db",
            color = ToolMuted
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            label = { Text("SQL query") },
            minLines = 6,
            colors = toolFieldColors()
        )

        Button(
            onClick = {
                scope.launch {
                    running = true
                    output = "Executing SQL..."

                    output = withContext(Dispatchers.IO) {
                        executeProjectSql(
                            context = context,
                            projectName = projectName,
                            sql = query
                        )
                    }

                    running = false
                }
            },
            enabled = !running,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (running) "Executing..." else "Run SQL")
        }

        ToolOutputCard(output)
    }
}

private fun executeProjectSql(
    context: android.content.Context,
    projectName: String,
    sql: String
): String {
    return try {
        if (!projectName.matches(Regex("[A-Za-z0-9._-]{1,80}"))) {
            return "Invalid project folder name."
        }

        val projectsRoot = File(context.filesDir, "projects")
        val project = File(projectsRoot, projectName)

        if (!project.exists() || !project.isDirectory) {
            return "Project not found in the imported projects folder."
        }

        val projectCanonical = project.canonicalFile
        if (projectCanonical.parentFile != projectsRoot.canonicalFile) {
            return "Invalid project path."
        }

        val dataDirectory = File(project, ".novarun")
        if (!dataDirectory.exists()) dataDirectory.mkdirs()

        val databaseFile = File(dataDirectory, "database.db")
        val database = SQLiteDatabase.openOrCreateDatabase(
            databaseFile,
            null
        )

        try {
            val statements = sql
                .split(";")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            if (statements.isEmpty()) {
                return "No SQL statement provided."
            }

            val result = StringBuilder()

            statements.forEach { statement ->
                val normalized = statement
                    .trimStart()
                    .uppercase()

                if (
                    normalized.startsWith("SELECT") ||
                    normalized.startsWith("PRAGMA") ||
                    normalized.startsWith("WITH")
                ) {
                    database.rawQuery(statement, null).use { cursor ->
                        val columns = cursor.columnNames

                        result.append(columns.joinToString(" | "))
                        result.append("\n")

                        var rowCount = 0

                        while (cursor.moveToNext() && rowCount < 200) {
                            val row = columns.indices.map { index ->
                                if (cursor.isNull(index)) {
                                    "NULL"
                                } else {
                                    cursor.getString(index) ?: ""
                                }
                            }

                            result.append(row.joinToString(" | "))
                            result.append("\n")
                            rowCount++
                        }

                        result.append("Rows shown: $rowCount\n\n")
                    }
                } else {
                    database.execSQL(statement)
                    result.append("Executed successfully.\n")
                    result.append(statement.take(160))
                    result.append("\n\n")
                }
            }

            result.toString().ifBlank {
                "SQL executed successfully."
            }
        } finally {
            database.close()
        }
    } catch (error: Exception) {
        "SQLite error: ${error.message ?: "Unknown database error"}"
    }
}

@Composable
private fun ApiTesterTool() {
    val scope = rememberCoroutineScope()

    var method by remember { mutableStateOf("GET") }
    var url by remember { mutableStateOf("https://jsonplaceholder.typicode.com/todos/1") }
    var headers by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    var response by remember {
        mutableStateOf("API response will appear here.")
    }

    var running by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "HTTP API Tester",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("GET", "POST", "PUT", "PATCH", "DELETE").forEach { item ->
                FilterChip(
                    selected = method == item,
                    onClick = { method = item },
                    label = { Text(item) }
                )
            }
        }

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Request URL") },
            singleLine = true,
            colors = toolFieldColors()
        )

        OutlinedTextField(
            value = headers,
            onValueChange = { headers = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            label = { Text("Headers: one Name: Value per line") },
            colors = toolFieldColors()
        )

        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            label = { Text("Request body") },
            colors = toolFieldColors()
        )

        Button(
            onClick = {
                scope.launch {
                    running = true
                    response = "Sending request..."

                    response = withContext(Dispatchers.IO) {
                        sendApiRequest(
                            method = method,
                            address = url,
                            headerText = headers,
                            requestBody = body
                        )
                    }

                    running = false
                }
            },
            enabled = !running,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (running) "Sending..." else "Send Request")
        }

        ToolOutputCard(response)
    }
}

private fun sendApiRequest(
    method: String,
    address: String,
    headerText: String,
    requestBody: String
): String {
    var connection: HttpURLConnection? = null

    return try {
        val parsedUrl = URL(address)

        if (parsedUrl.protocol != "https" && parsedUrl.protocol != "http") {
            return "Only HTTP and HTTPS URLs are allowed."
        }

        connection = (parsedUrl.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15000
            readTimeout = 20000
            instanceFollowRedirects = false
            useCaches = false
        }

        headerText.lines()
            .filter { it.contains(":") }
            .forEach { line ->
                val separator = line.indexOf(":")
                val name = line.substring(0, separator).trim()
                val value = line.substring(separator + 1).trim()

                if (
                    name.isNotBlank() &&
                    !name.contains("\r") &&
                    !name.contains("\n") &&
                    !value.contains("\r") &&
                    !value.contains("\n")
                ) {
                    connection.setRequestProperty(name, value)
                }
            }

        if (method in listOf("POST", "PUT", "PATCH")) {
            connection.doOutput = true

            if (connection.getRequestProperty("Content-Type") == null) {
                connection.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=utf-8"
                )
            }

            connection.outputStream.use { stream ->
                stream.write(requestBody.toByteArray(Charsets.UTF_8))
            }
        }

        val status = connection.responseCode
        val stream = if (status >= 400) {
            connection.errorStream
        } else {
            connection.inputStream
        }

        val responseText = stream?.bufferedReader()?.use {
            it.readText().take(100000)
        } ?: "(Empty response)"

        "HTTP Status: $status\n" +
            "Message: ${connection.responseMessage}\n\n" +
            responseText

    } catch (error: Exception) {
        "API request failed: ${error.message ?: "Unknown network error"}"
    } finally {
        connection?.disconnect()
    }
}

@Composable
private fun ToolOutputCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ToolCard
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "OUTPUT",
                color = ToolBlue,
                style = MaterialTheme.typography.labelMedium
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text,
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun toolFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = ToolBlue,
    unfocusedBorderColor = ToolMuted,
    focusedLabelColor = ToolBlue,
    unfocusedLabelColor = ToolMuted,
    cursorColor = ToolBlue
)
