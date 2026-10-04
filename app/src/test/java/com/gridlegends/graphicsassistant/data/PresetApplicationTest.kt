package com.gridlegends.graphicsassistant.data

import com.gridlegends.graphicsassistant.data.ConfigWriterRealFileTest.Companion.presetContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetApplicationTest {

    @Test
    fun everyBundledPresetExposesEveryEditableParameter() {
        val editableKeys = ParamDefinitions.allParams.map { it.key }
        PresetCatalog.all.forEach { preset ->
            val raw = parse(presetContent(preset.id))
            assertEquals("${preset.id} is missing editable keys", emptyList<String>(), editableKeys.filterNot(raw::containsKey))
        }
    }

    @Test
    fun noBundledPresetUsesAnExperimentalLevel() {
        PresetCatalog.all.forEach { preset ->
            val raw = parse(presetContent(preset.id))
            ParamDefinitions.allParams.filter { it.experimentalValues.isNotEmpty() }.forEach { param ->
                assertFalse(
                    "${preset.id} uses experimental ${param.key}=${raw[param.key]}",
                    raw[param.key] in param.experimentalValues
                )
            }
        }
    }

    @Test
    fun extremeLevelIsMarkedExperimentalWhereverItIsOffered() {
        ParamDefinitions.allParams.filter { "4" in it.options }.forEach { param ->
            assertEquals(param.key, setOf("4"), param.experimentalValues)
        }
    }

    @Test
    fun presetValuesIncludeProfilesButNeverMetalFx() {
        val values = PresetApplication.valuesFrom(parse(presetContent("extreme_final_120_hdr")))

        assertTrue(values.keys.containsAll(ParamDefinitions.allParams.map { it.key }))
        assertTrue(values.containsKey("Graphics/TextureQuality"))
        assertTrue(values.containsKey("Performance/FramerateLimit"))
        assertFalse(values.keys.any { it.endsWith("MetalFX") })
    }

    @Test
    fun applyingAnyPresetReproducesItsValuesInTheSavedFile() {
        val baseContent = presetContent("mid_dedicated")
        val baseRaw = parse(baseContent)
        val baseValues = ConfigParser.toParamValues(baseRaw).associateBy { it.key }

        PresetCatalog.all.forEach { preset ->
            val presetRaw = parse(presetContent(preset.id))
            val applied = PresetApplication.apply(baseValues, baseRaw, presetRaw)
            val expected = PresetApplication.valuesFrom(presetRaw).filterKeys { it !in applied.skippedKeys }

            val saved = ConfigWriter.renderVerified(baseContent, applied.paramValues.values.toList()).getOrThrow()
            val reparsed = parse(saved)

            expected.forEach { (key, value) ->
                assertEquals("${preset.id}: $key", value, reparsed[key])
            }
            assertEquals("${preset.id}: BatterySaver must stay untouched", batterySaver(baseContent), batterySaver(saved))
        }
    }

    @Test
    fun keysMissingFromTheTargetAreSkippedNotInvented() {
        val presetRaw = parse(presetContent("high_dedicated"))
        val target = mapOf("Shadows" to "1")
        val current = mapOf("Shadows" to ParamValue("Shadows", "1"))

        val applied = PresetApplication.apply(current, target, presetRaw)

        assertEquals(setOf("Shadows"), applied.paramValues.keys)
        assertEquals(1, applied.appliedCount)
        assertTrue(applied.skippedKeys.contains("Graphics/Shadows"))
    }

    private fun parse(content: String) = ConfigParser.parse(content.byteInputStream())

    private fun batterySaver(content: String): String =
        content.substringAfter("<key name=\"BatterySaver\">").substringBefore("</key>")
}
