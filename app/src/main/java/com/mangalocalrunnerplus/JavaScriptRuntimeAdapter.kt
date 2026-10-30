package com.mangalocalrunnerplus

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.javascript.Context as RhinoContext
import org.mozilla.javascript.Scriptable
import java.io.File

object JavaScriptRuntimeAdapter : RuntimeAdapter {

    override val language: String = "JavaScript"

    override fun isAvailable(context: Context): Boolean {
        return true
    }

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

        var output = ""

        try {
            val rhino = RhinoContext.enter()

            try {
                rhino.optimizationLevel = -1
                rhino.languageVersion = RhinoContext.VERSION_ES6

                val scope: Scriptable =
                    rhino.initSafeStandardObjects()

                val result = scriptFile.reader().use { reader ->
                    rhino.evaluateReader(
                        scope,
                        reader,
                        scriptFile.name,
                        1,
                        null
                    )
                }

                output = if (result == org.mozilla.javascript.Undefined.instance) {
                    "JavaScript executed successfully."
                } else {
                    RhinoContext.toString(result)
                }

                RuntimeExecutionResult(
                    success = true,
                    output = output,
                    exitCode = 0
                )

            } finally {
                RhinoContext.exit()
            }

        } catch (error: Exception) {
            RuntimeExecutionResult(
                success = false,
                output = output,
                error = error.message ?: "JavaScript execution error."
            )
        }
    }
}
