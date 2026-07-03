package com.gridlegends.graphicsassistant.fileaccess

import android.content.Context
import android.content.Intent
import android.content.UriPermission
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile

/**
 * SAF（Storage Access Framework）授权管理器
 * 管理游戏配置目录的访问权限
 */
class SafManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "saf_prefs"
        private const val KEY_TREE_URI = "tree_uri"
        private const val KEY_IS_AUTHORIZED = "is_authorized"

        /** 游戏配置文件所在子目录路径 */
        const val PREFS_SUBDIR = ConfigLocations.PREFS_SUBDIR

        /** 可能的配置文件名 */
        val CONFIG_FILE_NAMES = listOf(
            ConfigLocations.CONFIG_FILE_NAME
        )
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 获取已保存的目录 URI
     */
    fun getSavedTreeUri(): Uri? {
        val uriStr = prefs.getString(KEY_TREE_URI, null)
        return uriStr?.let { Uri.parse(it) }
    }

    /**
     * 检查是否已授权
     */
    fun isAuthorized(): Boolean {
        if (!prefs.getBoolean(KEY_IS_AUTHORIZED, false)) return false
        val uri = getSavedTreeUri() ?: return false

        // 检查持久化权限是否仍然有效
        return context.contentResolver.persistedUriPermissions.any { perm ->
            perm.uri == uri && perm.isReadPermission && perm.isWritePermission
        }
    }

    /**
     * 保存 SAF 授权结果
     *
     * @param treeUri 用户选择的目录 URI
     */
    fun saveAuthorization(treeUri: Uri) {
        // 获取持久化读写权限
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(treeUri, flags)

        prefs.edit()
            .putString(KEY_TREE_URI, treeUri.toString())
            .putBoolean(KEY_IS_AUTHORIZED, true)
            .apply()
    }

    /**
     * 清除授权（用户主动撤销）
     */
    fun clearAuthorization() {
        val uri = getSavedTreeUri()
        if (uri != null) {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.releasePersistableUriPermission(uri, flags)
            } catch (_: Exception) {
                // 权限可能已失效
            }
        }
        prefs.edit()
            .remove(KEY_TREE_URI)
            .putBoolean(KEY_IS_AUTHORIZED, false)
            .apply()
    }

    /**
     * 获取游戏配置目录的 DocumentFile
     *
     * @return 配置目录的 DocumentFile，未授权或目录不存在时返回 null
     */
    fun getPrefsDirectory(): DocumentFile? {
        val treeUri = getSavedTreeUri() ?: return null
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return null

        // 导航到 files/feral_app_support 子目录
        val parts = PREFS_SUBDIR.split("/")
        var current = rootDoc
        for (part in parts) {
            current = current.findFile(part) ?: return null
        }
        return current
    }

    /**
     * 查找配置文件
     *
     * @return 配置文件的 DocumentFile，未找到时返回 null
     */
    fun findConfigFile(): DocumentFile? {
        val prefsDir = getPrefsDirectory() ?: return null
        for (name in CONFIG_FILE_NAMES) {
            val file = prefsDir.findFile(name)
            if (file != null && file.exists() && file.isFile) {
                return file
            }
        }
        return null
    }

    /**
     * 创建 SAF 目录选择器 Intent
     */
    fun createOpenDocumentTreeIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION

            // 提示用户选择游戏目录
            putExtra(
                DocumentsContract.EXTRA_INITIAL_URI,
                Uri.parse("content://com.android.externalstorage.documents/document/primary%3AAndroid%2Fdata%2Fcom.feralinteractive.gridlegends_android")
            )
        }
    }

    /**
     * 检测游戏是否已安装
     */
    fun isGameInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(
                ConfigLocations.GAME_PACKAGE, 0
            )
            true
        } catch (_: Exception) {
            false
        }
    }
}
