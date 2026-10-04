package com.gridlegends.graphicsassistant.fileaccess

import com.gridlegends.graphicsassistant.data.ConfigParser
import com.gridlegends.graphicsassistant.data.ConfigWriterRealFileTest.Companion.presetContent
import com.gridlegends.graphicsassistant.data.ParamValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveConfigSaverTest {

    @Test
    fun backupNamesAreParsedSortedAndPruned() {
        val names = listOf(
            "preferences",
            "preferences_backup_100",
            "preferences_backup_300.xml",
            "preferences_backup_200",
            "preferences_backup_abc",
            "other_backup_400"
        )

        assertEquals(300L, ConfigBackups.timestampOf("preferences_backup_300.xml"))
        assertNull(ConfigBackups.timestampOf("preferences_backup_abc"))
        assertEquals(
            listOf("preferences_backup_300.xml", "preferences_backup_200", "preferences_backup_100"),
            ConfigBackups.newestFirst(names)
        )
        assertEquals(listOf("preferences_backup_100"), ConfigBackups.namesToPrune(names, keep = 2))
    }

    @Test
    fun saveBacksUpThenWritesAndKeepsOnlyTheNewestBackups() {
        val original = presetContent("mid_dedicated")
        val gateway = FakeGateway(original).apply {
            (1L..6L).forEach { backups[ConfigBackups.newName(it)] = "old $it" }
        }

        val outcome = LiveConfigSaver.save(gateway, editedShadows(original, "3"), nowMillis = 1_000)

        assertTrue(outcome is SaveOutcome.Saved)
        assertEquals("3", parse(gateway.content)["Shadows"])
        assertEquals(original, gateway.backups[ConfigBackups.newName(1_000)])
        assertEquals(ConfigBackups.KEEP_COUNT, gateway.backups.size)
        assertFalse(gateway.backups.containsKey(ConfigBackups.newName(1)))
    }

    @Test
    fun saveWithoutChangesTouchesNothing() {
        val original = presetContent("mid_dedicated")
        val gateway = FakeGateway(original)
        val unchanged = ConfigParser.toParamValues(parse(original))

        val outcome = LiveConfigSaver.save(gateway, unchanged, nowMillis = 1_000)

        assertEquals(SaveOutcome.Saved(original), outcome)
        assertTrue(gateway.backups.isEmpty())
        assertEquals(0, gateway.replaceCalls)
    }

    @Test
    fun failedBackupCancelsTheSave() {
        val original = presetContent("mid_dedicated")
        val gateway = FakeGateway(original).apply { backupFails = true }

        val outcome = LiveConfigSaver.save(gateway, editedShadows(original, "3"), nowMillis = 1_000)

        assertTrue(outcome is SaveOutcome.Failed)
        assertEquals(original, gateway.content)
        assertEquals(0, gateway.replaceCalls)
    }

    @Test
    fun restoreBacksUpTheCurrentFileFirst() {
        val original = presetContent("mid_dedicated")
        val backup = presetContent("high_dedicated")
        val gateway = FakeGateway(original).apply { backups[ConfigBackups.newName(5)] = backup }

        val outcome = LiveConfigSaver.restore(gateway, ConfigBackups.newName(5), nowMillis = 1_000)

        assertEquals(SaveOutcome.Saved(backup), outcome)
        assertEquals(backup, gateway.content)
        assertEquals(original, gateway.backups[ConfigBackups.newName(1_000)])
    }

    @Test
    fun corruptBackupIsNotRestored() {
        val original = presetContent("mid_dedicated")
        val gateway = FakeGateway(original).apply { backups[ConfigBackups.newName(5)] = "<registry" }

        val outcome = LiveConfigSaver.restore(gateway, ConfigBackups.newName(5), nowMillis = 1_000)

        assertTrue(outcome is SaveOutcome.Failed)
        assertEquals(original, gateway.content)
    }

    @Test
    fun shellGatewayStagesChunksAndOnlyThenReplacesTheTarget() {
        val content = presetContent("extreme_final_120_hdr")
        val shell = FakeShell(mutableMapOf(CONFIG_PATH to "old"))
        val gateway = ShellConfigGateway(CONFIG_PATH, shell::exec)

        assertTrue(gateway.replace(content, "old"))

        assertEquals(content, shell.files[CONFIG_PATH])
        assertTrue("content should be sent in several chunks", shell.chunkWrites > 1)
        assertEquals(setOf(CONFIG_PATH), shell.files.keys)
    }

    @Test
    fun shellGatewayLeavesTheTargetIntactWhenDecodingFails() {
        val shell = FakeShell(mutableMapOf(CONFIG_PATH to "old")).apply { decodeFails = true }
        val gateway = ShellConfigGateway(CONFIG_PATH, shell::exec)

        assertFalse(gateway.replace("new", "old"))

        assertEquals("old", shell.files[CONFIG_PATH])
    }

    @Test
    fun shellGatewayManagesOnlyBackupFiles() {
        val shell = FakeShell(
            mutableMapOf(
                CONFIG_PATH to "current",
                "$DIRECTORY/preferences_backup_7" to "backup",
                "$DIRECTORY/notes.txt" to "keep"
            )
        )
        val gateway = ShellConfigGateway(CONFIG_PATH, shell::exec)

        assertEquals(listOf("preferences_backup_7"), gateway.listBackups())
        assertEquals("backup", gateway.readBackup("preferences_backup_7"))
        assertFalse(gateway.deleteBackup("notes.txt"))
        assertTrue(gateway.createBackup("preferences_backup_8", "current"))
        assertEquals("current", shell.files["$DIRECTORY/preferences_backup_8"])
    }

    private fun editedShadows(content: String, value: String): List<ParamValue> =
        ConfigParser.toParamValues(parse(content)).map {
            if (it.key == "Shadows") it.copy(value = value, isModified = true) else it
        }

    private fun parse(content: String) = ConfigParser.parse(content.byteInputStream())

    private class FakeGateway(var content: String) : ConfigFileGateway {
        val backups = linkedMapOf<String, String>()
        var backupFails = false
        var replaceCalls = 0

        override fun read() = content
        override fun replace(content: String, previous: String): Boolean {
            replaceCalls++
            this.content = content
            return true
        }
        override fun listBackups() = backups.keys.toList()
        override fun readBackup(name: String) = backups[name]
        override fun createBackup(name: String, content: String): Boolean {
            if (backupFails) return false
            backups[name] = content
            return true
        }
        override fun deleteBackup(name: String) = backups.remove(name) != null
        override fun isGameRunning(): Boolean? = false
        override fun stopGame() = true
    }

    /** Interprets exactly the commands ShellConfigGateway issues against an in-memory file map. */
    private class FakeShell(val files: MutableMap<String, String>) {
        var decodeFails = false
        var chunkWrites = 0

        fun exec(command: String): ShellResult {
            Regex("""^rm -f (.+)$""").matchEntire(command)?.let { match ->
                quoted(match.groupValues[1]).forEach(files::remove)
                return ok()
            }
            Regex("""^printf '%s' '([^']*)' >> "([^"]+)"$""").matchEntire(command)?.let { match ->
                chunkWrites++
                files[match.groupValues[2]] = files[match.groupValues[2]].orEmpty() + match.groupValues[1]
                return ok()
            }
            Regex("""^base64 -d < "([^"]+)" > "([^"]+)" && cat "([^"]+)" > "([^"]+)"$""").matchEntire(command)?.let { match ->
                if (decodeFails) return ShellResult("", 1)
                val decoded = String(java.util.Base64.getDecoder().decode(files.getValue(match.groupValues[1])))
                files[match.groupValues[2]] = decoded
                files[match.groupValues[4]] = decoded
                return ok()
            }
            Regex("""^cat "([^"]+)"$""").matchEntire(command)?.let { match ->
                return files[match.groupValues[1]]?.let { ShellResult(it, 0) } ?: ShellResult("", 1)
            }
            Regex("""^ls -1 "([^"]+)"$""").matchEntire(command)?.let { match ->
                val prefix = match.groupValues[1] + "/"
                return ShellResult(files.keys.filter { it.startsWith(prefix) }.joinToString("\n") { it.removePrefix(prefix) }, 0)
            }
            error("Unexpected command: $command")
        }

        private fun quoted(args: String) = Regex(""""([^"]+)"""").findAll(args).map { it.groupValues[1] }.toList()

        private fun ok() = ShellResult("", 0)
    }

    private companion object {
        const val DIRECTORY = "/sdcard/Android/data/com.feralinteractive.gridlegends_android/files/feral_app_support"
        const val CONFIG_PATH = "$DIRECTORY/preferences"
    }
}
