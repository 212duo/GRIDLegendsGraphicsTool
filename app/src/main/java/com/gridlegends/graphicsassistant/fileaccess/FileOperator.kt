package com.gridlegends.graphicsassistant.fileaccess

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.gridlegends.graphicsassistant.data.ConfigParser
import com.gridlegends.graphicsassistant.data.ConfigWriter
import com.gridlegends.graphicsassistant.data.ParamValue
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 文件读写操作
 * 基于 DocumentFile 进行 SAF 文件操作
 */
class FileOperator(
    private val context: Context,
    private val safManager: SafManager
) {

    /**
     * 读取配置文件内容为字符串
     */
    fun readContent(file: DocumentFile): String {
        val uri = file.uri
        val resolver = context.contentResolver

        return try {
            resolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            } ?: throw IllegalStateException("无法打开文件输入流")
        } catch (e: SecurityException) {
            throw SecurityException("没有文件读取权限，请重新授权", e)
        }
    }

    /**
     * 读取配置文件并解析为原始键值对
     * 自动查找配置文件
     *
     * @return 参数键值对，未找到配置文件时返回 null
     */
    fun readConfig(): Map<String, String>? {
        val file = safManager.findConfigFile() ?: return null
        val uri = file.uri
        val resolver = context.contentResolver

        return resolver.openInputStream(uri)?.use { stream ->
            ConfigParser.parse(stream)
        }
    }

    /**
     * 保存参数到配置文件
     * 自动查找配置文件并创建备份
     *
     * @param paramValues 修改后的参数值映射
     * @return 是否保存成功
     */
    fun writeConfig(paramValues: Map<String, ParamValue>): Boolean {
        val file = safManager.findConfigFile() ?: return false
        val resolver = context.contentResolver

        // 读取原文件内容
        val originalContent = readContent(file)

        // 创建备份
        createBackup(file.parentFile, originalContent)

        // 写入修改后的配置
        val valuesList = paramValues.values.toList()
        return resolver.openOutputStream(file.uri, "wt")?.use { stream ->
            ConfigWriter.write(stream, valuesList, originalContent)
            true
        } ?: false
    }

    /**
     * 创建配置文件备份
     */
    private fun createBackup(parentDir: DocumentFile?, content: String) {
        if (parentDir == null) return

        try {
            val backupName = ConfigWriter.backupFileName()
            val backupFile = parentDir.createFile("application/xml", backupName)
            backupFile?.uri?.let { uri ->
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(content.toByteArray(Charsets.UTF_8))
                    stream.flush()
                }
            }
        } catch (_: Exception) {
            // 备份失败不影响主流程
        }
    }
}
