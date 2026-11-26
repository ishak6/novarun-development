package com.mangalocalrunnerplus

import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class NodeRuntimeLimitsTest {
    @Test
    fun commandArgumentsApplyExplicitV8HeapLimitsBeforeEntryPoint() {
        val entry = File("/projects/store/server.js")
        assertArrayEquals(
            arrayOf(
                "node",
                "--max-old-space-size=256",
                "--max-semi-space-size=16",
                "/projects/store/server.js"
            ),
            NodeRuntimeLimits.commandArguments(entry)
        )
    }
}
