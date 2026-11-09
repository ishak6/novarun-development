package com.mangalocalrunnerplus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeCapabilityManagerTest {
    @Test
    fun executableRuntimesAreDistinctFromPreviewOnlyAssets() {
        assertEquals(RuntimeStatus.AVAILABLE_FOR_EXECUTION, RuntimeCapabilityManager.getCapability("Python").status)
        assertEquals(RuntimeStatus.AVAILABLE_FOR_EXECUTION, RuntimeCapabilityManager.getCapability("JavaScript").status)
        assertEquals(RuntimeStatus.AVAILABLE_FOR_EXECUTION, RuntimeCapabilityManager.getCapability("SQL").status)
        listOf("HTML", "CSS", "SVG").forEach {
            assertEquals("$it must be preview-only", RuntimeStatus.AVAILABLE_FOR_PREVIEW, RuntimeCapabilityManager.getCapability(it).status)
        }
    }

    @Test
    fun unsupportedCompilerAndInterpreterIntegrationsRemainPlanned() {
        assertEquals("Node.js has an embedded runner", RuntimeStatus.AVAILABLE_FOR_EXECUTION, RuntimeCapabilityManager.getCapability("Node.js").status)
        listOf("PHP", "npm", "Java", "Kotlin", "C", "C++").forEach {
            assertEquals("$it must not be reported as installed", RuntimeStatus.ADAPTER_PLANNED, RuntimeCapabilityManager.getCapability(it).status)
        }
    }

    @Test
    fun browserJavaScriptIsPreviewableAndStandaloneJavaScriptCanExecute() {
        val javascript = RuntimeCapabilityManager.getCapability("JavaScript")
        assertTrue(javascript.canPreviewInWebView)
        assertTrue(javascript.description.contains("Rhino"))
        assertTrue(javascript.description.contains("not Node.js"))
    }

    @Test
    fun runtimeOverviewSeparatesExecutionFromPreview() {
        val overview = RuntimeOverviewManager.create(
            listOf(
                RuntimeCheckResult("SQL", RuntimeStatus.AVAILABLE_FOR_EXECUTION, "ready"),
                RuntimeCheckResult("JavaScript", RuntimeStatus.AVAILABLE_FOR_EXECUTION, "ready"),
                RuntimeCheckResult("HTML", RuntimeStatus.AVAILABLE_FOR_PREVIEW, "preview"),
                RuntimeCheckResult("Python", RuntimeStatus.ADAPTER_PLANNED, "planned"),
                RuntimeCheckResult("PHP", RuntimeStatus.NOT_CONFIGURED, "missing"),
                RuntimeCheckResult("Unknown", RuntimeStatus.NOT_SUPPORTED, "unsupported")
            )
        )
        assertEquals(6, overview.totalLanguages)
        assertEquals(2, overview.executionAvailable)
        assertEquals(1, overview.previewAvailable)
        assertEquals(1, overview.adapterPlanned)
        assertEquals(1, overview.notConfigured)
        assertEquals(1, overview.notSupported)
    }
}
