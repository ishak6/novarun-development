package com.mangalocalrunnerplus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Process
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Properties
import java.util.concurrent.atomic.AtomicBoolean

internal class NodeNativeBridge {
    companion object {
        init {
            System.loadLibrary("node")
            System.loadLibrary("novarun_node_bridge")
        }
    }
    external fun startNodeWithArguments(
        arguments: Array<String>, workingDirectory: String, modulesPath: String,
        tempDirectory: String, logPath: String, port: String
    ): Int
}

internal data class NodeLaunchPlan(
    val root: File, val entry: File, val port: Int, val warnings: List<String>
) {
    companion object {
        const val BUNDLED_NODE_VERSION = "24.22.1"
        private val entryNames = listOf("server.js", "index.js", "app.js", "src/server.js", "src/index.js", "backend/server.js")
        private val ignoredDirectories = setOf(".git", ".gradle", ".idea", "node_modules", "vendor", ".next", ".nuxt", ".cache")

        fun create(project: File, port: Int): Result<NodeLaunchPlan> = runCatching {
            require(port in 1024..65535) { "Port must be between 1024 and 65535." }
            val selectedRoot = project.canonicalFile
            require(selectedRoot.isDirectory && selectedRoot.canRead()) { "Project folder is missing or unreadable." }

            // ZIP archives often wrap the real app in one or more folders.
            // Select the nearest safe directory containing both package.json and a server entry.
            val candidateDirectories = sequenceOf(selectedRoot) + selectedRoot.walkTopDown()
                .onEnter { directory ->
                    directory == selectedRoot || directory.name.lowercase() !in ignoredDirectories
                }
                .maxDepth(8)
                .filter { it.isDirectory && it.canonicalFile.path.startsWith(selectedRoot.path + File.separator) }
                .sortedBy { it.relativeTo(selectedRoot).invariantSeparatorsPath.count { ch -> ch == '/' } }
            val projectRoot = candidateDirectories.firstOrNull { directory ->
                File(directory, "package.json").isFile && findEntry(directory) != null
            } ?: selectedRoot

            val root = projectRoot.canonicalFile
            val entry = findEntry(root)
                ?: error("No Node server entry found (server.js, index.js, app.js, src/server.js or backend/server.js).")
            val packageFile = File(root, "package.json")
            val report = if (packageFile.isFile) {
                NodeProjectInspector.inspect(packageFile, root.walkTopDown().maxDepth(2).filter { it.isFile }.map { it.name }.toSet())
            } else null
            val deps = (report?.dependencyCount ?: 0) + (report?.devDependencyCount ?: 0)
            require(deps == 0 || File(root, "node_modules").isDirectory) {
                "package.json declares $deps dependencies but node_modules is missing. npm install is not bundled in this build."
            }

            val engineSource = if (packageFile.isFile) runCatching { packageFile.readText().take(256 * 1024) }.getOrDefault("") else ""
            val enginesBlock = Regex(""" "engines"\s*:\s*\{([^}]*)\} """.trim())
                .find(engineSource)?.groupValues?.get(1).orEmpty()
            val engine = Regex(""" "node"\s*:\s*"([^"]+)" """.trim())
                .find(enginesBlock)?.groupValues?.get(1)
            val minimum = engine?.let { requirement ->
                Regex("(?:>=|>)\\s*(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?").find(requirement)
            }
            if (minimum != null) {
                val required = listOf(1, 2, 3).map { minimum.groupValues[it].toIntOrNull() ?: 0 }
                val bundled = BUNDLED_NODE_VERSION.split('.').map { it.toIntOrNull() ?: 0 }
                val incompatible = (0..2).firstNotNullOfOrNull { index ->
                    when {
                        bundled[index] < required[index] -> true
                        bundled[index] > required[index] -> false
                        else -> null
                    }
                } ?: false
                require(!incompatible) {
                    "This project requires Node.js ${engine}; NovaRun currently bundles Node.js ${BUNDLED_NODE_VERSION}. This backend cannot start until a compatible Node.js runtime is integrated."
                }
            }

            val warnings = buildList {
                if (report?.hasNativeAddonIndicators == true) add("Native addons need Android ABI-compatible prebuilt modules.")
                if (report?.hasLifecycleScripts == true) add("Package lifecycle scripts are not run automatically.")
                add("Node.js Mobile does not support child_process or cluster; scripts requiring spawned processes will not work.")
                if (File(root, "public/index.html").isFile) add("Your server.js must serve the public/ directory to expose public/index.html.")
                if (root != selectedRoot) add("Detected nested Node project root: ${root.relativeTo(selectedRoot).invariantSeparatorsPath}")
            }
            NodeLaunchPlan(root, entry.canonicalFile, port, warnings)
        }

        private fun findEntry(root: File): File? {
            val canonicalRoot = root.canonicalFile
            return entryNames.asSequence().map { File(canonicalRoot, it) }.firstOrNull { candidate ->
                runCatching {
                    val canonical = candidate.canonicalFile
                    canonical.isFile && canonical.canRead() &&
                        canonical.path.startsWith(canonicalRoot.path + File.separator)
                }.getOrDefault(false)
            }
        }
    }
}

internal object NodeLogLimits {
    const val MAX_BYTES = 2 * 1024 * 1024
    const val TRUNCATION_MARKER = "[NovaRun] log output truncated after 2 MiB."
}

internal data class NodeState(
    val status: String = "STOPPED", val message: String = "Node.js backend stopped",
    val url: String? = null, val logPath: String? = null
)

internal object NodeStateStore {
    private fun file(context: Context) = File(context.filesDir, "node-backend-state.properties")
    fun read(context: Context): NodeState = runCatching {
        val p = Properties(); file(context).inputStream().use(p::load)
        NodeState(p.getProperty("status", "STOPPED"), p.getProperty("message", "Stopped"),
            p.getProperty("url")?.takeIf(String::isNotBlank), p.getProperty("logPath")?.takeIf(String::isNotBlank))
    }.getOrDefault(NodeState())
    fun write(context: Context, state: NodeState) {
        runCatching {
            val target = file(context); val temp = File(target.parentFile, target.name + ".tmp")
            val p = Properties().apply {
                setProperty("status", state.status); setProperty("message", state.message)
                setProperty("url", state.url.orEmpty()); setProperty("logPath", state.logPath.orEmpty())
            }
            temp.outputStream().use { p.store(it, "NovaRun Node state") }
            if (!temp.renameTo(target)) { target.outputStream().use { p.store(it, "NovaRun Node state") }; temp.delete() }
        }
    }
}

class NodeProjectService : Service() {
    private val started = AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "NovaRun Node.js backend", NotificationManager.IMPORTANCE_LOW)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                NodeStateStore.write(this, NodeState(message = "Stopped by user"))
                stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
                // This service is isolated in :nodejs; only that runtime process is terminated.
                Process.killProcess(Process.myPid())
            }
            ACTION_START -> startProject(intent)
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startProject(intent: Intent) {
        if (!started.compareAndSet(false, true)) return
        foreground("Preparing embedded Node.js…")
        val project = intent.getStringExtra(EXTRA_PROJECT)?.let(::File)
        val port = intent.getIntExtra(EXTRA_PORT, 3000)
        val plan = project?.let { NodeLaunchPlan.create(it, port) }
        if (plan == null || plan.isFailure) {
            NodeStateStore.write(this, NodeState("ERROR", plan?.exceptionOrNull()?.message ?: "No project selected."))
            started.set(false)
            stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return
        }
        val p = plan.getOrThrow()
        val log = File(filesDir, "node-backend-${p.root.name.hashCode()}-$port.log")
        log.writeText("NovaRun embedded Node.js 24.22.1\nProject: ${p.root.name}\nEntry: ${p.entry.relativeTo(p.root).path}\n")
        // Write all service-owned diagnostics before the native reader snapshots
        // the initial file length and starts enforcing the combined 2 MiB cap.
        if (p.warnings.isNotEmpty()) {
            log.appendText("\nCompatibility notes:\n- " + p.warnings.joinToString("\n- ") + "\n")
        }
        val url = "http://127.0.0.1:$port/"
        NodeStateStore.write(this, NodeState("STARTING", "Starting ${p.entry.name}…", url, log.absolutePath))
        Thread({
            try {
                val code = NodeNativeBridge().startNodeWithArguments(
                    arrayOf("node", p.entry.absolutePath), p.root.absolutePath,
                    File(p.root, "node_modules").absolutePath, cacheDir.absolutePath, log.absolutePath, p.port.toString()
                )
                NodeStateStore.write(this, NodeState(if (code == 0) "STOPPED" else "ERROR", "Node.js exited with code $code", url, log.absolutePath))
            } catch (e: Throwable) {
                NodeStateStore.write(this, NodeState("ERROR", "Node runtime error: ${e.message ?: e.javaClass.simpleName}", url, log.absolutePath))
            } finally {
                started.set(false)
                stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
            }
        }, "NovaRun-Node-Engine").start()

        Thread({
            repeat(120) {
                if (NodeStateStore.read(this).status in setOf("ERROR", "STOPPED")) return@Thread
                val listening = runCatching {
                    Socket().use { it.connect(InetSocketAddress("127.0.0.1", port), 250) }
                    true
                }.getOrDefault(false)
                if (listening) {
                    NodeStateStore.write(this, NodeState("RUNNING", "Node.js server is accepting local connections.", url, log.absolutePath))
                    foreground("Node.js server running on port $port")
                    return@Thread
                }
                Thread.sleep(250)
            }
            if (NodeStateStore.read(this).status == "STARTING") {
                NodeStateStore.write(this, NodeState("ERROR", "No server accepted connections on port $port within 30 seconds. Check the log.", url, log.absolutePath))
                Process.killProcess(Process.myPid())
            }
        }, "NovaRun-Node-Health").start()
    }

    private fun foreground(message: String) {
        val stop = PendingIntent.getService(this, 8102, Intent(this, NodeProjectService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("NovaRun Node.js backend").setContentText(message).setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Stop", stop).build()).build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?) = null

    companion object {
        private const val CHANNEL = "novarun_node_backend"
        private const val NOTIFICATION_ID = 8102
        private const val ACTION_START = "com.mangalocalrunnerplus.NODE_START"
        private const val ACTION_STOP = "com.mangalocalrunnerplus.NODE_STOP"
        private const val EXTRA_PROJECT = "node_project_root"
        private const val EXTRA_PORT = "node_server_port"
        fun start(context: Context, project: File, port: Int) {
            NodeStateStore.write(context, NodeState("STARTING", "Preparing Node.js runtime…"))
            val i = Intent(context, NodeProjectService::class.java).setAction(ACTION_START)
                .putExtra(EXTRA_PROJECT, project.canonicalPath).putExtra(EXTRA_PORT, port)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i) else context.startService(i)
        }
        fun stop(context: Context) {
            context.startService(Intent(context, NodeProjectService::class.java).setAction(ACTION_STOP))
        }
    }
}

@Composable
fun NodeBackendRunnerCard(project: File?, onOpenPreview: (String) -> Unit = {}) {
    val context = LocalContext.current
    var portText by remember { mutableStateOf("3000") }
    var state by remember { mutableStateOf(NodeStateStore.read(context)) }
    var logText by remember { mutableStateOf("") }
    LaunchedEffect(project) {
        while (true) {
            state = NodeStateStore.read(context)
            logText = state.logPath?.let { path -> runCatching { File(path).takeIf(File::isFile)?.readText()?.takeLast(10000).orEmpty() }.getOrDefault("") } ?: ""
            delay(1000)
        }
    }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NovaRunPalette.Surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, NovaRunPalette.Border)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("NODE.JS FULL-STACK RUNNER", color = NovaRunPalette.Cyan, fontSize = 12.sp)
            Text(state.status, color = when (state.status) {
                "RUNNING" -> NovaRunPalette.Lime
                "ERROR" -> NovaRunPalette.Error
                "STARTING" -> NovaRunPalette.Warning
                else -> NovaRunPalette.TextPrimary
            })
            Text(state.message, color = NovaRunPalette.TextSecondary, fontSize = 12.sp)
            OutlinedTextField(value = portText, onValueChange = { if (it.length <= 5 && it.all(Char::isDigit)) portText = it },
                label = { Text("Node server port") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("Uses Node.js 24.22.1 full runtime with built-in SQLite. Both supplied store ZIPs have no npm dependencies. child_process/cluster are unsupported.", color = NovaRunPalette.TextSecondary, fontSize = 11.sp)
            Text("First admin setup token appears in SERVER LOG on first launch. Keep it private.", color = NovaRunPalette.Warning, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = project != null && state.status !in setOf("STARTING", "RUNNING"),
                    onClick = {
                        val port = portText.toIntOrNull()
                        when {
                            project == null -> android.widget.Toast.makeText(context, "Select a project first", android.widget.Toast.LENGTH_SHORT).show()
                            port == null || port !in 1024..65535 -> android.widget.Toast.makeText(context, "Port must be 1024–65535", android.widget.Toast.LENGTH_SHORT).show()
                            else -> NodeProjectService.start(context, project, port)
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = NovaRunPalette.Cyan)) { Text("Start Node") }
                OutlinedButton(enabled = state.status in setOf("STARTING", "RUNNING", "ERROR"), onClick = { NodeProjectService.stop(context) }) { Text("Stop Node") }
            }
            state.url?.let { Text("Local URL: $it", color = NovaRunPalette.Lime, fontSize = 12.sp) }
            Button(
                enabled = state.status == "RUNNING" && state.url != null,
                onClick = { state.url?.let(onOpenPreview) },
                colors = ButtonDefaults.buttonColors(containerColor = NovaRunPalette.Lime)
            ) { Text("Open Website Preview") }
            if (logText.isNotBlank()) {
                Text("SERVER LOG", color = NovaRunPalette.TextPrimary, fontSize = 11.sp)
                Text(logText, color = NovaRunPalette.TextSecondary, fontSize = 10.sp, maxLines = 40)
            }
        }
    }
}
