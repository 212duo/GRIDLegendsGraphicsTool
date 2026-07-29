package com.gridlegends.graphicsassistant.data

import java.io.OutputStream

object ConfigWriter {

    fun write(
        output: OutputStream,
        paramValues: List<ParamValue>,
        existingContent: String? = null
    ) {
        if (existingContent != null) {
            var content: String = existingContent
            for (param in paramValues) {
                content = replaceParam(content, param.key, param.value)
            }
            output.write(content.toByteArray(Charsets.UTF_8))
        } else {
            output.write(generateFreshConfig(paramValues).toByteArray(Charsets.UTF_8))
        }
        output.flush()
    }

    private fun replaceParam(content: String, key: String, value: String): String {
        val location = ValueLocation.fromKey(key)
        val replaced = replaceValueInBlock(
            content = content,
            blockName = location.blockName,
            valueName = location.valueName,
            newValue = value
        )
        if (replaced != content) return replaced

        return replaceLegacyFormats(content, key, location, value)
    }

    private fun replaceValueInBlock(
        content: String,
        blockName: String,
        valueName: String,
        newValue: String
    ): String {
        val pattern = Regex(
            pattern = """(<key\s+name="${Regex.escape(blockName)}">\s*(?:(?!<key\s+name=).)*?<value\s+name="${Regex.escape(valueName)}"\s+type="[^"]*">)[^<]*(</value>)""",
            options = setOf(RegexOption.DOT_MATCHES_ALL)
        )
        return pattern.replaceFirst(
            content,
            "$1${Regex.escapeReplacement(newValue)}$2"
        )
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
