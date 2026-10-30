
package com.mangalocalrunnerplus

import android.content.Context
import java.io.File

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
                message = capability.description
            )
        }

        if (RuntimeAdapterRegistry.isRegistered(language)) {
            return RuntimeCheckResult(
                language = language,
                status = RuntimeStatus.NOT_CONFIGURED,
                message = "Adapter registered. Check whether its runtime is executable on this device."
            )
        }

        if (capability.status == RuntimeStatus.ADAPTER_PLANNED) {
            return RuntimeCheckResult(
                language = language,
                status = RuntimeStatus.ADAPTER_PLANNED,
                message = capability.description
            )
        }

        val runtimeDirectory = context.getDir(
            "runtime",
            Context.MODE_PRIVATE
        )

        val runtimeFile = File(
            runtimeDirectory,
            language.lowercase().replace(" ", "_")
        )

        return RuntimeCheckResult(
            language = language,
            status = RuntimeStatus.NOT_CONFIGURED,
            message = if (runtimeFile.exists()) {
                "Runtime files exist, but executable verification and an adapter are still required."
            } else {
                "Runtime is not installed."
            },
            executablePath = runtimeFile.takeIf { it.exists() }?.absolutePath
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
