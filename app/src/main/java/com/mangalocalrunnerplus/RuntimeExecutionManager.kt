
package com.mangalocalrunnerplus

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RuntimeExecutionManager {

    suspend fun execute(
        context: Context,
        language: String,
        request: RuntimeExecutionRequest
    ): RuntimeExecutionResult {

        return withContext(Dispatchers.IO) {

            val validation =
                RuntimeExecutionValidator.validate(
                    projectDirectory = request.projectDirectory,
                    entryFile = request.entryFile
                )

            if (!validation.valid) {
                return@withContext RuntimeExecutionResult(
                    success = false,
                    output = "",
                    error = validation.message
                )
            }

            val adapter =
                RuntimeAdapterRegistry.getAdapter(language)

            if (adapter == null) {
                return@withContext RuntimeExecutionResult(
                    success = false,
                    output = "",
                    error = "No runtime adapter registered for $language."
                )
            }

            val available = try {
                adapter.isAvailable(context)
            } catch (error: Exception) {
                return@withContext RuntimeExecutionResult(
                    success = false,
                    output = "",
                    error = error.message
                        ?: "Could not check runtime availability."
                )
            }

            if (!available) {
                return@withContext RuntimeExecutionResult(
                    success = false,
                    output = "",
                    error = "$language runtime is not available."
                )
            }

            try {
                adapter.execute(
                    context,
                    request.copy(
                        entryFile = validation.entryFile!!.absolutePath
                    )
                )
            } catch (error: Exception) {
                RuntimeExecutionResult(
                    success = false,
                    output = "",
                    error = error.message
                        ?: "Runtime execution failed."
                )
            }
        }
    }
}
