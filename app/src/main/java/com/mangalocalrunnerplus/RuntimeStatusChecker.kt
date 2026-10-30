
package com.mangalocalrunnerplus

import android.content.Context

data class RuntimeCheckResult(
    val language: String,
    val status: RuntimeStatus,
    val message: String,
    val executablePath: String? = null
)

object RuntimeStatusChecker {

    fun checkAll(context: Context): List<RuntimeCheckResult> {
        return RuntimeCapabilityManager
            .getAllCapabilities()
            .map { capability ->
                checkRuntime(context, capability.language)
            }
    }

    fun checkRuntime(
        context: Context,
        language: String
    ): RuntimeCheckResult {

        val capability =
            RuntimeCapabilityManager.getCapability(language)

        if (capability.status == RuntimeStatus.NOT_SUPPORTED) {
            return RuntimeCheckResult(
                language = language,
                status = RuntimeStatus.NOT_SUPPORTED,
                message = capability.description
            )
        }

        if (capability.runtimeType == RuntimeType.WEB_VIEW) {
            return RuntimeCheckResult(
                language = language,
                status = RuntimeStatus.AVAILABLE_FOR_PREVIEW,
                message = "Available for Android WebView preview."
            )
        }

        val runtimeDirectory = context.getDir(
            "runtime",
            Context.MODE_PRIVATE
        )

        val runtimeFile = java.io.File(
            runtimeDirectory,
            language.lowercase().replace(" ", "_")
        )

        if (runtimeFile.exists() && runtimeFile.isDirectory) {
            return RuntimeCheckResult(
                language = language,
                status = RuntimeStatus.NOT_CONFIGURED,
                message = "Runtime directory exists, but executable verification is required.",
                executablePath = runtimeFile.absolutePath
            )
        }

        return RuntimeCheckResult(
            language = language,
            status = RuntimeStatus.NOT_CONFIGURED,
            message = "Runtime is not installed. An adapter is required."
        )
    }

    fun getStatusLabel(status: RuntimeStatus): String {
        return when (status) {
            RuntimeStatus.AVAILABLE_FOR_PREVIEW ->
                "Available for Preview"

            RuntimeStatus.ADAPTER_PLANNED ->
                "Adapter Planned"

            RuntimeStatus.NOT_CONFIGURED ->
                "Not Configured"

            RuntimeStatus.NOT_SUPPORTED ->
                "Not Supported"
        }
    }
}
