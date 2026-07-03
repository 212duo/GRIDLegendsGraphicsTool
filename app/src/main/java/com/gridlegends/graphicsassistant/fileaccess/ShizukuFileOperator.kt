package com.gridlegends.graphicsassistant.fileaccess

import android.content.Context
import android.util.Log
import com.gridlegends.graphicsassistant.data.ConfigParser
import com.gridlegends.graphicsassistant.data.ConfigWriter
import com.gridlegends.graphicsassistant.data.ParamValue
import rikka.shizuku.Shizuku
import java.io.ByteArrayOutputStream

internal val logBuffer = mutableListOf<String>()

private fun log(msg: String) {
    val entry = "[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}] $msg"
    logBuffer.add(entry)
    if (logBuffer.size > 200) logBuffer.removeAt(0)
    Log.d("GLGraphics", msg)
}

class ShizukuFileOperator(
    private val context: Context,
    private val shizukuManager: ShizukuManager
) {
    companion object {
        private const val GAME_DATA_DIR = ConfigLocations.GAME_DATA_DIR
        private const val PREFS_SUBDIR = ConfigLocations.PREFS_SUBDIR

        val CONFIG_FILE_NAMES = listOf(
            ConfigLocations.CONFIG_FILE_NAME
        )

        private var newProcessMethod: java.lang.reflect.Method? = null
        private var methodResolved = false

        private fun getNewProcessMethod(): java.lang.reflect.Method? {
            if (methodResolved) return newProcessMethod
            methodResolved = true
            try {
                val methods = Shizuku::class.java.declaredMethods
                log("Shizuku 共 ${methods.size} 个方法")
                methods.forEach { m -> log("  - ${m.name}(${m.parameterTypes.joinToString { it.simpleName }})") }
                newProcessMethod = methods.firstOrNull { it.name == "newProcess" }
                newProcessMethod?.isAccessible = true
                log("newProcess: ${if (newProcessMethod != null) "找到" else "未找到"}")
            } catch (e: Exception) {
                log("反射失败: ${e.javaClass.simpleName}: ${e.message}")
            }
            return newProcessMethod
        }
    }

    var lastDiag: String = ""
        private set

    private fun exec(command: String): Pair<String, Int> {
        log("exec: $command")
        return try {
            val method = getNewProcessMethod()
            if (method == null) {
                log("WARN: newProcess 为空，用 fallback")
                return execFallback(command)
            }
            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            log("exec exit=$exitCode out=${output.take(100)} err=${error.take(100)}")
            Pair(output, exitCode)
        } catch (e: Exception) {
            log("ERROR exec: ${e.javaClass.simpleName}: ${e.message}")
            Pair("", -1)
        }
    }

    private fun execFallback(command: String): Pair<String, Int> {
        log("execFallback: $command")
        return try {
            val cls = Class.forName("rikka.shizuku.ShizukuRemoteProcess")
            val constructor = cls.getDeclaredConstructor(Array<String>::class.java)
            constructor.isAccessible = true
            val process = constructor.newInstance(arrayOf("sh", "-c", command)) as Process
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            log("execFallback exit=$exitCode out=${output.take(100)}")
            Pair(output, exitCode)
        } catch (e: Exception) {
            log("ERROR fallback: ${e.javaClass.simpleName}: ${e.message}")
            Pair("exec error: ${e.message}", -1)
        }
    }

    fun diagnose(): String {
        log("=== 开始诊断 ===")
        val sb = StringBuilder()

        try {
            val ping = Shizuku.pingBinder()
            sb.appendLine("Shizuku binder: ${if (ping) "OK" else "FAIL"}")
            log("binder: $ping")
        } catch (e: Exception) {
            sb.appendLine("Shizuku 错误: ${e.message}")
            log("ERROR binder: ${e.message}")
            lastDiag = sb.toString()
            return lastDiag
        }

        val method = getNewProcessMethod()
        sb.appendLine("newProcess: ${if (method != null) "OK" else "FAIL"}")

        val (existsOut, existsCode) = exec("[ -d \"$GAME_DATA_DIR\" ] && echo YES || echo NO")
        sb.appendLine("游戏目录: ${existsOut.trim()} (exit=$existsCode)")

        val (rootList, _) = exec("ls \"$GAME_DATA_DIR\" 2>&1")
        sb.appendLine("目录内容:")
        rootList.lines().take(20).forEach { sb.appendLine("  $it") }

        val prefsDir = "$GAME_DATA_DIR/$PREFS_SUBDIR"
        val (prefsList, _) = exec("ls \"$prefsDir\" 2>&1")
        sb.appendLine("\nferal_app_support目录:")
        prefsList.lines().take(20).forEach { sb.appendLine("  $it") }

        sb.appendLine("\nXML 文件:")
        val (xmlFind, _) = exec("find \"$GAME_DATA_DIR\" -name \"*.xml\" -type f 2>/dev/null")
        if (xmlFind.isNotBlank()) {
            xmlFind.lines().forEach { sb.appendLine("  $it") }
        } else {
            sb.appendLine("  (无)")
        }

        lastDiag = sb.toString()
        log("诊断完成")
        return lastDiag
    }

    private fun getPrefsDirPath(): String = "$GAME_DATA_DIR/$PREFS_SUBDIR"

    fun findConfigFile(): String? {
        log("findConfigFile 开始")
        val prefsDir = getPrefsDirPath()
        for (name in CONFIG_FILE_NAMES) {
            val path = "$prefsDir/$name"
            val (out, code) = exec("[ -f \"$path\" ] && echo EXISTS")
            log("检查 $path -> ${out.trim()} exit=$code")
            if (out.trim() == "EXISTS") {
                log("找到: $path")
                return path
            }
        }
        for (name in CONFIG_FILE_NAMES) {
            val (out, _) = exec("find \"$GAME_DATA_DIR\" -name \"$name\" -type f 2>/dev/null | head -1")
            val found = out.trim()
            log("搜索 $name -> $found")
            if (found.isNotEmpty()) {
                log("搜索找到: $found")
                return found
            }
        }
        log("未找到配置文件")
        return null
    }

    fun readConfig(): Map<String, String>? {
        log("readConfig 开始")
        val filePath = findConfigFile() ?: return null
        return readConfigFromPath(filePath)
    }

    fun readConfigFromPath(filePath: String): Map<String, String>? {
        log("readConfigFromPath: $filePath")
        val content = readFile(filePath) ?: return null
        log("readConfigFromPath: 解析 ${content.length} 字符")
        return ConfigParser.parse(content.byteInputStream())
    }

    fun readFile(filePath: String): String? {
        log("readFile: $filePath")
        val (out, code) = exec("cat \"$filePath\"")
        return if (code == 0 && out.isNotEmpty()) {
            log("readFile 成功: ${out.length} 字符")
            out
        } else {
            log("WARN readFile 失败: exit=$code empty=${out.isEmpty()}")
            null
        }
    }

    fun writeConfigToPath(filePath: String, paramValues: Map<String, ParamValue>): Boolean {
        log("writeConfigToPath: $filePath")
        val originalContent = readFile(filePath) ?: return false
        val backupPath = "${filePath.substringBeforeLast('/')}/${ConfigWriter.backupFileName()}"
        exec("cp \"$filePath\" \"$backupPath\"")
        val outputStream = ByteArrayOutputStream()
        ConfigWriter.write(outputStream, paramValues.values.toList(), originalContent)
        val newContent = outputStream.toString("UTF-8")
        val encoded = android.util.Base64.encodeToString(
            newContent.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP
        )
        val (_, code) = exec("echo '$encoded' | base64 -d > \"$filePath\"")
        log("writeConfigToPath exit=$code")
        return code == 0
    }

    fun writeConfig(paramValues: Map<String, ParamValue>): Boolean {
        log("writeConfig 开始")
        val filePath = findConfigFile() ?: return false
        return writeConfigToPath(filePath, paramValues)
    }
}
