package com.gridlegends.graphicsassistant.fileaccess

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

/**
 * 文件读写操作
 * 基于 DocumentFile 进行 SAF 文件操作，所有方法都会阻塞，需在后台线程调用
 */
class FileOperator(
    private val context: Context,
    private val safManager: SafManager
) {
    /** 未授权或未找到配置文件时返回 null */
    fun gateway(): ConfigFileGateway? {
        val file = safManager.findConfigFile() ?: return null
        return SafConfigGateway(context, file)
    }
}

private class SafConfigGateway(
    context: Context,
    private val file: DocumentFile
) : ConfigFileGateway {

    private val resolver = context.contentResolver
    private val directory: DocumentFile? = file.parentFile

    override fun read(): String? = readUri(file.uri)

    override fun replace(content: String, previous: String): Boolean {
        if (writeUri(file.uri, content) && read() == content) return true
        writeUri(file.uri, previous)
        return false
    }

    override fun listBackups(): List<String> =
        directory?.listFiles()
            ?.mapNotNull { it.name }
            ?.filter(ConfigBackups::isBackupName)
            .orEmpty()

    override fun readBackup(name: String): String? = findBackup(name)?.let { readUri(it.uri) }

    override fun createBackup(name: String, content: String): Boolean {
        if (!ConfigBackups.isBackupName(name)) return false
        val created = runCatching {
            directory?.createFile("application/octet-stream", name)
        }.getOrNull() ?: return false
        return writeUri(created.uri, content) && readUri(created.uri) == content
    }

    override fun deleteBackup(name: String): Boolean =
        runCatching { findBackup(name)?.delete() == true }.getOrDefault(false)

    // SAF cannot see other apps' processes; the editor shows a reminder instead.
    override fun isGameRunning(): Boolean? = null

    override fun stopGame(): Boolean = false

    private fun findBackup(name: String): DocumentFile? =
        if (ConfigBackups.isBackupName(name)) directory?.findFile(name) else null

    private fun readUri(uri: Uri): String? = runCatching {
        resolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
    }.getOrNull()

    private fun writeUri(uri: Uri, content: String): Boolean = runCatching {
        resolver.openOutputStream(uri, "wt")?.use { output ->
            output.write(content.toByteArray(Charsets.UTF_8))
            output.flush()
            true
        } ?: false
    }.getOrDefault(false)
}
