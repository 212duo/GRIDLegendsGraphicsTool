package com.gridlegends.graphicsassistant.data

import com.gridlegends.graphicsassistant.fileaccess.AccessMode
import java.io.Serializable

sealed interface EditorSource : Serializable {
    data class LiveConfig(
        val accessMode: AccessMode,
        val configPath: String?,
        val configContent: String?
    ) : EditorSource

    data class BundledPreset(
        val preset: LoadedPreset
    ) : EditorSource
}

fun EditorSource.sessionKey(): String = when (this) {
    is EditorSource.LiveConfig -> "live:$accessMode:${configPath.orEmpty()}"
    is EditorSource.BundledPreset -> "preset:${preset.metadata.id}"
}

fun EditorSource.logSummary(): String = when (this) {
    is EditorSource.LiveConfig -> "LiveConfig(mode=$accessMode, " +
        "pathLength=${configPath?.length ?: 0}, contentLength=${configContent?.length ?: 0})"
    is EditorSource.BundledPreset -> "BundledPreset(id=${preset.metadata.id}, bytesLength=${preset.baseBytes.size}, contentLength=${preset.baseContent.length})"
}
