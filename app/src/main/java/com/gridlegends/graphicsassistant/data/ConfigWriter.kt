package com.gridlegends.graphicsassistant.data

import java.io.OutputStream

object ConfigWriter {

    fun write(
        output: OutputStream,
        paramValues: List<ParamValue>,
        existingContent: String? = null
    ) {
        output.write(render(paramValues, existingContent).toByteArray(Charsets.UTF_8))
        output.flush()
    }

    /**
     * Renders the rewritten file and re-parses it, failing when any value that differs from
     * [existingContent] did not land. Nothing should be written to disk when this fails.
     */
    fun renderVerified(
        existingContent: String,
        paramValues: List<ParamValue>
    ): Result<String> = runCatching {
        val changed = changedValues(paramValues, parseOrNull(existingContent))
        val rendered = render(paramValues, existingContent)
        val reparsed = ConfigParser.parse(rendered.byteInputStream())
        val failedKeys = changed.filter { reparsed[it.key] != it.value }.map { it.key }
        check(failedKeys.isEmpty()) {
            "以下字段未成功写入：${failedKeys.joinToString()}"
        }
        rendered
    }

    private fun render(paramValues: List<ParamValue>, existingContent: String?): String {
        val explicitKeys = paramValues.mapTo(mutableSetOf()) { it.key }
        if (existingContent == null) {
            return generateFreshConfig(synchronizeGraphicsProfileValues(paramValues, explicitKeys))
        }

        // Only touch values that actually differ from the file, so an edit to one option never
        // rewrites unrelated fields or profile blocks.
        val existingValues = parseOrNull(existingContent)
        val changed = changedValues(paramValues, existingValues)
        val valuesToWrite = changedValues(
            synchronizeGraphicsProfileValues(changed, explicitKeys),
            existingValues
        )

        var content: String = existingContent
        for (param in valuesToWrite) {
            content = replaceParam(content, param.key, param.value)
        }
        return content
    }

    private fun parseOrNull(content: String): Map<String, String>? =
        runCatching { ConfigParser.parse(content.byteInputStream()) }.getOrNull()

    private fun changedValues(
        values: List<ParamValue>,
        existingValues: Map<String, String>?
    ): List<ParamValue> =
        if (existingValues == null) values else values.filter { existingValues[it.key] != it.value }

    /**
     * GRID Legends keeps a current value and a value for the Graphics preset.
     * When a user edits a current value directly, mirror it to the Graphics
     * block unless a preset explicitly supplied that profile value.
     */
    private fun synchronizeGraphicsProfileValues(
        paramValues: List<ParamValue>,
        explicitKeys: Set<String>
    ): List<ParamValue> {
        val result = paramValues.toMutableList()

        paramValues.filter { it.key in graphicsProfileKeys }.forEach { current ->
            val profileKey = "Graphics/${current.key}"
            if (profileKey in explicitKeys) return@forEach

            val profileValue = if (current.key in integerGraphicsProfileKeys) {
                current.value
            } else {
                "${current.value}, 0, 0"
            }
            result += ParamValue(
                key = profileKey,
                value = profileValue,
                originalValue = profileValue,
                isModified = current.isModified
            )
        }
        return result
    }

    private fun replaceParam(content: String, key: String, value: String): String {
        val location = ValueLocation.fromKey(key)
        replaceValueInBlock(
            content = content,
            blockName = location.blockName,
            valueName = location.valueName,
            newValue = value
        )?.let { return it }

        return replaceLegacyFormats(content, key, location, value)
    }

    /** Returns null when the block has no such value, even if the new value equals the old one. */
    private fun replaceValueInBlock(
        content: String,
        blockName: String,
        valueName: String,
        newValue: String
    ): String? {
        val pattern = Regex(
            pattern = """<key\s+name="${Regex.escape(blockName)}"\s*>(?:(?!<key\s+name=|</key>).)*?<value\s+name="${Regex.escape(valueName)}"\s+type="[^"]*">([^<]*)</value>""",
            options = setOf(RegexOption.DOT_MATCHES_ALL)
        )
        val valueRange = pattern.find(content)?.groups?.get(1)?.range ?: return null
        return content.replaceRange(valueRange, newValue)
    }

    private fun replaceLegacyFormats(
        content: String,
        key: String,
        location: ValueLocation,
        newValue: String
    ): String {
        val escapedValue = Regex.escapeReplacement(newValue)

        val settingPattern = Regex(
            pattern = """(<Setting\b(?=[^>]*\bname\s*=\s*["']${Regex.escape(key)}["'])[^>]*\bvalue\s*=\s*)(["'])[^"']*\2""",
            option = RegexOption.IGNORE_CASE
        )
        val settingUpdated = settingPattern.replaceFirst(content, "$1$2$escapedValue$2")
        if (settingUpdated != content) return settingUpdated

        if (key.contains("/")) {
            return replaceElementInBlock(
                content = content,
                blockName = location.blockName,
                valueName = location.valueName,
                newValue = newValue
            )
        }

        // In the game's registry format a value missing from GraphicsSettings must not be
        // "found" in BatterySaver, BenchmarkSetup or another block further down the file.
        if (registryGraphicsBlock.containsMatchIn(content)) return content

        val valuePattern = Regex(
            """(<value\s+name="${Regex.escape(location.valueName)}"\s+type="[^"]*">)[^<]*(</value>)"""
        )
        val valueUpdated = valuePattern.replaceFirst(content, "$1$escapedValue$2")
        if (valueUpdated != content) return valueUpdated

        val elementPattern = Regex(
            """(<${Regex.escape(location.valueName)}>)\s*[^<]*(</${Regex.escape(location.valueName)}>)"""
        )
        return elementPattern.replaceFirst(content, "$1$escapedValue$2")
    }

    private fun replaceElementInBlock(
        content: String,
        blockName: String,
        valueName: String,
        newValue: String
    ): String {
        val blockStartPattern = Regex(
            pattern = """<\s*${Regex.escape(blockName)}\b[^>]*>""",
            option = RegexOption.IGNORE_CASE
        )
        val tagPattern = Regex(
            pattern = """<\s*(/?)\s*([A-Za-z_][A-Za-z0-9_.:-]*)\b[^>]*>"""
        )
        val blockStart = blockStartPattern.find(content) ?: return content
        var depth = 0
        var tag = tagPattern.find(content, blockStart.range.last + 1)

        while (tag != null) {
            val isClosing = tag.groupValues[1].isNotEmpty()
            val tagName = tag.groupValues[2]
            val isSelfClosing = !isClosing && tag.value.trimEnd().endsWith("/>")

            if (isClosing) {
                if (depth == 0 && tagName.equals(blockName, ignoreCase = true)) {
                    return content
                }
                if (depth > 0) depth--
            } else {
                if (depth == 0 && tagName.equals(valueName, ignoreCase = true)) {
                    val closingTag = tagPattern.find(content, tag.range.last + 1) ?: return content
                    val closesValue = closingTag.groupValues[1].isNotEmpty() &&
                        closingTag.groupValues[2].equals(valueName, ignoreCase = true)
                    if (!closesValue) return content

                    return content.replaceRange(
                        startIndex = tag.range.last + 1,
                        endIndex = closingTag.range.first,
                        replacement = newValue
                    )
                }
                if (!isSelfClosing) depth++
            }

            tag = tagPattern.find(content, tag.range.last + 1)
        }

        return content
    }

    private fun generateFreshConfig(paramValues: List<ParamValue>): String {
        val paramMap = paramValues.associateBy { it.key }
        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<registry>""")
            appendLine("""    <key name="HKEY_CURRENT_USER">""")
            appendLine("""        <key name="Software">""")
            appendLine("""            <key name="Feral Interactive">""")
            appendLine("""                <key name="GRIDLegends">""")
            appendLine("""                    <key name="GraphicsSettings">""")
            for (param in ParamDefinitions.allParams.filter { !it.key.contains("/") }) {
                val value = paramMap[param.key]?.value ?: continue
                appendLine("""                        <value name="${param.key}" type="${inferType(value)}">$value</value>""")
            }
            for (blockName in listOf("Graphics", "Performance")) {
                val blockParams = ParamDefinitions.allParams.filter { it.key.startsWith("$blockName/") }
                if (blockParams.none { paramMap.containsKey(it.key) }) continue
                appendLine("""                        <key name="$blockName">""")
                for (param in blockParams) {
                    val value = paramMap[param.key]?.value ?: continue
                    appendLine("""                            <value name="${param.key.substringAfter("/")}" type="${inferType(value)}">$value</value>""")
                }
                appendLine("""                        </key>""")
            }
            appendLine("""                    </key>""")
            appendLine("""                    <key name="Setup">""")
            for (param in ParamDefinitions.allParams.filter { it.key.startsWith("Setup/") }) {
                val value = paramMap[param.key]?.value ?: continue
                appendLine("""                        <value name="${param.key.substringAfter("/")}" type="${inferType(value)}">$value</value>""")
            }
            appendLine("""                    </key>""")
            appendLine("""                </key>""")
            appendLine("""            </key>""")
            appendLine("""        </key>""")
            appendLine("""    </key>""")
            appendLine("""</registry>""")
        }
    }

    private fun inferType(value: String): String {
        return if (value.contains(",")) "string" else "integer"
    }

    private val graphicsProfileKeys = setOf(
        "FramerateLimit",
        "AmbientOcclusion",
        "AntiAliasing",
        "Crowds",
        "DynamicResolution",
        "FireworksAndConfetti",
        "GroundCover",
        "LightingQuality",
        "LODQuality",
        "Mirrors",
        "Particles",
        "PostProcess",
        "ScreenSpaceReflections",
        "Shadows",
        "Skidmarks",
        "SmokeShadows",
        "TextureQuality",
        "VehicleReflections",
        "VolumetricLights",
        "WeatherEffects"
    )

    private val integerGraphicsProfileKeys = setOf("FramerateLimit", "FireworksAndConfetti")

    private val registryGraphicsBlock = Regex("""<key\s+name="GraphicsSettings"\s*>""")

    fun backupFileName(): String {
        return "preferences_backup_${System.currentTimeMillis()}"
    }

    private data class ValueLocation(
        val blockName: String,
        val valueName: String
    ) {
        companion object {
            fun fromKey(key: String): ValueLocation {
                val block = key.substringBefore("/", missingDelimiterValue = "GraphicsSettings")
                val value = key.substringAfter("/", missingDelimiterValue = key)
                return when (block) {
                    "Setup" -> ValueLocation("Setup", value)
                    "Graphics" -> ValueLocation("Graphics", value)
                    "Performance" -> ValueLocation("Performance", value)
                    else -> ValueLocation("GraphicsSettings", value)
                }
            }
        }
    }
}
