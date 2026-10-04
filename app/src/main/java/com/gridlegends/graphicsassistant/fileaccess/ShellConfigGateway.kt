package com.gridlegends.graphicsassistant.fileaccess

import java.util.Base64

data class ShellResult(
    val output: String,
    val exitCode: Int
)

/**
 * Config access through a privileged shell, shared by the Shizuku and Root modes.
 *
 * Content is staged as base64 chunks in a temp file and decoded before the target is touched,
 * so a failed transfer never truncates the game config. Every write is confirmed by reading it
 * back.
 */
class ShellConfigGateway(
    private val configPath: String,
    private val exec: (String) -> ShellResult
) : ConfigFileGateway {

    private val directory = configPath.substringBeforeLast('/')
    private val stagedBase64 = "$directory/.glgraphics.tmp.b64"
    private val stagedFile = "$directory/.glgraphics.tmp"

    override fun read(): String? = readFile(configPath)

    override fun replace(content: String, previous: String): Boolean {
        if (writeFile(configPath, content) && read() == content) return true
        writeFile(configPath, previous)
        return false
    }

    override fun listBackups(): List<String> {
        val result = exec("ls -1 ${quote(directory)}")
        if (result.exitCode != 0) return emptyList()
        return result.output.lines().map(String::trim).filter(ConfigBackups::isBackupName)
    }

    override fun readBackup(name: String): String? =
        if (ConfigBackups.isBackupName(name)) readFile(backupPath(name)) else null

    override fun createBackup(name: String, content: String): Boolean {
        if (!ConfigBackups.isBackupName(name)) return false
        val path = backupPath(name)
        return writeFile(path, content) && readFile(path) == content
    }

    override fun deleteBackup(name: String): Boolean {
        if (!ConfigBackups.isBackupName(name)) return false
        return exec("rm -f ${quote(backupPath(name))}").exitCode == 0
    }

    override fun isGameRunning(): Boolean? = when (exec("pidof ${ConfigLocations.GAME_PACKAGE}").exitCode) {
        0 -> true
        1 -> false
        else -> null
    }

    override fun stopGame(): Boolean =
        exec("am force-stop ${ConfigLocations.GAME_PACKAGE}").exitCode == 0

    private fun backupPath(name: String) = "$directory/$name"

    private fun readFile(path: String): String? {
        val result = exec("cat ${quote(path)}")
        return result.output.takeIf { result.exitCode == 0 && it.isNotEmpty() }
    }

    private fun writeFile(target: String, content: String): Boolean {
        val encoded = Base64.getEncoder().encodeToString(content.toByteArray(Charsets.UTF_8))
        val cleanup = "rm -f ${quote(stagedBase64)} ${quote(stagedFile)}"
        try {
            if (exec(cleanup).exitCode != 0) return false
            encoded.chunked(CHUNK_LENGTH).forEach { chunk ->
                // Base64 output only uses [A-Za-z0-9+/=], so single quotes are safe.
                if (exec("printf '%s' '$chunk' >> ${quote(stagedBase64)}").exitCode != 0) return false
            }
            return exec(
                "base64 -d < ${quote(stagedBase64)} > ${quote(stagedFile)} && " +
                    "cat ${quote(stagedFile)} > ${quote(target)}"
            ).exitCode == 0
        } finally {
            exec(cleanup)
        }
    }

    private fun quote(path: String) = "\"$path\""

    private companion object {
        const val CHUNK_LENGTH = 16_000
    }
}
