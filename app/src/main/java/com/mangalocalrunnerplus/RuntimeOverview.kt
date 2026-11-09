package com.mangalocalrunnerplus

data class RuntimeOverview(
    val totalLanguages: Int,
    val executionAvailable: Int,
    val previewAvailable: Int,
    val adapterPlanned: Int,
    val notConfigured: Int,
    val notSupported: Int
)

object RuntimeOverviewManager {
    fun create(results: List<RuntimeCheckResult>): RuntimeOverview = RuntimeOverview(
        totalLanguages = results.size,
        executionAvailable = results.count { it.status == RuntimeStatus.AVAILABLE_FOR_EXECUTION },
        previewAvailable = results.count { it.status == RuntimeStatus.AVAILABLE_FOR_PREVIEW },
        adapterPlanned = results.count { it.status == RuntimeStatus.ADAPTER_PLANNED },
        notConfigured = results.count { it.status == RuntimeStatus.NOT_CONFIGURED },
        notSupported = results.count { it.status == RuntimeStatus.NOT_SUPPORTED }
    )
}
