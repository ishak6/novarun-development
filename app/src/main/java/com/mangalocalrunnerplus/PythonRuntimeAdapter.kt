package com.mangalocalrunnerplus

import android.content.Context
import com.chaquo.python.android.AndroidPlatform
import com.chaquo.python.Python
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

private const val MAX_PYTHON_SCRIPT_BYTES = 1_000_000L
private const val MAX_PYTHON_OUTPUT_CHARS = 48_000

// Chaquopy embeds one process-wide interpreter. The runner temporarily changes
// cwd, sys.path, sys.settrace, stdout and stderr, so two scripts must never
// enter it concurrently from different Dispatchers.IO threads.
private val pythonExecutionMutex = Mutex()

object PythonRuntimeAdapter : RuntimeAdapter {
    override val language: String = "Python"

    override fun isAvailable(context: Context): Boolean = true

    override suspend fun execute(
        context: Context,
        request: RuntimeExecutionRequest
    ): RuntimeExecutionResult = withContext(Dispatchers.IO) {
        pythonExecutionMutex.withLock {
            val root = try {
                request.projectDirectory.canonicalFile
            } catch (_: Exception) {
                return@withLock RuntimeExecutionResult(false, "", "Project directory is unavailable.")
            }
            val script = try {
                File(request.entryFile).canonicalFile
            } catch (_: Exception) {
                return@withLock RuntimeExecutionResult(false, "", "Invalid Python script path.")
            }

            if (!root.isDirectory || !script.isFile ||
                !script.path.startsWith(root.path + File.separator)
            ) {
                return@withLock RuntimeExecutionResult(
                    false, "", "Python script must be inside the selected project."
                )
            }
            if (script.length() > MAX_PYTHON_SCRIPT_BYTES) {
                return@withLock RuntimeExecutionResult(
                    false, "", "Python script exceeds the 1 MB execution limit."
                )
            }

            try {
                ensureStarted(context)
                val result = Python.getInstance()
                    .getModule("novarun_runner")
                    .callAttr("run_script", script.absolutePath, root.absolutePath)
                    .toString()
                val json = JSONObject(result)
                val output = json.optString("output")
                val error = json.optString("error")
                RuntimeExecutionResult(
                    success = json.optBoolean("success"),
                    output = if (output.length > MAX_PYTHON_OUTPUT_CHARS) {
                        output.take(MAX_PYTHON_OUTPUT_CHARS) +
                            "\nOutput truncated at 48,000 characters."
                    } else output,
                    error = if (error.length > MAX_PYTHON_OUTPUT_CHARS) {
                        error.take(MAX_PYTHON_OUTPUT_CHARS) +
                            "\nError output truncated at 48,000 characters."
                    } else error,
                    exitCode = if (json.optBoolean("success")) 0 else 1
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                RuntimeExecutionResult(
                    false, "", error.message ?: "Python execution failed."
                )
            }
        }
    }

    @Synchronized
    private fun ensureStarted(context: Context) {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context.applicationContext))
        }
    }
}
