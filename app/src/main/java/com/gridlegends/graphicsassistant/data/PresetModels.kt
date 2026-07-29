package com.gridlegends.graphicsassistant.data

import java.io.Serializable

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
) : Serializable

data class PresetSummary(
    val frameRate: String,
    val renderHeight: String,
    val hdrEnabled: Boolean
) : Serializable

data class LoadedPreset(
    val metadata: PresetMetadata,
    val baseBytes: ByteArray,
    val baseContent: String,
    val rawValues: Map<String, String>,
    val summary: PresetSummary
) : Serializable
