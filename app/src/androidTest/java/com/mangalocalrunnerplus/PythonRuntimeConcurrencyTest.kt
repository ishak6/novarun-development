package com.mangalocalrunnerplus

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PythonRuntimeConcurrencyTest {
    @Test
    fun concurrentProjectsDoNotShareInterpreterProcessState() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val suffix = UUID.randomUUID().toString()
        val firstRoot = File(context.cacheDir, "novarun-python-first-$suffix").apply { check(mkdirs()) }
        val secondRoot = File(context.cacheDir, "novarun-python-second-$suffix").apply { check(mkdirs()) }

        try {
            val script = """
                import os
                import time
                print("START=" + os.getcwd())
                time.sleep(0.4)
                print("END=" + os.getcwd())
            """.trimIndent()
            val firstScript = File(firstRoot, "main.py").apply { writeText(script) }
            val secondScript = File(secondRoot, "main.py").apply { writeText(script) }

            val first = async(Dispatchers.IO) {
                PythonRuntimeAdapter.execute(
                    context,
                    RuntimeExecutionRequest(firstRoot, firstScript.absolutePath)
                )
            }
            val second = async(Dispatchers.IO) {
                PythonRuntimeAdapter.execute(
                    context,
                    RuntimeExecutionRequest(secondRoot, secondScript.absolutePath)
                )
            }

            val firstResult = first.await()
            val secondResult = second.await()

            assertTrue("First Python project failed: ${firstResult.error}", firstResult.success)
            assertTrue("Second Python project failed: ${secondResult.error}", secondResult.success)
            assertTrue(firstResult.output.contains("START=${firstRoot.canonicalPath}"))
            assertTrue(firstResult.output.contains("END=${firstRoot.canonicalPath}"))
            assertFalse(firstResult.output.contains(secondRoot.canonicalPath))
            assertTrue(secondResult.output.contains("START=${secondRoot.canonicalPath}"))
            assertTrue(secondResult.output.contains("END=${secondRoot.canonicalPath}"))
            assertFalse(secondResult.output.contains(firstRoot.canonicalPath))
        } finally {
            firstRoot.deleteRecursively()
            secondRoot.deleteRecursively()
        }
    }
}
