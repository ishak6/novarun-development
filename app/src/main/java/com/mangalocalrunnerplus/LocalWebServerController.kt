
package com.mangalocalrunnerplus

import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LocalServerStatus {
    STOPPED,
    STARTING,
    RUNNING,
    ERROR
}

data class LocalServerState(
    val status: LocalServerStatus = LocalServerStatus.STOPPED,
    val projectName: String? = null,
    val url: String? = null,
    val message: String = "Server is stopped.",
    val port: Int? = null
)

class LocalWebServerController {

    private val _state = MutableStateFlow(
        LocalServerState()
    )

    val state: StateFlow<LocalServerState> =
        _state.asStateFlow()

    private var server: LocalWebServer? = null

    private var activeProject: File? = null

    @Synchronized
    fun start(
        projectDirectory: File,
        port: Int = 8080
    ): Boolean {

        if (port !in 1024..65535) {

            _state.value = LocalServerState(
                status = LocalServerStatus.ERROR,
                message = "Invalid port number."
            )

            return false
        }

        val root = try {
            projectDirectory.canonicalFile
        } catch (exception: Exception) {

            _state.value = LocalServerState(
                status = LocalServerStatus.ERROR,
                message = "Unable to access project directory."
            )

            return false
        }

        if (
            !root.exists() ||
            !root.isDirectory ||
            !root.canRead()
        ) {

            _state.value = LocalServerState(
                status = LocalServerStatus.ERROR,
                message = "Project directory is unavailable."
            )

            return false
        }

        // Reuse an already running server for the same project.
        if (
            server?.isRunning == true &&
            activeProject == root &&
            _state.value.port == port
        ) {
            return true
        }

        // Stop the previous server before starting another.
        stopInternal()

        _state.value = LocalServerState(
            status = LocalServerStatus.STARTING,
            projectName = root.name,
            message = "Starting local server...",
            port = port
        )

        val newServer = LocalWebServer(
            projectDirectory = root,
            port = port
        )

        val started = newServer.start()

        if (!started) {

            _state.value = LocalServerState(
                status = LocalServerStatus.ERROR,
                projectName = root.name,
                message = "Could not start server. Port may be in use.",
                port = port
            )

            return false
        }

        server = newServer
        activeProject = root

        _state.value = LocalServerState(
            status = LocalServerStatus.RUNNING,
            projectName = root.name,
            url = "http://127.0.0.1:$port/",
            message = "Local server is running.",
            port = port
        )

        return true
    }

    @Synchronized
    fun stop() {
        stopInternal()
    }

    private fun stopInternal() {

        runCatching {
            server?.stop()
        }

        server = null
        activeProject = null

        _state.value = LocalServerState(
            status = LocalServerStatus.STOPPED,
            message = "Server is stopped."
        )
    }

    fun getCurrentState(): LocalServerState {
        return _state.value
    }
}
