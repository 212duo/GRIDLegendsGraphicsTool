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
            val merged = buildString {
                append(output)
                if (error.isNotBlank()) append(error)
            }
            Log.d("GLGraphics", "root exit=$exitCode out=${merged.take(120)}")
            RootCommandResult(merged, exitCode)
        } catch (e: Exception) {
            Log.d("GLGraphics", "root error=${e.javaClass.simpleName}: ${e.message}")
            RootCommandResult(e.message.orEmpty(), -1)
        }
    }
}

internal data class RootCommandResult(
    val output: String,
    val exitCode: Int
)

enum class RootStatus {
    UNKNOWN,
    NOT_AVAILABLE,
    NOT_AUTHORIZED,
    READY
}
