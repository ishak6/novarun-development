package com.mangalocalrunnerplus

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.javascript.Context as RhinoContext
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.Scriptable
import java.io.File

private const val MAX_SCRIPT_BYTES = 1_000_000L
private const val MAX_SCRIPT_INSTRUCTIONS = 2_000_000L
private const val MAX_EXECUTION_TIME_MS = 5_000L
private const val INSTRUCTION_OBSERVER_THRESHOLD = 10_000

private class ScriptExecutionLimitException :
    RuntimeException("JavaScript execution stopped after reaching its time or instruction limit.")

private class BoundedRhinoContextFactory(
    private val deadlineNanos: Long
) : ContextFactory() {

    private var observedInstructions = 0L

    override fun makeContext(): RhinoContext {
        return super.makeContext().apply {
            optimizationLevel = -1
            languageVersion = RhinoContext.VERSION_ES6
            instructionObserverThreshold = INSTRUCTION_OBSERVER_THRESHOLD
        }
    }

    override fun observeInstructionCount(
        cx: RhinoContext,
        instructionCount: Int
    ) {
        observedInstructions += instructionCount

        if (
            observedInstructions > MAX_SCRIPT_INSTRUCTIONS ||
            System.nanoTime() > deadlineNanos
        ) {
            throw ScriptExecutionLimitException()
        }
    }
}

object JavaScriptRuntimeAdapter : RuntimeAdapter {

    override val language: String = "JavaScript"

    override fun isAvailable(context: Context): Boolean = true

    override suspend fun execute(
        context: Context,
        request: RuntimeExecutionRequest
    ): RuntimeExecutionResult = withContext(Dispatchers.IO) {

        val scriptFile = File(request.entryFile)

        if (!scriptFile.exists() || !scriptFile.isFile) {
            return@withContext RuntimeExecutionResult(
                success = false,
                output = "",
                error = "JavaScript file not found."
            )
        }

        if (!scriptFile.canonicalPath.startsWith(
                request.projectDirectory.canonicalPath + File.separator
            )
        ) {
            return@withContext RuntimeExecutionResult(
                success = false,
                output = "",
                error = "Script is outside the selected project."
            )
        }

        if (scriptFile.length() > MAX_SCRIPT_BYTES) {
            return@withContext RuntimeExecutionResult(
                success = false,
                output = "",
                error = "JavaScript file exceeds the 1 MB execution limit."
            )
        }

        try {
            val deadline = System.nanoTime() +
                MAX_EXECUTION_TIME_MS * 1_000_000L

            val output = BoundedRhinoContextFactory(deadline).call { rhino ->
                val scope: Scriptable = rhino.initSafeStandardObjects()

                val result = scriptFile.reader(Charsets.UTF_8).use { reader ->
                    rhino.evaluateReader(
                        scope,
                        reader,
                        scriptFile.name,
                        1,
                        null
                    )
                }

                if (result == org.mozilla.javascript.Undefined.instance) {
                    "JavaScript executed successfully."
                } else {
                    RhinoContext.toString(result)
                }
            }

            RuntimeExecutionResult(
                success = true,
                output = output,
                exitCode = 0
            )

        } catch (error: CancellationException) {
            throw error

        } catch (error: ScriptExecutionLimitException) {
            RuntimeExecutionResult(
                success = false,
                output = "",
                error = error.message
                    ?: "JavaScript execution limit reached."
            )

        } catch (error: Exception) {
            RuntimeExecutionResult(
                success = false,
                output = "",
                error = error.message ?: "JavaScript execution error."
            )
        }
    }
}
