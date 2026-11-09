
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
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class LocalServerServiceStatus {
    STOPPED,
    STARTING,
    RUNNING,
    ERROR
}

data class LocalServerServiceState(
    val status: LocalServerServiceStatus =
        LocalServerServiceStatus.STOPPED,
    val message: String = "Server is stopped",
    val projectName: String? = null,
    val url: String? = null,
    val lanUrl: String? = null,
    val localUrl: String? = null,
    val durationMillis: Long = 0L,
    val remainingMillis: Long = 0L,
    val port: Int = 8080,
    val accessPassword: String? = null
)

object LocalServerServiceStateStore {

    private val _state =
        MutableStateFlow(LocalServerServiceState())

    val state: StateFlow<LocalServerServiceState> =
        _state.asStateFlow()

    internal fun update(
        newState: LocalServerServiceState
    ) {
        _state.value = newState
    }
}

class LocalWebServerService : Service() {

    private val serviceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    private var server: LocalWebServer? = null
    private var startJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {

            ACTION_STOP -> {
                stopServer()
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_START -> {

                val projectPath = intent.getStringExtra(
                    EXTRA_PROJECT_PATH
                )

                val durationMillis = intent.getLongExtra(
                    EXTRA_DURATION,
                    0L
                ).coerceAtLeast(0L)

                val port = intent.getIntExtra(
                    EXTRA_PORT,
                    DEFAULT_PORT
                )
                val accessToken = intent.getStringExtra(EXTRA_ACCESS_TOKEN)

                if (projectPath.isNullOrBlank()) {
                    setError("No project selected")
                    stopSelf()
                    return START_NOT_STICKY
                }

                if (accessToken.isNullOrBlank()) {
                    setError("LAN access token is missing")
                    stopSelf()
                    return START_NOT_STICKY
                }

                showForegroundNotification(
                    "Starting NovaRun LAN server..."
                )

                LocalServerServiceStateStore.update(
                    LocalServerServiceState(
                        status = LocalServerServiceStatus.STARTING,
                        message = "Starting LAN server...",
                        port = port,
                        accessPassword = accessToken
                    )
                )

                startServer(
                    projectPath = projectPath,
                    durationMillis = durationMillis,
                    port = port,
                    accessToken = accessToken
                )
            }

            else -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        return START_NOT_STICKY
    }

    private fun startServer(
        projectPath: String,
        durationMillis: Long,
        port: Int,
        accessToken: String
    ) {

        startJob?.cancel()

        startJob = serviceScope.launch {

            stopExistingServer()

            val project = try {
                File(projectPath).canonicalFile
            } catch (exception: Exception) {
                null
            }

            if (
                project == null ||
                !project.exists() ||
                !project.isDirectory ||
                !project.canRead()
            ) {
                setError("Project directory is unavailable")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return@launch
            }

            if (port !in 1024..65535) {
                setError("Invalid server port")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return@launch
            }

            // Serve compiled frontend output when present (dist/build/out),
            // otherwise serve the selected project root as a static directory.
            val webRoot = ProjectPreviewValidator.validate(project).projectRoot
                ?: project

            val newServer = LocalWebServer(
                projectDirectory = webRoot,
                port = port,
                bindToLan = true,
                accessToken = accessToken
            )

            val started = withContext(Dispatchers.IO) {
                newServer.start()
            }

            if (!started) {
                setError(
                    "Could not start LAN server. Port may already be in use."
                )
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return@launch
            }

            server = newServer

            val localAddress =
                "http://127.0.0.1:$port"

            val lanAddress =
                LocalWebServer.getLocalNetworkAddress()

            val lanAddressUrl = lanAddress?.let {
                "http://$it:$port"
            }

            val primaryUrl =
                lanAddressUrl ?: localAddress

            val startTime = SystemClock.elapsedRealtime()

            val endTime = if (durationMillis > 0L) {
                startTime + durationMillis
            } else {
                0L
            }

            LocalServerServiceStateStore.update(
                LocalServerServiceState(
                    status = LocalServerServiceStatus.RUNNING,
                    message = if (lanAddressUrl != null) {
                        if (durationMillis == 0L) {
                            "LAN server running - Unlimited"
                        } else {
                            "LAN server running"
                        }
                    } else {
                        "Server running. LAN IP unavailable."
                    },
                    projectName = project.name,
                    url = primaryUrl,
                    lanUrl = lanAddressUrl,
                    localUrl = localAddress,
                    durationMillis = durationMillis,
                    remainingMillis = durationMillis,
                    port = port,
                    accessPassword = accessToken
                )
            )

            showForegroundNotification(
                if (lanAddressUrl != null) {
                    "LAN server running on port $port"
                } else {
                    "Server running. LAN IP unavailable."
                }
            )

            // Unlimited mode: no automatic stop.
            if (durationMillis > 0L) {

                while (true) {

                    val remaining =
                        endTime - SystemClock.elapsedRealtime()

                    if (remaining <= 0L) {
                        break
                    }

                    LocalServerServiceStateStore.update(
                        LocalServerServiceState(
                            status = LocalServerServiceStatus.RUNNING,
                            message = if (lanAddressUrl != null) {
                                "LAN server running"
                            } else {
                                "Server running. LAN IP unavailable."
                            },
                            projectName = project.name,
                            url = primaryUrl,
                            lanUrl = lanAddressUrl,
                            localUrl = localAddress,
                            durationMillis = durationMillis,
                            remainingMillis = remaining,
                            port = port,
                            accessPassword = accessToken
                        )
                    )

                    delay(minOf(remaining, 1000L))
                }

                stopServer()
                stopSelf()
            }
        }
    }

    private suspend fun stopExistingServer() {

        withContext(Dispatchers.IO) {
            server?.stop()
            server = null
        }
    }

    private fun setError(message: String) {

        LocalServerServiceStateStore.update(
            LocalServerServiceState(
                status = LocalServerServiceStatus.ERROR,
                message = message
            )
        )
    }

    private fun stopServer() {

        startJob?.cancel()
        startJob = null

        server?.stop()
        server = null

        LocalServerServiceStateStore.update(
            LocalServerServiceState(
                status = LocalServerServiceStatus.STOPPED,
                message = "Server is stopped"
            )
        )

        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "NovaRun Local Server",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description =
                    "Shows when a NovaRun project server is running."
            }

            getSystemService(
                NotificationManager::class.java
            ).createNotificationChannel(channel)
        }
    }

    private fun showForegroundNotification(
        message: String
    ) {

        val stopIntent = Intent(
            this,
            LocalWebServerService::class.java
        ).apply {
            action = ACTION_STOP
        }

        val stopPendingIntent = PendingIntent.getService(
            this,
            1001,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val notification = if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        ) {

            Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle("NovaRun Local Server")
                .setContentText(message)
                .setOngoing(true)
                .addAction(
                    Notification.Action.Builder(
                        null,
                        "Stop",
                        stopPendingIntent
                    ).build()
                )
                .build()

        } else {

            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle("NovaRun Local Server")
                .setContentText(message)
                .setOngoing(true)
                .addAction(
                    android.R.drawable.ic_media_pause,
                    "Stop",
                    stopPendingIntent
                )
                .build()
        }

        if (Build.VERSION.SDK_INT >= 34) {

            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )

        } else {

            startForeground(
                NOTIFICATION_ID,
                notification
            )
        }
    }

    override fun onDestroy() {

        startJob?.cancel()

        server?.stop()
        server = null

        val currentState =
            LocalServerServiceStateStore.state.value

        if (
            currentState.status ==
                LocalServerServiceStatus.RUNNING ||
            currentState.status ==
                LocalServerServiceStatus.STARTING
        ) {
            LocalServerServiceStateStore.update(
                LocalServerServiceState(
                    status = LocalServerServiceStatus.STOPPED,
                    message = "Server stopped"
                )
            )
        }

        serviceScope.cancel()

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {

        private const val CHANNEL_ID =
            "novarun_local_server"

        private const val NOTIFICATION_ID = 4501

        private const val ACTION_START =
            "com.mangalocalrunnerplus.SERVER_START"

        private const val ACTION_STOP =
            "com.mangalocalrunnerplus.SERVER_STOP"

        private const val EXTRA_PROJECT_PATH =
            "project_path"

        private const val EXTRA_DURATION =
            "duration_millis"

        private const val EXTRA_PORT =
            "server_port"

        private const val EXTRA_ACCESS_TOKEN =
            "server_access_token"

        private const val DEFAULT_PORT = 8080

        private fun createAccessToken(): String {
            val bytes = ByteArray(24)
            java.security.SecureRandom().nextBytes(bytes)
            return android.util.Base64.encodeToString(
                bytes,
                android.util.Base64.URL_SAFE or
                    android.util.Base64.NO_PADDING or
                    android.util.Base64.NO_WRAP
            )
        }

        fun start(
            context: Context,
            projectDirectory: File,
            durationMillis: Long = 0L,
            port: Int = DEFAULT_PORT
        ) {

            val accessToken = createAccessToken()
            LocalServerServiceStateStore.update(
                LocalServerServiceState(
                    status = LocalServerServiceStatus.STARTING,
                    message = "Preparing authenticated LAN server...",
                    port = port,
                    accessPassword = accessToken
                )
            )

            val intent = Intent(
                context,
                LocalWebServerService::class.java
            ).apply {

                action = ACTION_START

                putExtra(
                    EXTRA_PROJECT_PATH,
                    projectDirectory.absolutePath
                )

                putExtra(
                    EXTRA_DURATION,
                    durationMillis.coerceAtLeast(0L)
                )

                putExtra(
                    EXTRA_PORT,
                    port
                )
                putExtra(
                    EXTRA_ACCESS_TOKEN,
                    accessToken
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {

            val intent = Intent(
                context,
                LocalWebServerService::class.java
            ).apply {
                action = ACTION_STOP
            }

            context.startService(intent)
        }
    }
}
