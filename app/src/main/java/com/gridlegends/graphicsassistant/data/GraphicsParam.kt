package com.gridlegends.graphicsassistant.data

enum class ParamControlType {
    SLIDER,
    SWITCH,
    RADIO_GROUP,
    LEVEL_SELECTOR
}

data class GraphicsParam(
    val key: String,
    val displayName: String,
    val description: String,
    val controlType: ParamControlType,
    val section: ParamSection,
    val minValue: Float = 0f,
    val maxValue: Float = 100f,
    val step: Float = 1f,
    val unitSuffix: String = "",
    val options: Map<String, String> = emptyMap()
)

enum class ParamSection(val title: String, val icon: String) {
    DISPLAY("显示与帧率", "显示"),
    QUALITY("画面质量", "画质"),
    POST_PROCESSING("后处理效果", "后期"),
    REFLECTIONS("反射与光照", "反射"),
    DETAIL("细节与高级", "细节")
}

data class ParamValue(
    val key: String,
    val value: String,
    val originalValue: String = value,
    val isModified: Boolean = false
)
