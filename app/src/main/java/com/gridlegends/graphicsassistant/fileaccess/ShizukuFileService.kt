package com.gridlegends.graphicsassistant.fileaccess

import java.io.File

/**
 * Shizuku User Service 实现
 * 在 Shizuku 进程中运行，拥有 shell 权限（UID 2000）
 * 可以直接访问 Android/data/ 目录
 */
class ShizukuFileService : IShizukuFileService.Stub {

    constructor() : super()

    @Suppress("unused")
    constructor(data: android.os.Parcel) : this()

    override fun readFile(path: String?): String? {
        if (path == null) return null
        return try {
            val file = File(path)
            if (!file.exists() || !file.isFile) return null
            file.readText(Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    override fun writeFile(path: String?, content: String?): Boolean {
        if (path == null || content == null) return false
        return try {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.writeText(content, Charsets.UTF_8)
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun fileExists(path: String?): Boolean {
        if (path == null) return false
        return try {
            File(path).exists()
        } catch (_: Exception) {
            false
        }
    }

    override fun listDir(path: String?): String {
        if (path == null) return ""
        return try {
            val dir = File(path)
            if (!dir.exists() || !dir.isDirectory) return ""
            dir.listFiles()?.joinToString("\n") { entry ->
                if (entry.isDirectory) "${entry.name}/" else entry.name
            } ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    override fun findFile(startDir: String?, fileName: String?): String {
        if (startDir == null || fileName == null) return ""
        return try {
            val dir = File(startDir)
            if (!dir.exists()) return ""
            findFileRecursive(dir, fileName) ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun findFileRecursive(dir: File, fileName: String): String? {
        val files = dir.listFiles() ?: return null
        for (file in files) {
            if (file.name == fileName && file.isFile) {
                return file.absolutePath
            }
            if (file.isDirectory) {
                val found = findFileRecursive(file, fileName)
                if (found != null) return found
            }
        }
        return null
    }
}
