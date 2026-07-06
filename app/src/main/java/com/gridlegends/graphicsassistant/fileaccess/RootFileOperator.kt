package com.gridlegends.graphicsassistant.fileaccess

import android.util.Base64
import android.util.Log
import com.gridlegends.graphicsassistant.data.ConfigParser
import com.gridlegends.graphicsassistant.data.ConfigWriter
import com.gridlegends.graphicsassistant.data.ParamValue
import java.io.ByteArrayOutputStream

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

    private fun log(message: String) {
        val entry = "[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}] ROOT $message"
        logBuffer.add(entry)
        if (logBuffer.size > 200) logBuffer.removeAt(0)
        Log.d("GLGraphics", "ROOT $message")
    }

    private fun exec(command: String): Pair<String, Int> {
        log("exec: $command")
        val result = rootManager.runRootCommand(command)
        log("exit=${result.exitCode} out=${result.output.take(100)}")
        return result.output to result.exitCode
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
            log("检查 $path -> ${out.trim()} exit=$code")
            if (out.trim() == "EXISTS") return path
        }
        for (name in CONFIG_FILE_NAMES) {
            val (out, _) = exec("find \"$GAME_DATA_DIR\" -name \"$name\" -type f 2>/dev/null | head -1")
            val found = out.trim()
            if (found.isNotEmpty()) return found
        }
        return null
    }

    fun readConfig(): Map<String, String>? {
        val filePath = findConfigFile() ?: return null
        return readConfigFromPath(filePath)
    }

    fun readConfigFromPath(filePath: String): Map<String, String>? {
        val content = readFile(filePath) ?: return null
        return ConfigParser.parse(content.byteInputStream())
    }

    fun readFile(filePath: String): String? {
        val (out, code) = exec("cat \"$filePath\"")
        return if (code == 0 && out.isNotEmpty()) out else null
    }

    fun writeConfigToPath(filePath: String, paramValues: Map<String, ParamValue>): Boolean {
        val originalContent = readFile(filePath) ?: return false
        val backupPath = "${filePath.substringBeforeLast('/')}/${ConfigWriter.backupFileName()}"
        exec("cp \"$filePath\" \"$backupPath\"")

        val outputStream = ByteArrayOutputStream()
        ConfigWriter.write(outputStream, paramValues.values.toList(), originalContent)
        val newContent = outputStream.toString("UTF-8")
        val encoded = Base64.encodeToString(
            newContent.toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP
        )
        val (_, code) = exec("echo '$encoded' | base64 -d > \"$filePath\"")
        return code == 0
    }

    fun writeConfig(paramValues: Map<String, ParamValue>): Boolean {
        val filePath = findConfigFile() ?: return false
        return writeConfigToPath(filePath, paramValues)
    }
}
