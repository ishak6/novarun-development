
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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
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
    val accessPassword: String? = null,
    val accessUsername: String? = null,
    val authenticationEnabled: Boolean = true,
    val tlsFingerprint: String? = null
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
    private var loopbackServer: LocalWebServer? = null
    private var startJob: Job? = null
    private val serverLock = Any()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        // Every startForegroundService() request must be promoted immediately,
        // even if a Stop intent races ahead of the queued Start intent.
        showForegroundNotification(
            if (intent?.action == ACTION_STOP) {
                "Stopping NovaRun local server..."
            } else {
                "Starting NovaRun local server..."
            }
        )

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
                val authenticationEnabled = intent.getBooleanExtra(EXTRA_AUTH_ENABLED, true)
                val credentials = if (authenticationEnabled) LanCredentialsStore.getOrNull(this) else null
                val accessToken = credentials?.password
                val accessUsername = credentials?.username

                if (projectPath.isNullOrBlank()) {
                    setError("No project selected")
                    stopSelf()
                    return START_NOT_STICKY
                }

                // Missing LAN credentials must not block this-device localhost hosting.
                // LAN hosting is simply skipped until credentials are available.

                LocalServerServiceStateStore.update(
                    LocalServerServiceState(
                        status = LocalServerServiceStatus.STARTING,
                        message = "Starting local preview server...",
                        port = port,
                        accessPassword = accessToken,
                        accessUsername = accessUsername,
                        authenticationEnabled = authenticationEnabled
                    )
                )

                startServer(
                    projectPath = projectPath,
                    durationMillis = durationMillis,
                    port = port,
                    accessToken = accessToken,
                    accessUsername = accessUsername,
                    authenticationEnabled = authenticationEnabled
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
        accessToken: String?,
        accessUsername: String?,
        authenticationEnabled: Boolean
    ) {

        startJob?.cancel()

        startJob = serviceScope.launch {

            val startCoroutineJob = currentCoroutineContext()[Job]
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

            // Resolve and validate the HTML entry point before opening a socket.
            val preview = ProjectPreviewValidator.validate(project)
            if (!preview.isValid || preview.projectRoot == null || preview.entryFile == null) {
                setError(preview.message)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return@launch
            }
            val webRoot = preview.projectRoot
            val defaultDocument = preview.entryFile.name

            // Start the loopback listener first. It is the required service for
            // this-device preview and has no dependency on LAN TLS or Wi-Fi.
            val loopback = withContext(Dispatchers.IO) {
                (18080..18100).firstNotNullOfOrNull { candidatePort ->
                    val candidate = LocalWebServer(
                        projectDirectory = webRoot,
                        port = candidatePort,
                        bindToLan = false,
                        defaultDocument = defaultDocument
                    )
                    if (candidate.start()) {
                        val adopted = synchronized(serverLock) {
                            if (startCoroutineJob?.isActive == true) {
                                loopbackServer = candidate
                                true
                            } else {
                                false
                            }
                        }
                        if (adopted) {
                            candidatePort to candidate
                        } else {
                            candidate.stop()
                            null
                        }
                    } else {
                        null
                    }
                }
            }

            if (!currentCoroutineContext().isActive) return@launch

            if (loopback == null) {
                setError("Could not start localhost server. Ports 18080–18100 are unavailable.")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return@launch
            }

            val localAddress = "http://127.0.0.1:${loopback.first}/"

            // LAN hosting is optional and independent. A missing password,
            // Keystore failure, unavailable Wi-Fi interface, or occupied LAN
            // port must not stop the localhost server.
            var tlsFingerprint: String? = null
            var lanAddressUrl: String? = null
            var lanFailure: String? = null

            if (port in 1024..65535 && (!authenticationEnabled || !accessToken.isNullOrBlank())) {
                try {
                    val tlsIdentity = withContext(Dispatchers.IO) {
                        LanTlsIdentity.create(this@LocalWebServerService)
                    }

                    val lanServer = LocalWebServer(
                        projectDirectory = webRoot,
                        port = port,
                        bindToLan = true,
                        accessToken = accessToken,
                        accessUsername = accessUsername ?: "novarun",
                        sslContext = tlsIdentity.sslContext,
                        defaultDocument = defaultDocument
                    )

                    // Finish the blocking bind attempt even if the parent job is
                    // cancelled, so we can deterministically close an unadopted socket.
                    val lanStarted = withContext(NonCancellable + Dispatchers.IO) {
                        lanServer.start()
                    }

                    if (lanStarted) {
                        val adopted = synchronized(serverLock) {
                            if (startCoroutineJob?.isActive == true) {
                                server = lanServer
                                true
                            } else {
                                false
                            }
                        }
                        if (adopted) {
                            tlsFingerprint = tlsIdentity.sha256Fingerprint
                            val lanAddress = LocalWebServer.getLocalNetworkAddress()
                            lanAddressUrl = lanAddress?.let { "https://$it:$port" }
                            if (lanAddressUrl == null) {
                                synchronized(serverLock) {
                                    if (server === lanServer) server = null
                                }
                                lanServer.stop()
                                lanFailure = "LAN IP unavailable"
                            }
                        } else {
                            lanServer.stop()
                            return@launch
                        }
                    } else {
                        lanFailure = "LAN port $port could not be opened"
                    }
                } catch (error: Exception) {
                    lanFailure = "LAN TLS unavailable: ${error.message ?: "Keystore error"}"
                }
            } else {
                lanFailure = if (port !in 1024..65535) {
                    "LAN port must be between 1024 and 65535"
                } else {
                    "LAN password is not configured"
                }
            }

            // Do not publish RUNNING if Stop/restart cancelled this startup while
            // the optional LAN listener was being prepared.
            if (!currentCoroutineContext().isActive) return@launch

            // Localhost is the primary URL on this device. LAN URL is separately
            // exposed only when its HTTPS listener is actually running.
            val primaryUrl = localAddress

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
                        if (durationMillis == 0L) "Localhost and HTTPS LAN server running - Unlimited"
                        else "Localhost and HTTPS LAN server running"
                    } else {
                        "Localhost running. ${lanFailure ?: "LAN hosting unavailable"}."
                    },
                    projectName = project.name,
                    url = primaryUrl,
                    lanUrl = lanAddressUrl,
                    localUrl = localAddress,
                    durationMillis = durationMillis,
                    remainingMillis = durationMillis,
                    port = port,
                    accessPassword = accessToken,
                    accessUsername = accessUsername,
                    authenticationEnabled = authenticationEnabled,
                    tlsFingerprint = tlsFingerprint
                )
            )

            showForegroundNotification(
                if (lanAddressUrl != null) {
                    "Localhost and HTTPS LAN server running"
                } else {
                    "Localhost server running; LAN unavailable"
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
                                "Localhost and HTTPS LAN server running"
                            } else {
                                "Localhost running. ${lanFailure ?: "LAN hosting unavailable"}."
                            },
                            projectName = project.name,
                            url = primaryUrl,
                            lanUrl = lanAddressUrl,
                            localUrl = localAddress,
                            durationMillis = durationMillis,
                            remainingMillis = remaining,
                            port = port,
                            accessPassword = accessToken,
                            accessUsername = accessUsername,
                            authenticationEnabled = authenticationEnabled,
                            tlsFingerprint = tlsFingerprint
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
            synchronized(serverLock) {
                server?.stop()
                server = null
                loopbackServer?.stop()
                loopbackServer = null
            }
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

        synchronized(serverLock) {
            server?.stop()
            server = null
            loopbackServer?.stop()
            loopbackServer = null
        }

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
        loopbackServer?.stop()
        loopbackServer = null

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

        private const val EXTRA_AUTH_ENABLED =
            "server_authentication_enabled"

        private const val DEFAULT_PORT = 8080

        fun start(
            context: Context,
            projectDirectory: File,
            durationMillis: Long = 0L,
            port: Int = DEFAULT_PORT,
            authenticationEnabled: Boolean = true
        ) {

            val credentials = if (authenticationEnabled) LanCredentialsStore.getOrNull(context) else null
            val accessToken = credentials?.password
            LocalServerServiceStateStore.update(
                LocalServerServiceState(
                    status = LocalServerServiceStatus.STARTING,
                    message = if (authenticationEnabled) "Preparing password-protected LAN server..." else "Preparing LAN server without password protection...",
                    port = port,
                    accessPassword = accessToken,
                    accessUsername = credentials?.username,
                    authenticationEnabled = authenticationEnabled
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
                putExtra(EXTRA_AUTH_ENABLED, authenticationEnabled)
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
