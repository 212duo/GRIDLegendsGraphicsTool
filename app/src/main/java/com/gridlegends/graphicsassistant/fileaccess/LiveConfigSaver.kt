package com.gridlegends.graphicsassistant.fileaccess

import com.gridlegends.graphicsassistant.data.ConfigParser
import com.gridlegends.graphicsassistant.data.ConfigWriter
import com.gridlegends.graphicsassistant.data.ParamValue

sealed interface SaveOutcome {
    /** [content] is what is now on disk. */
    data class Saved(val content: String) : SaveOutcome
    data class Failed(val message: String) : SaveOutcome
}

/**
 * Save and restore pipeline shared by every access mode: render and verify first, back up the
 * current file, replace it with read-back confirmation, then prune old backups.
 */
object LiveConfigSaver {

    fun save(
        gateway: ConfigFileGateway,
        paramValues: List<ParamValue>,
        nowMillis: Long = System.currentTimeMillis()
    ): SaveOutcome {
        val original = gateway.read() ?: return SaveOutcome.Failed("读取配置失败，请检查权限")
        val updated = ConfigWriter.renderVerified(original, paramValues).getOrElse { error ->
            return SaveOutcome.Failed(error.message ?: "写入内容校验失败")
        }
        if (updated == original) return SaveOutcome.Saved(original)
        return replaceWithBackup(gateway, original, updated, nowMillis)
    }

    fun restore(
        gateway: ConfigFileGateway,
        backupName: String,
        nowMillis: Long = System.currentTimeMillis()
    ): SaveOutcome {
        val backup = gateway.readBackup(backupName) ?: return SaveOutcome.Failed("读取备份失败")
        val parsed = runCatching { ConfigParser.parse(backup.byteInputStream()) }.getOrNull()
        if (parsed.isNullOrEmpty()) return SaveOutcome.Failed("备份文件无法解析，未恢复")

        val current = gateway.read() ?: return SaveOutcome.Failed("读取当前配置失败")
        if (current == backup) return SaveOutcome.Saved(current)
        return replaceWithBackup(gateway, current, backup, nowMillis)
    }

    private fun replaceWithBackup(
        gateway: ConfigFileGateway,
        current: String,
        updated: String,
        nowMillis: Long
    ): SaveOutcome {
        if (!gateway.createBackup(ConfigBackups.newName(nowMillis), current)) {
            return SaveOutcome.Failed("创建备份失败，已取消保存")
        }
        if (!gateway.replace(updated, current)) {
            return SaveOutcome.Failed("写入校验失败，已尝试还原原配置；如有异常可通过“恢复备份”还原")
        }
        ConfigBackups.namesToPrune(gateway.listBackups()).forEach(gateway::deleteBackup)
        return SaveOutcome.Saved(updated)
    }
}
