package com.gridlegends.graphicsassistant.data

/**
 * Applies a bundled real `preferences` preset on top of another configuration.
 *
 * A preset contributes every editable parameter plus its Graphics and Performance profile
 * values, so the game does not restore the old profile on the next launch. BatterySaver is
 * identical in all bundled presets and MetalFX is not applicable on Android, so both are kept.
 */
object PresetApplication {

    data class Applied(
        val paramValues: Map<String, ParamValue>,
        val appliedCount: Int,
        val skippedKeys: List<String>
    )

    private val profilePrefixes = listOf("Graphics/", "Performance/")
    private const val UNSUPPORTED_VALUE_NAME = "MetalFX"

    fun valuesFrom(presetRawValues: Map<String, String>): Map<String, String> {
        val editable = ParamDefinitions.allParams
            .mapNotNull { param -> presetRawValues[param.key]?.let { param.key to it } }
            .toMap()
        val profiles = presetRawValues.filterKeys { key ->
            profilePrefixes.any(key::startsWith) &&
                key.substringAfter('/') != UNSUPPORTED_VALUE_NAME
        }
        return editable + profiles
    }

    /**
     * Only keys present in [targetRawValues] are applied; writing a field the target file does
     * not have would fail verification and block the whole save.
     */
    fun apply(
        currentValues: Map<String, ParamValue>,
        targetRawValues: Map<String, String>,
        presetRawValues: Map<String, String>
    ): Applied {
        val next = currentValues.toMutableMap()
        val skipped = mutableListOf<String>()
        var applied = 0

        valuesFrom(presetRawValues).forEach { (key, value) ->
            val originalValue = currentValues[key]?.originalValue ?: targetRawValues[key]
            if (originalValue == null) {
                skipped += key
                return@forEach
            }
            next[key] = ParamValue(
                key = key,
                value = value,
                originalValue = originalValue,
                isModified = value != originalValue
            )
            applied++
        }
        return Applied(next, applied, skipped.sorted())
    }
}
