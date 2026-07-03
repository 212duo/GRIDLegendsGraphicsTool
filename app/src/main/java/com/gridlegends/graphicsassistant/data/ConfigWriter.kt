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

        if (key.contains("/")) return content

        return replaceLegacyFormats(content, location.valueName, value)
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

    private fun replaceLegacyFormats(content: String, valueName: String, newValue: String): String {
        var updated = content
        val escapedValue = Regex.escapeReplacement(newValue)

        val valuePattern = Regex(
            """(<value\s+name="${Regex.escape(valueName)}"\s+type="[^"]*">)[^<]*(</value>)"""
        )
        updated = valuePattern.replaceFirst(updated, "$1$escapedValue$2")

        val settingPattern = Regex(
            """(<Setting\s+name="${Regex.escape(valueName)}"\s+value=")[^"]*(")"""
        )
        updated = settingPattern.replaceFirst(updated, "$1$escapedValue$2")

        val elementPattern = Regex(
            """(<${Regex.escape(valueName)}>)\s*[^<]*(</${Regex.escape(valueName)}>)"""
        )
        updated = elementPattern.replaceFirst(updated, "$1$escapedValue$2")

        return updated
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
