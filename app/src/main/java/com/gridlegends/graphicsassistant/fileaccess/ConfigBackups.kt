package com.gridlegends.graphicsassistant.fileaccess

/**
 * Naming and retention rules for the backups this app writes next to the game config.
 * Only names matching [isBackupName] are ever listed, restored or deleted.
 */
object ConfigBackups {
    const val KEEP_COUNT = 5

    private const val PREFIX = "preferences_backup_"

    // Older SAF builds created backups with an ".xml" suffix.
    private val namePattern = Regex("""^${PREFIX}(\d+)(\.[A-Za-z0-9]+)?$""")

    fun newName(nowMillis: Long): String = "$PREFIX$nowMillis"

    fun timestampOf(name: String): Long? =
        namePattern.matchEntire(name)?.groupValues?.get(1)?.toLongOrNull()

    fun isBackupName(name: String): Boolean = timestampOf(name) != null

    fun newestFirst(names: Collection<String>): List<String> =
        names.filter(::isBackupName).sortedByDescending { timestampOf(it) }

    fun namesToPrune(names: Collection<String>, keep: Int = KEEP_COUNT): List<String> =
        newestFirst(names).drop(keep)
}
