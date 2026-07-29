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
    fun rootOwnedEditorRouteSurvivesJavaSerialization() {
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

        val restored = javaSerializationRoundTrip(
            EditorRouteState.open(source).withSession(session)
        )
        val restoredSource = restored.source as EditorSource.BundledPreset
        val restoredSession = restored.session

        assertEquals("balanced", restoredSource.preset.metadata.id)
        assertArrayEquals(byteArrayOf(1, 2, 3), restoredSource.preset.baseBytes)
        assertEquals("preset:balanced", restoredSession.sourceKey)
        assertTrue(restoredSession.isInitialized)
        assertEquals(mapOf("quality" to "high"), restoredSession.rawConfigValues)
        val restoredParam = restoredSession.paramValues.getValue("quality")
        assertEquals("ultra", restoredParam.value)
        assertEquals("high", restoredParam.originalValue)
        assertTrue(restoredParam.isModified)
        assertArrayEquals(byteArrayOf(9, 8, 7), restoredSession.pendingExportBytes)
        assertEquals(mapOf("quality" to "ultra"), restoredSession.pendingExportSnapshot)
        assertEquals(mapOf("quality" to "high"), restoredSession.lastExportedSnapshot)
    }

    @Test
    fun openingDifferentSourceStartsFreshSession() {
        val firstSource = EditorSource.LiveConfig(
            accessMode = AccessMode.ROOT,
            configPath = "/first/preferences",
            configContent = null
        )
        val firstRoute = EditorRouteState.open(firstSource).withSession(
            EditorSessionState(
                sourceKey = firstSource.sessionKey(),
                rawConfigValues = mapOf("quality" to "high"),
                paramValues = mapOf("quality" to ParamValue("quality", "ultra", "high", true)),
                isInitialized = true
            ).beginExport(byteArrayOf(1), mapOf("quality" to "ultra"))
        )
        val secondSource = EditorSource.LiveConfig(
            accessMode = AccessMode.SHIZUKU,
            configPath = "/second/preferences",
            configContent = null
        )

        val secondRoute = EditorRouteState.open(secondSource)

        assertEquals("live:ROOT:/first/preferences", firstRoute.session.sourceKey)
        assertEquals("live:SHIZUKU:/second/preferences", secondRoute.session.sourceKey)
        assertTrue(secondRoute.session.rawConfigValues.isEmpty())
        assertTrue(secondRoute.session.paramValues.isEmpty())
        assertNull(secondRoute.session.pendingExportBytes)
        assertFalse(secondRoute.session.isInitialized)
    }

    @Test(expected = IllegalArgumentException::class)
    fun routeRejectsSessionFromDifferentSource() {
        val source = EditorSource.LiveConfig(
            accessMode = AccessMode.ROOT,
            configPath = "/first/preferences",
            configContent = null
        )
        val route = EditorRouteState.open(source)

        route.withSession(EditorSessionState(sourceKey = "preset:other"))
    }

    @Test
    fun resultCallbackReadsPendingFromRootStoreBeforeUiSnapshotRefresh() {
        val source = EditorSource.LiveConfig(
            accessMode = AccessMode.ROOT,
            configPath = "/game/preferences",
            configContent = null
        )
        val initialRoute = EditorRouteState.open(source)
        var publishedRoute = initialRoute
        val store = EditorSessionStore(initialRoute) { publishedRoute = it }
        val staleUiSession = publishedRoute.session
        val pendingBytes = byteArrayOf(7, 8, 9)
        val pendingSnapshot = mapOf("quality" to "ultra")

        store.updateSession(source.sessionKey()) { current ->
            current.beginExport(pendingBytes, pendingSnapshot)
        }
        val callbackSession = store.currentSession(source.sessionKey())

        assertNull(staleUiSession.pendingExportBytes)
        assertArrayEquals(pendingBytes, callbackSession?.pendingExportBytes)
        assertEquals(pendingSnapshot, callbackSession?.pendingExportSnapshot)
        assertArrayEquals(pendingBytes, publishedRoute.session.pendingExportBytes)

        val completedSession = store.updateSession(source.sessionKey()) { current ->
            current.completeExport()
        }

        assertNull(completedSession?.pendingExportBytes)
        assertEquals(pendingSnapshot, completedSession?.lastExportedSnapshot)
    }

    @Test
    fun rootStoreRejectsUpdateAfterSourceSwitch() {
        val firstSource = EditorSource.LiveConfig(
            accessMode = AccessMode.ROOT,
            configPath = "/first/preferences",
            configContent = null
        )
        val secondSource = EditorSource.LiveConfig(
            accessMode = AccessMode.SHIZUKU,
            configPath = "/second/preferences",
            configContent = null
        )
        val store = EditorSessionStore(EditorRouteState.open(firstSource))
        store.open(secondSource)

        val rejected = store.updateSession(firstSource.sessionKey()) { current ->
            current.beginExport(byteArrayOf(1), mapOf("quality" to "ultra"))
        }

        assertNull(rejected)
        assertNull(store.currentSession(secondSource.sessionKey())?.pendingExportBytes)
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
        val contentSecret = "do-not-log-4fb4d6ef"
        val pathSecret = "path-secret-81ad5c2e"
        val path = "/sdcard/Android/data/$pathSecret/preferences"
        val content = "<Settings><token>$contentSecret</token></Settings>"
        val source = EditorSource.LiveConfig(
            accessMode = AccessMode.SAF,
            configPath = path,
            configContent = content
        )

        val summary = source.logSummary()

        assertTrue(summary.contains("SAF"))
        assertTrue(summary.contains("pathLength=${path.length}"))
        assertTrue(summary.contains(content.length.toString()))
        assertFalse(summary.contains(pathSecret))
        assertFalse(summary.contains(contentSecret))
        assertFalse(summary.contains("<Settings>"))
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> javaSerializationRoundTrip(value: T): T {
        val bytes = ByteArrayOutputStream().use { output ->
            ObjectOutputStream(output).use { it.writeObject(value) }
            output.toByteArray()
        }
        return ObjectInputStream(ByteArrayInputStream(bytes)).use { input ->
            input.readObject() as T
        }
    }
}
