package com.mangalocalrunnerplus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoundedRuntimeOutputTest {
    @Test
    fun preservesLinesWithinLimit() {
        val output = BoundedRuntimeOutput(128)
        output.appendLine("first")
        output.appendLine("second")
        assertEquals("first\nsecond\n", output.render())
    }

    @Test
    fun truncatesAndKeepsMarkerInsideLimit() {
        val output = BoundedRuntimeOutput(64)
        output.appendLine("x".repeat(100))
        val rendered = output.render()
        assertTrue(rendered.contains("Output truncated"))
        assertTrue(rendered.length <= 64)
    }

    @Test
    fun ignoresFurtherWritesAfterTruncation() {
        val output = BoundedRuntimeOutput(64)
        output.appendLine("x".repeat(100))
        val first = output.render()
        output.appendLine("late write")
        assertEquals(first, output.render())
    }
}
