package com.gridlegends.graphicsassistant.data

import com.gridlegends.graphicsassistant.fileaccess.AccessMode
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class EditorSessionStateTest {
    @Test
    fun pendingExportAndBundledSourceSurviveJavaSerialization() {
        val preset = LoadedPreset(
            metadata = PresetMetadata("balanced", "平衡", PresetGroup.MID, "适合大多数设备"),
            baseBytes = byteArrayOf(1, 2, 3),
            baseContent = "<Settings><Setting name=\"quality\" value=\"high\" /></Settings>",
            rawValues = mapOf("quality" to "high"),
            summary = PresetSummary("60", "1080", true)
        )
        val source = EditorSource.BundledPreset(preset)
        val session = EditorSessionState(
            sourceKey = "preset:balanced",
            rawConfigValues = preset.rawValues,
            paramValues = mapOf("quality" to ParamValue("quality", "ultra", "high", true)),
            isInitialized = true,
            lastExportedSnapshot = mapOf("quality" to "high")
        ).beginExport(byteArrayOf(9, 8, 7), mapOf("quality" to "ultra"))

        val restored = javaSerializationRoundTrip(source to session)
        val restoredSource = restored.first as EditorSource.BundledPreset
        val restoredSession = restored.second

        assertEquals("balanced", restoredSource.preset.metadata.id)
        assertArrayEquals(byteArrayOf(1, 2, 3), restoredSource.preset.baseBytes)
        assertEquals("ultra", restoredSession.paramValues.getValue("quality").value)
        assertArrayEquals(byteArrayOf(9, 8, 7), restoredSession.pendingExportBytes)
        assertEquals(mapOf("quality" to "ultra"), restoredSession.pendingExportSnapshot)
        assertEquals(mapOf("quality" to "high"), restoredSession.lastExportedSnapshot)
    }

    @Test
    fun successfulExportClearsPendingAndRecordsSnapshot() {
        val exportSnapshot = mapOf("shadow_quality" to "high")
        val completed = EditorSessionState(sourceKey = "live:/settings.xml")
            .beginExport(byteArrayOf(1, 2), exportSnapshot)
            .completeExport()

        assertNull(completed.pendingExportBytes)
        assertNull(completed.pendingExportSnapshot)
        assertEquals(exportSnapshot, completed.lastExportedSnapshot)
    }

    @Test
    fun sessionDefensivelyCopiesMutableExportAndConfigInputs() {
        val rawValues = mutableMapOf("quality" to "high")
        val pendingBytes = byteArrayOf(1, 2)
        val pendingSnapshot = mutableMapOf("quality" to "high")
        val session = EditorSessionState(
            sourceKey = "live:/settings.xml",
            rawConfigValues = rawValues,
            pendingExportBytes = pendingBytes,
            pendingExportSnapshot = pendingSnapshot
        )

        rawValues["quality"] = "ultra"
        pendingBytes[0] = 9
        pendingSnapshot["quality"] = "ultra"
        session.pendingExportBytes!![1] = 8

        assertEquals("high", session.rawConfigValues.getValue("quality"))
        assertArrayEquals(byteArrayOf(1, 2), session.pendingExportBytes)
        assertEquals(mapOf("quality" to "high"), session.pendingExportSnapshot)
    }

    @Test
    fun logSummaryNeverContainsEmbeddedConfigBody() {
        val secret = "do-not-log-4fb4d6ef"
        val content = "<Settings><token>$secret</token></Settings>"
        val source = EditorSource.LiveConfig(
            accessMode = AccessMode.SAF,
            configPath = "/sdcard/Android/data/game/files/settings.xml",
            configContent = content
        )

        val summary = source.logSummary()

        assertTrue(summary.contains("SAF"))
        assertTrue(summary.contains("/sdcard/Android/data/game/files/settings.xml"))
        assertTrue(summary.contains(content.length.toString()))
        assertFalse(summary.contains(secret))
        assertFalse(summary.contains("<Settings>"))
    }

    @Suppress("UNCHECKED_CAST")
    private fun javaSerializationRoundTrip(value: Pair<EditorSource, EditorSessionState>): Pair<EditorSource, EditorSessionState> {
        val bytes = ByteArrayOutputStream().use { output ->
            ObjectOutputStream(output).use { it.writeObject(value) }
            output.toByteArray()
        }
        return ObjectInputStream(ByteArrayInputStream(bytes)).use { input ->
            input.readObject() as Pair<EditorSource, EditorSessionState>
        }
    }
}
