package com.gridlegends.graphicsassistant.data

object ConfigExporter {

    fun build(
        preset: LoadedPreset,
        paramValues: Map<String, ParamValue>
    ): Result<ByteArray> {
        if (paramValues.values.none { it.isModified }) {
            return Result.success(preset.baseBytes.copyOf())
        }

        return ConfigWriter.renderVerified(
            existingContent = preset.baseContent,
            paramValues = paramValues.values.toList()
        ).map { it.toByteArray(Charsets.UTF_8) }
    }
}
