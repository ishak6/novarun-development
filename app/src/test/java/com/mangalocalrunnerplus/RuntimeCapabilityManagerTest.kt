package com.mangalocalrunnerplus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeCapabilityManagerTest {

    @Test
    fun registeredSqliteRuntimeIsMarkedAvailable() {
        val sql = RuntimeCapabilityManager.getCapability("SQL")

        assertEquals(RuntimeType.DATABASE, sql.runtimeType)
        assertEquals(RuntimeStatus.AVAILABLE_FOR_PREVIEW, sql.status)
        assertTrue(sql.description.contains("SQLite"))
    }

    @Test
    fun runtimesWithoutAdaptersRemainPlanned() {
        assertEquals(
            RuntimeStatus.ADAPTER_PLANNED,
            RuntimeCapabilityManager.getCapability("Python").status
        )
        assertEquals(
            RuntimeStatus.ADAPTER_PLANNED,
            RuntimeCapabilityManager.getCapability("PHP").status
        )
    }

    @Test
    fun runtimeOverviewCountsEachStatus() {
        val overview = RuntimeOverviewManager.create(
            listOf(
                RuntimeCheckResult("SQL", RuntimeStatus.AVAILABLE_FOR_PREVIEW, "ready"),
                RuntimeCheckResult("JavaScript", RuntimeStatus.AVAILABLE_FOR_PREVIEW, "ready"),
                RuntimeCheckResult("Python", RuntimeStatus.ADAPTER_PLANNED, "planned"),
                RuntimeCheckResult("PHP", RuntimeStatus.NOT_CONFIGURED, "missing"),
                RuntimeCheckResult("Unknown", RuntimeStatus.NOT_SUPPORTED, "unsupported")
            )
        )

        assertEquals(5, overview.totalLanguages)
        assertEquals(2, overview.previewAvailable)
        assertEquals(1, overview.adapterPlanned)
        assertEquals(1, overview.notConfigured)
        assertEquals(1, overview.notSupported)
    }
}
