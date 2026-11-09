package com.mangalocalrunnerplus

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.javascript.Context as RhinoContext
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.Scriptable
import java.io.File
import java.security.MessageDigest

private const val MAX_SCRIPT_BYTES = 1_000_000L
private const val MAX_SCRIPT_INSTRUCTIONS = 2_000_000L
private const val MAX_SCRIPT_OUTPUT_CHARS = 48_000
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
                output = if (output.length > MAX_SCRIPT_OUTPUT_CHARS) {
                    output.take(MAX_SCRIPT_OUTPUT_CHARS) + "\\nOutput truncated at 48,000 characters."
                } else {
                    output
                },
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


/**
 * Project-scoped SQLite engine. Each imported project receives a private
 * database outside its served project directory.
 */
object SqliteRuntimeAdapter : RuntimeAdapter {
    override val language: String = "SQL"
    override fun isAvailable(context: Context): Boolean = true

    override suspend fun execute(
        context: Context,
        request: RuntimeExecutionRequest
    ): RuntimeExecutionResult = withContext(Dispatchers.IO) {
        val root = request.projectDirectory.canonicalFile
        val script = File(request.entryFile).canonicalFile

        if (!script.isFile || !script.path.startsWith(root.path + File.separator)) {
            return@withContext RuntimeExecutionResult(false, "", "SQL script must be inside the selected project.")
        }
        if (script.length() > 2_000_000L) {
            return@withContext RuntimeExecutionResult(false, "", "SQL script exceeds the 2 MB limit.")
        }

        val statements = try {
            splitSqlStatements(script.readText(Charsets.UTF_8))
        } catch (error: Exception) {
            return@withContext RuntimeExecutionResult(false, "", error.message ?: "Invalid SQL script.")
        }
        if (statements.isEmpty()) {
            return@withContext RuntimeExecutionResult(false, "", "SQL script contains no statements.")
        }
        if (statements.size > 500) {
            return@withContext RuntimeExecutionResult(false, "", "SQL script exceeds the 500 statement limit.")
        }

        val directory = context.getDir("project_databases", Context.MODE_PRIVATE)
        if (!directory.exists() && !directory.mkdirs()) {
            return@withContext RuntimeExecutionResult(false, "", "Could not create the private database directory.")
        }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(root.path.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val dbFile = File(directory, "$digest.sqlite")
        var db: SQLiteDatabase? = null

        try {
            val database = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
            db = database
            database.setForeignKeyConstraintsEnabled(true)
            database.setMaximumSize(50L * 1024L * 1024L)
            val output = StringBuilder()
            database.beginTransaction()
            try {
                statements.forEachIndexed { index, statement ->
                    if (Thread.currentThread().isInterrupted) {
                        throw CancellationException("SQL execution cancelled.")
                    }
                    val sql = statement.trim()
                    val keyword = sql.split(Regex("\\s+"), limit = 2).first().uppercase()
                    if (keyword in setOf("ATTACH", "DETACH", "VACUUM", "BEGIN", "COMMIT", "ROLLBACK")) {
                        throw IllegalArgumentException("$keyword is disabled to isolate project databases.")
                    }
                    if (keyword == "PRAGMA" && sql.contains("=")) {
                        throw IllegalArgumentException("Changing SQLite PRAGMA settings is disabled.")
                    }

                    if (keyword in setOf("SELECT", "PRAGMA", "EXPLAIN", "WITH")) {
                        database.rawQuery(sql, null).use { cursor ->
                            appendSqlRows(output, cursor, index + 1)
                        }
                    } else {
                        database.execSQL(sql)
                        output.append("Statement ").append(index + 1).append(": executed.\n")
                    }
                    if (output.length > 48_000) {
                        throw IllegalStateException("SQL output limit reached (48,000 characters).")
                    }
                }
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
            RuntimeExecutionResult(true, output.toString().ifBlank { "SQL executed successfully." }, exitCode = 0)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            RuntimeExecutionResult(false, "", error.message ?: "SQLite execution failed.")
        } finally {
            runCatching { db?.close() }
        }
    }

    private fun appendSqlRows(output: StringBuilder, cursor: Cursor, number: Int) {
        output.append("Statement ").append(number).append(" (").append(cursor.count).append(" rows)\n")
        val columns = (0 until cursor.columnCount).take(20)
        output.append(columns.joinToString(" | ") { cursor.getColumnName(it) }).append('\n')
        var rows = 0
        while (cursor.moveToNext() && rows < 100) {
            output.append(columns.joinToString(" | ") { column ->
                if (cursor.isNull(column)) "NULL" else cursor.getString(column).orEmpty().take(500)
            }).append('\n')
            rows++
            if (output.length > 48_000) throw IllegalStateException("SQL output limit reached.")
        }
        if (cursor.count > 100) output.append("Only the first 100 rows are shown.\n")
    }

    private fun splitSqlStatements(source: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var quote: Char? = null
        var lineComment = false
        var blockComment = false
        var i = 0

        while (i < source.length) {
            val ch = source[i]
            val next = source.getOrNull(i + 1)
            if (lineComment) {
                if (ch == '\n') {
                    lineComment = false
                    current.append(ch)
                }
                i++
                continue
            }
            if (blockComment) {
                if (ch == '*' && next == '/') {
                    blockComment = false
                    i += 2
                } else i++
                continue
            }
            if (quote != null) {
                current.append(ch)
                val close = if (quote == '[') ']' else quote
                if (ch == close) {
                    if (next == close && quote != '[') {
                        current.append(next)
                        i += 2
                        continue
                    }
                    quote = null
                }
                i++
                continue
            }
            if (ch == '-' && next == '-') {
                lineComment = true
                i += 2
                continue
            }
            if (ch == '/' && next == '*') {
                blockComment = true
                i += 2
                continue
            }
            if (ch.code == 39 || ch.code == 34 || ch.code == 96 || ch == '[') {
                quote = ch
                current.append(ch)
                i++
                continue
            }
            if (ch == ';') {
                current.toString().trim().takeIf { it.isNotEmpty() }?.let(result::add)
                current.setLength(0)
            } else current.append(ch)
            i++
        }
        if (quote != null) throw IllegalArgumentException("Unterminated quoted value in SQL script.")
        if (blockComment) throw IllegalArgumentException("Unterminated block comment in SQL script.")
        current.toString().trim().takeIf { it.isNotEmpty() }?.let(result::add)
        return result
    }
}
