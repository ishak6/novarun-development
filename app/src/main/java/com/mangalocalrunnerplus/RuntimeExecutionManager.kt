
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

            RuntimeConsoleLogger.info(
                "Starting $language execution."
            )

            val validation =
                RuntimeExecutionValidator.validate(
                    projectDirectory = request.projectDirectory,
                    entryFile = request.entryFile
                )

            if (!validation.valid) {
                RuntimeConsoleLogger.error(
                    validation.message
                )

                return@withContext RuntimeExecutionResult(
                    success = false,
                    output = "",
                    error = validation.message
                )
            }

            val adapter =
                RuntimeAdapterRegistry.getAdapter(language)

            if (adapter == null) {
                val message =
                    "No runtime adapter registered for $language."

                RuntimeConsoleLogger.warning(message)

                return@withContext RuntimeExecutionResult(
                    success = false,
                    output = "",
                    error = message
                )
            }

            val available = try {
                adapter.isAvailable(context)
            } catch (error: Exception) {
                val message = error.message
                    ?: "Could not check runtime availability."

                RuntimeConsoleLogger.error(message)

                return@withContext RuntimeExecutionResult(
                    success = false,
                    output = "",
                    error = message
                )
            }

            if (!available) {
                val message =
                    "$language runtime is not available."

                RuntimeConsoleLogger.warning(message)

                return@withContext RuntimeExecutionResult(
                    success = false,
                    output = "",
                    error = message
                )
            }

            try {
                val result = adapter.execute(
                    context,
                    request.copy(
                        entryFile =
                            validation.entryFile!!.absolutePath
                    )
                )

                if (result.output.isNotBlank()) {
                    RuntimeConsoleLogger.output(
                        result.output
                    )
                }

                if (result.error.isNotBlank()) {
                    RuntimeConsoleLogger.error(
                        result.error
                    )
                }

                if (result.success) {
                    RuntimeConsoleLogger.info(
                        "$language execution completed successfully."
                    )
                } else {
                    RuntimeConsoleLogger.error(
                        "$language execution failed."
                    )
                }

                result

            } catch (error: Exception) {
                val message = error.message
                    ?: "Runtime execution failed."

                RuntimeConsoleLogger.error(message)

                RuntimeExecutionResult(
                    success = false,
                    output = "",
                    error = message
                )
            }
        }
    }
}
