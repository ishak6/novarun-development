
package com.mangalocalrunnerplus

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class RuntimeLogLevel {
    INFO,
    OUTPUT,
    WARNING,
    ERROR
}

data class RuntimeLogEntry(
    val timestamp: Long,
    val level: RuntimeLogLevel,
    val message: String
)

object RuntimeConsoleLogger {

    private const val MAX_LOGS = 1000

    private val logs = mutableListOf<RuntimeLogEntry>()

    private val _logEntries =
        MutableStateFlow<List<RuntimeLogEntry>>(emptyList())

    /**
     * Observable log stream for Jetpack Compose.
     */
    val logEntries: StateFlow<List<RuntimeLogEntry>> =
        _logEntries.asStateFlow()

    @Synchronized
    fun log(
        level: RuntimeLogLevel,
        message: String
    ) {
        logs.add(
            RuntimeLogEntry(
                timestamp = System.currentTimeMillis(),
                level = level,
                message = message
            )
        )

        while (logs.size > MAX_LOGS) {
            logs.removeAt(0)
        }

        _logEntries.value = logs.toList()
    }

    @Synchronized
    fun info(message: String) {
        log(RuntimeLogLevel.INFO, message)
    }

    @Synchronized
    fun output(message: String) {
        log(RuntimeLogLevel.OUTPUT, message)
    }

    @Synchronized
    fun warning(message: String) {
        log(RuntimeLogLevel.WARNING, message)
    }

    @Synchronized
    fun error(message: String) {
        log(RuntimeLogLevel.ERROR, message)
    }

    @Synchronized
    fun getLogs(): List<RuntimeLogEntry> {
        return logs.toList()
    }

    @Synchronized
    fun clear() {
        logs.clear()
        _logEntries.value = emptyList()
    }

    fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat(
            "HH:mm:ss",
            Locale.getDefault()
        ).format(Date(timestamp))
    }
}
