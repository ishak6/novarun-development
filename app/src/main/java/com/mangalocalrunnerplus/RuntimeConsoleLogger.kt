
package com.mangalocalrunnerplus

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

enum class RuntimeLogLevel {
    INFO,
    OUTPUT,
    WARNING,
    ERROR
}

data class RuntimeLogEntry(
    val timestamp: Long,
    val level: RuntimeLogLevel,
    val message: String,
    val id: Long = 0L
)

object RuntimeConsoleLogger {

    private const val MAX_LOGS = 1000

    private val nextId = AtomicLong(0L)

    private val logs = mutableListOf<RuntimeLogEntry>()

    private val _logEntries =
        MutableStateFlow<List<RuntimeLogEntry>>(emptyList())

    val logEntries: StateFlow<List<RuntimeLogEntry>> =
        _logEntries.asStateFlow()

    @Synchronized
    fun log(
        level: RuntimeLogLevel,
        message: String
    ) {
        val entry = RuntimeLogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            message = message,
            id = nextId.incrementAndGet()
        )

        logs.add(entry)

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
