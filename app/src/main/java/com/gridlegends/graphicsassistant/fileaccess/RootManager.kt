package com.gridlegends.graphicsassistant.fileaccess

import android.util.Log

class RootManager {
    fun checkRootAccess(): RootStatus {
        val result = runRootCommand("id -u")
        if (result.exitCode == 0 && result.output.trim() == "0") {
            return RootStatus.READY
        }
        return if (result.output.contains("not found", ignoreCase = true) ||
            result.output.contains("inaccessible", ignoreCase = true)
        ) {
            RootStatus.NOT_AVAILABLE
        } else {
            RootStatus.NOT_AUTHORIZED
        }
    }

    internal fun runRootCommand(command: String): RootCommandResult {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            Log.d(
                "GLGraphics",
                safeCommandSummary("ROOT", command, output, error, exitCode)
            )
            val merged = buildString {
                append(output)
                if (error.isNotBlank()) append(error)
            }
            RootCommandResult(merged, exitCode, stdout = output)
        } catch (e: Exception) {
            Log.d("GLGraphics", "ROOT exec failed type=${e.javaClass.simpleName}")
            RootCommandResult(e.message.orEmpty(), -1, stdout = "")
        }
    }
}

internal data class RootCommandResult(
    /** stdout followed by stderr, used for status detection messages. */
    val output: String,
    val exitCode: Int,
    /** stdout only; file contents must never include su diagnostics written to stderr. */
    val stdout: String
)

enum class RootStatus {
    UNKNOWN,
    NOT_AVAILABLE,
    NOT_AUTHORIZED,
    READY
}
