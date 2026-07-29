package com.gridlegends.graphicsassistant.data

import java.io.ByteArrayOutputStream

object ConfigExporter {

    fun build(
        preset: LoadedPreset,
        paramValues: Map<String, ParamValue>
    ): Result<ByteArray> = runCatching {
        val modifiedValues = paramValues.values.filter { it.isModified }
        if (modifiedValues.isEmpty()) {
            return@runCatching preset.baseBytes.copyOf()
        }

        val output = ByteArrayOutputStream()
        ConfigWriter.write(
            output = output,
            paramValues = paramValues.values.toList(),
            existingContent = preset.baseContent
        )
        val bytes = output.toByteArray()
        val reparsedValues = ConfigParser.parse(bytes.inputStream())
        val failedKeys = modifiedValues.filter { modified ->
            reparsedValues[modified.key] != modified.value
        }.map { it.key }

        check(failedKeys.isEmpty()) {
            "以下字段未成功写入：${failedKeys.joinToString()}"
        }
        bytes
    }
}
