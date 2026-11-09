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
    fun checkAll(context: Context): List<RuntimeCheckResult> =
        RuntimeCapabilityManager.getAllCapabilities().map { checkRuntime(context, it.language) }

    fun checkRuntime(context: Context, language: String): RuntimeCheckResult {
        val capability = RuntimeCapabilityManager.getCapability(language)

        if (capability.status == RuntimeStatus.NOT_SUPPORTED) {
            return RuntimeCheckResult(language, RuntimeStatus.NOT_SUPPORTED, capability.description)
        }

        if (language.equals("Node.js", ignoreCase = true)) {
            val nativeDir = File(context.applicationInfo.nativeLibraryDir)
            val nodeLibrary = File(nativeDir, "libnode.so")
            val bridgeLibrary = File(nativeDir, "libnovarun_node_bridge.so")
            val ready = nodeLibrary.isFile && bridgeLibrary.isFile
            return RuntimeCheckResult(
                language,
                if (ready) RuntimeStatus.AVAILABLE_FOR_EXECUTION else RuntimeStatus.NOT_CONFIGURED,
                if (ready) "Embedded Node.js Mobile runtime and JNI bridge are packaged for this device ABI. Use the Node Backend Runner; npm install is not available."
                else "Node.js native runtime libraries are missing for this device ABI.",
                nodeLibrary.takeIf { it.isFile }?.absolutePath
            )
        }

        val adapter = RuntimeAdapterRegistry.getAdapter(language)
        if (adapter != null) {
            val available = runCatching { adapter.isAvailable(context) }.getOrDefault(false)
            return RuntimeCheckResult(
                language,
                if (available) RuntimeStatus.AVAILABLE_FOR_EXECUTION else RuntimeStatus.NOT_CONFIGURED,
                if (available) "Runtime adapter is registered and reports ready for local execution."
                else "Runtime adapter is registered, but its runtime is currently unavailable."
            )
        }

        if (capability.status == RuntimeStatus.AVAILABLE_FOR_EXECUTION) {
            return RuntimeCheckResult(
                language,
                RuntimeStatus.NOT_CONFIGURED,
                "Execution is expected, but no runtime adapter is registered in this app session."
            )
        }

        if (capability.status == RuntimeStatus.AVAILABLE_FOR_PREVIEW) {
            return RuntimeCheckResult(language, RuntimeStatus.AVAILABLE_FOR_PREVIEW, capability.description)
        }

        if (capability.status == RuntimeStatus.ADAPTER_PLANNED) {
            return RuntimeCheckResult(language, RuntimeStatus.ADAPTER_PLANNED, capability.description)
        }

        val runtimeDirectory = context.getDir("runtime", Context.MODE_PRIVATE)
        val runtimeFile = File(runtimeDirectory, language.lowercase().replace(" ", "_"))
        return RuntimeCheckResult(
            language,
            RuntimeStatus.NOT_CONFIGURED,
            if (runtimeFile.exists()) "Runtime files exist, but executable verification and an adapter are still required."
            else "Runtime is not installed.",
            runtimeFile.takeIf { it.exists() }?.absolutePath
        )
    }

    fun getStatusLabel(status: RuntimeStatus): String = when (status) {
        RuntimeStatus.AVAILABLE_FOR_EXECUTION -> "Ready to Run"
        RuntimeStatus.AVAILABLE_FOR_PREVIEW -> "Preview Only"
        RuntimeStatus.ADAPTER_PLANNED -> "Planned"
        RuntimeStatus.NOT_CONFIGURED -> "Not Configured"
        RuntimeStatus.NOT_SUPPORTED -> "Not Supported"
    }
}
