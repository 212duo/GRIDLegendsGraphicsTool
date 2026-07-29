package com.gridlegends.graphicsassistant.data

import android.content.Context
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun decodeUtf8Strict(bytes: ByteArray): String =
    Charsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(bytes))
        .toString()

class PresetRepository(private val context: Context) {

    suspend fun load(metadata: PresetMetadata): Result<LoadedPreset> =
        withContext(Dispatchers.IO) {
            try {
                val bytes = context.assets.open(metadata.assetPath).use { it.readBytes() }
                val content = decodeUtf8Strict(bytes)
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
                .let(Result.Companion::success)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Result.failure(error)
            }
        }
}
