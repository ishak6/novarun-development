
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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

                val requestedDuration = intent.getLongExtra(
                    EXTRA_DURATION,
                    0L
                )

                val durationMillis = requestedDuration.coerceAtLeast(0L)

                val port = intent.getIntExtra(
                    EXTRA_PORT,
                    DEFAULT_PORT
                )

                if (projectPath.isNullOrBlank()) {
                    stopSelf()
                    return START_NOT_STICKY
                }

                showForegroundNotification(
                    "Starting NovaRun server..."
                )

                startServer(
                    projectPath = projectPath,
                    durationMillis = durationMillis,
                    port = port
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
        port: Int
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
                showForegroundNotification(
                    "Project directory is unavailable"
                )
                stopSelf()
                return@launch
            }

            if (port !in 1024..65535) {
                showForegroundNotification(
                    "Invalid server port"
                )
                stopSelf()
                return@launch
            }

            val newServer = LocalWebServer(
                projectDirectory = project,
                port = port
            )

            val started = withContext(Dispatchers.IO) {
                newServer.start()
            }

            if (!started) {
                showForegroundNotification(
                    "Could not start local server"
                )
                stopSelf()
                return@launch
            }

            server = newServer

            showForegroundNotification(
                if (durationMillis == 0L) {
                    "Server running - Unlimited"
                } else {
                    "Server running on port $port"
                }
            )

            // Zero means Unlimited.
            // Any positive value automatically stops the server.
            if (durationMillis > 0L) {
                delay(durationMillis)

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

    private fun stopServer() {

        startJob?.cancel()
        startJob = null

        server?.stop()
        server = null

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

            val manager = getSystemService(
                NotificationManager::class.java
            )

            manager.createNotificationChannel(channel)
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

        serviceScope.cancel()

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {

        private const val CHANNEL_ID = "novarun_local_server"
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

        private const val DEFAULT_PORT = 8080

        fun start(
            context: Context,
            projectDirectory: File,
            durationMillis: Long = 0L,
            port: Int = DEFAULT_PORT
        ) {

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
