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

fun EditorSource.logSummary(): String = when (this) {
    is EditorSource.LiveConfig -> "LiveConfig(mode=$accessMode, path=$configPath, contentLength=${configContent?.length ?: 0})"
    is EditorSource.BundledPreset -> "BundledPreset(id=${preset.metadata.id}, bytesLength=${preset.baseBytes.size}, contentLength=${preset.baseContent.length})"
}
