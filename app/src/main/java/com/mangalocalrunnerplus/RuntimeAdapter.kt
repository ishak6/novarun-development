
package com.mangalocalrunnerplus

import android.content.Context
import java.io.File

data class RuntimeExecutionRequest(
    val projectDirectory: File,
    val entryFile: String,
    val arguments: List<String> = emptyList()
)

data class RuntimeExecutionResult(
    val success: Boolean,
    val output: String,
    val error: String = "",
    val exitCode: Int? = null
)

interface RuntimeAdapter {

    val language: String

    fun isAvailable(context: Context): Boolean

    suspend fun execute(
        context: Context,
        request: RuntimeExecutionRequest
    ): RuntimeExecutionResult
}

object RuntimeAdapterRegistry {

    private val adapters =
        mutableMapOf<String, RuntimeAdapter>()

    fun register(adapter: RuntimeAdapter) {
        adapters[adapter.language.lowercase()] = adapter
    }

    fun getAdapter(language: String): RuntimeAdapter? {
        return adapters[language.lowercase()]
    }

    fun getRegisteredLanguages(): List<String> {
        return adapters.values
            .map { it.language }
            .sorted()
    }

    fun isRegistered(language: String): Boolean {
        return adapters.containsKey(language.lowercase())
    }

    fun unregister(language: String) {
        adapters.remove(language.lowercase())
    }

    fun clear() {
        adapters.clear()
    }
}
