package com.gridlegends.graphicsassistant.fileaccess

/** Blocking root shell access; call every method off the main thread. */
class RootFileOperator(
    private val rootManager: RootManager
) {
    companion object {
        private const val GAME_DATA_DIR = ConfigLocations.GAME_DATA_DIR
        private const val PREFS_SUBDIR = ConfigLocations.PREFS_SUBDIR

        val CONFIG_FILE_NAMES = listOf(
            ConfigLocations.CONFIG_FILE_NAME
        )
    }

    private fun log(message: String) = OperationLog.add("ROOT $message")

    private fun exec(command: String): Pair<String, Int> {
        val result = rootManager.runRootCommand(command)
        log(safeCommandSummary("ROOT", command, result.stdout, "", result.exitCode))
        return result.stdout to result.exitCode
    }

    fun diagnose(): String {
        val sb = StringBuilder()
        val status = rootManager.checkRootAccess()
        sb.appendLine("Root 状态: $status")

        val (existsOut, existsCode) = exec("[ -d \"$GAME_DATA_DIR\" ] && echo YES || echo NO")
        sb.appendLine("游戏目录: ${existsOut.trim()} (exit=$existsCode)")

        val (rootList, _) = exec("ls \"$GAME_DATA_DIR\" 2>&1")
        sb.appendLine("目录内容:")
        rootList.lines().take(20).forEach { sb.appendLine("  $it") }

        val prefsDir = "$GAME_DATA_DIR/$PREFS_SUBDIR"
        val (prefsList, _) = exec("ls \"$prefsDir\" 2>&1")
        sb.appendLine("\nferal_app_support目录:")
        prefsList.lines().take(20).forEach { sb.appendLine("  $it") }

        return sb.toString()
    }

    private fun getPrefsDirPath(): String = "$GAME_DATA_DIR/$PREFS_SUBDIR"

    fun findConfigFile(): String? {
        log("findConfigFile 开始")
        val prefsDir = getPrefsDirPath()
        for (name in CONFIG_FILE_NAMES) {
            val path = "$prefsDir/$name"
            val (out, code) = exec("[ -f \"$path\" ] && echo EXISTS")
            val found = out.trim() == "EXISTS"
            log(safeFileLookupSummary("ROOT", "direct", path, out, found, code))
            if (found) return path
        }
        for (name in CONFIG_FILE_NAMES) {
            val (out, _) = exec("find \"$GAME_DATA_DIR\" -name \"$name\" -type f 2>/dev/null | head -1")
            val found = out.trim()
            if (found.isNotEmpty()) return found
        }
        return null
    }

    fun readFile(filePath: String): String? {
        val (out, code) = exec("cat \"$filePath\"")
        return if (code == 0 && out.isNotEmpty()) out else null
    }

    fun gateway(configPath: String): ConfigFileGateway =
        ShellConfigGateway(configPath) { command ->
            val (output, exitCode) = exec(command)
            ShellResult(output, exitCode)
        }
}
