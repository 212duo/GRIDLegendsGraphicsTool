package com.gridlegends.graphicsassistant.data

import com.gridlegends.graphicsassistant.fileaccess.AccessMode

sealed interface EditorSource {
    data class LiveConfig(
        val accessMode: AccessMode,
        val configPath: String?,
        val configContent: String?
    ) : EditorSource

    data class BundledPreset(
        val preset: LoadedPreset
    ) : EditorSource
}
