package com.gridlegends.graphicsassistant.data

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

object ConfigParser {

    fun parse(input: InputStream): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val keyStack = mutableListOf<String>()

        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(input, "UTF-8")

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when {
                        parser.name.equals("key", ignoreCase = true) -> {
                            parser.getAttributeValue(null, "name")?.let { keyStack.add(it) }
                        }
                        parser.name.equals("value", ignoreCase = true) -> {
                            val key = parser.getAttributeValue(null, "name")
                                ?: parser.getAttributeValue(null, "Name")
                            if (key != null) {
                                val text = parser.nextText().trim()
                                if (text.isNotBlank()) {
                                    shortKey(keyStack, key)?.let { result[it] = text }
                                    result.putIfAbsent(key, text)
                                }
                            }
                        }
                        parser.name.equals("Setting", ignoreCase = true) -> {
                            val key = parser.getAttributeValue(null, "name")
                                ?: parser.getAttributeValue(null, "Name")
                            val value = parser.getAttributeValue(null, "value")
                                ?: parser.getAttributeValue(null, "Value")
                            if (key != null && value != null) {
                                result[key] = value
                            }
                        }
                        ParamDefinitions.findByKey(parser.name) != null -> {
                            val text = parser.nextText().trim()
                            if (text.isNotBlank()) {
                                result[parser.name] = text
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.equals("key", ignoreCase = true) && keyStack.isNotEmpty()) {
                        keyStack.removeAt(keyStack.lastIndex)
                    }
                }
            }
            eventType = parser.next()
        }

        return result
    }

    fun toParamValues(rawValues: Map<String, String>): List<ParamValue> {
        return ParamDefinitions.allParams.mapNotNull { param ->
            val value = rawValues[param.key] ?: return@mapNotNull null
            ParamValue(
                key = param.key,
                value = value,
                originalValue = value,
                isModified = false
            )
        }
    }

    fun isValidConfig(input: InputStream): Boolean {
        val values = parse(input)
        return values.keys.any { ParamDefinitions.findByKey(it) != null }
    }

    private fun shortKey(path: List<String>, valueName: String): String? {
        return when {
            path.endsWith("Feral Interactive", "GRIDLegends", "GraphicsSettings") -> valueName
            path.endsWith("Feral Interactive", "GRIDLegends", "GraphicsSettings", "Graphics") -> "Graphics/$valueName"
            path.endsWith("Feral Interactive", "GRIDLegends", "GraphicsSettings", "Performance") -> "Performance/$valueName"
            path.endsWith("Feral Interactive", "GRIDLegends", "Setup") -> "Setup/$valueName"
            else -> null
        }
    }

    private fun List<String>.endsWith(vararg names: String): Boolean {
        if (size < names.size) return false
        val start = size - names.size
        return names.indices.all { this[start + it] == names[it] }
    }
}
