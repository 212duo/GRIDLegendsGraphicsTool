package com.gridlegends.graphicsassistant.data

enum class PresetGroup(val title: String) {
    LOW("低配"),
    MID("中配"),
    HIGH("高配"),
    EXTREME("极限")
}

data class PresetMetadata(
    val id: String,
    val name: String,
    val group: PresetGroup,
    val description: String,
    val assetPath: String = "presets/$id/preferences"
)

data class PresetSummary(
    val frameRate: String,
    val renderHeight: String,
    val hdrEnabled: Boolean
)

data class LoadedPreset(
    val metadata: PresetMetadata,
    val baseBytes: ByteArray,
    val baseContent: String,
    val rawValues: Map<String, String>,
    val summary: PresetSummary
)
