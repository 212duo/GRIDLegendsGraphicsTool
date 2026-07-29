package com.gridlegends.graphicsassistant.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PresetRepository(private val context: Context) {

    suspend fun load(metadata: PresetMetadata): Result<LoadedPreset> =
        withContext(Dispatchers.IO) {
            runCatching {
                val bytes = context.assets.open(metadata.assetPath).use { it.readBytes() }
                val content = bytes.toString(Charsets.UTF_8)
                val rawValues = ConfigParser.parse(bytes.inputStream())
                val missingKeys = ParamDefinitions.allParams
                    .map { it.key }
                    .filterNot(rawValues::containsKey)

                check(missingKeys.isEmpty()) {
                    "缺少可编辑字段：${missingKeys.joinToString()}"
                }

                LoadedPreset(
                    metadata = metadata,
                    baseBytes = bytes,
                    baseContent = content,
                    rawValues = rawValues,
                    summary = PresetSummary(
                        frameRate = requireNotNull(rawValues["FramerateLimit"]) {
                            "缺少 FramerateLimit"
                        },
                        renderHeight = requireNotNull(rawValues["Setup/FixedScreenHeight"]) {
                            "缺少 Setup/FixedScreenHeight"
                        },
                        hdrEnabled = rawValues["HDR"] == "1"
                    )
                )
            }
        }
}
