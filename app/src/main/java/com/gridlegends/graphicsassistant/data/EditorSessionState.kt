package com.gridlegends.graphicsassistant.data

import java.io.Serializable
import java.util.Collections
import java.util.LinkedHashMap

class EditorSessionState(
    val sourceKey: String,
    rawConfigValues: Map<String, String> = emptyMap(),
    paramValues: Map<String, ParamValue> = emptyMap(),
    val isInitialized: Boolean = false,
    val loadError: String? = null,
    pendingExportBytes: ByteArray? = null,
    pendingExportSnapshot: Map<String, String>? = null,
    lastExportedSnapshot: Map<String, String> = emptyMap()
) : Serializable {
    private val rawConfigValuesSnapshot = immutableMap(rawConfigValues)
    private val paramValuesSnapshot = immutableMap(paramValues)
    private val pendingExportBytesSnapshot = pendingExportBytes?.copyOf()
    private val pendingExportSnapshotValue = pendingExportSnapshot?.let(::immutableMap)
    private val lastExportedSnapshotValue = immutableMap(lastExportedSnapshot)

    val rawConfigValues: Map<String, String>
        get() = rawConfigValuesSnapshot

    val paramValues: Map<String, ParamValue>
        get() = paramValuesSnapshot

    val pendingExportBytes: ByteArray?
        get() = pendingExportBytesSnapshot?.copyOf()

    val pendingExportSnapshot: Map<String, String>?
        get() = pendingExportSnapshotValue

    val lastExportedSnapshot: Map<String, String>
        get() = lastExportedSnapshotValue

    fun beginExport(bytes: ByteArray, snapshot: Map<String, String>): EditorSessionState = EditorSessionState(
        sourceKey = sourceKey,
        rawConfigValues = rawConfigValuesSnapshot,
        paramValues = paramValuesSnapshot,
        isInitialized = isInitialized,
        loadError = loadError,
        pendingExportBytes = bytes,
        pendingExportSnapshot = snapshot,
        lastExportedSnapshot = lastExportedSnapshotValue
    )

    fun cancelExport(): EditorSessionState = EditorSessionState(
        sourceKey = sourceKey,
        rawConfigValues = rawConfigValuesSnapshot,
        paramValues = paramValuesSnapshot,
        isInitialized = isInitialized,
        loadError = loadError,
        lastExportedSnapshot = lastExportedSnapshotValue
    )

    fun completeExport(): EditorSessionState = EditorSessionState(
        sourceKey = sourceKey,
        rawConfigValues = rawConfigValuesSnapshot,
        paramValues = paramValuesSnapshot,
        isInitialized = isInitialized,
        loadError = loadError,
        lastExportedSnapshot = pendingExportSnapshotValue ?: lastExportedSnapshotValue
    )

    private companion object {
        fun <K, V> immutableMap(values: Map<K, V>): Map<K, V> =
            Collections.unmodifiableMap(LinkedHashMap(values))
    }
}
