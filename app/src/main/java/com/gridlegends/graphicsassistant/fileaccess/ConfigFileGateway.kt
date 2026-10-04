package com.gridlegends.graphicsassistant.fileaccess

/**
 * Access to the live game config and its backups for one access mode (SAF, Shizuku or Root).
 * All methods block and must be called off the main thread.
 */
interface ConfigFileGateway {
    fun read(): String?

    /**
     * Replaces the config with [content] and confirms it by reading the file back. On mismatch
     * the implementation writes [previous] back and returns false.
     */
    fun replace(content: String, previous: String): Boolean

    fun listBackups(): List<String>

    fun readBackup(name: String): String?

    fun createBackup(name: String, content: String): Boolean

    fun deleteBackup(name: String): Boolean

    /** Returns null when this access mode cannot see other apps' processes. */
    fun isGameRunning(): Boolean?

    fun stopGame(): Boolean
}
