package com.gridlegends.graphicsassistant.fileaccess

import rikka.shizuku.Shizuku

private fun log(msg: String) = OperationLog.add(msg)

/** Blocking Shizuku shell access; call every method off the main thread. */
class ShizukuFileOperator {
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
                log("反射失败 type=${e.javaClass.simpleName}")
            }
            return newProcessMethod
        }
    }

    var lastDiag: String = ""
        private set

    private fun exec(command: String): Pair<String, Int> {
        return try {
            val method = getNewProcessMethod()
            if (method == null) {
                log("ERROR newProcess 不可用")
                return Pair("", -1)
            }
            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            log(safeCommandSummary("SHIZUKU", command, output, error, exitCode))
            Pair(output, exitCode)
        } catch (e: Exception) {
            log("ERROR exec failed type=${e.javaClass.simpleName}")
            Pair("", -1)
        }
    }

    fun gateway(configPath: String): ConfigFileGateway =
        ShellConfigGateway(configPath) { command ->
            val (output, exitCode) = exec(command)
            ShellResult(output, exitCode)
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
            log("ERROR binder type=${e.javaClass.simpleName}")
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
            val found = out.trim() == "EXISTS"
            log(safeFileLookupSummary("SHIZUKU", "direct", path, out, found, code))
            if (found) {
                return path
            }
        }
        for (name in CONFIG_FILE_NAMES) {
            val (out, code) = exec("find \"$GAME_DATA_DIR\" -name \"$name\" -type f 2>/dev/null | head -1")
            val foundPath = out.trim()
            val found = foundPath.isNotEmpty()
            log(safeFileLookupSummary("SHIZUKU", "recursive", foundPath, out, found, code))
            if (found) {
                return foundPath
            }
        }
        log("未找到配置文件")
        return null
    }

    fun readFile(filePath: String): String? {
        log("readFile 开始")
        val (out, code) = exec("cat \"$filePath\"")
        return if (code == 0 && out.isNotEmpty()) {
            log("readFile 成功: ${out.length} 字符")
            out
        } else {
            log("WARN readFile 失败: exit=$code empty=${out.isEmpty()}")
            null
        }
    }
}
