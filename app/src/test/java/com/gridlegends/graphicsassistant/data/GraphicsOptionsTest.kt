package com.gridlegends.graphicsassistant.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphicsOptionsTest {

    @Test
    fun allRecommendedGraphicsOptionsAreEditableAndUnsupportedKeysAreExcluded() {
        val keys = ParamDefinitions.allParams.map { it.key }.toSet()

        assertTrue("Missing recommended keys: ${recommendedKeys - keys}", keys.containsAll(recommendedKeys))
        assertFalse(keys.contains("MetalFX"))
        assertFalse(keys.contains("Graphics/MetalFX"))
        assertFalse(keys.contains("CubeMapUpdateMode"))
        assertFalse(keys.any { it.endsWith("_maximum") })
    }

    @Test
    fun recommendedOptionsUseTheDesignedControlsRangesAndChoices() {
        val params = ParamDefinitions.allParams.associateBy { it.key }
        val levelOptions = mapOf("1" to "低", "2" to "中", "3" to "高")
        val optionalLevelOptions = mapOf("0" to "关", "1" to "低", "2" to "中", "3" to "高")
        val switchOptions = mapOf("0" to "关", "1" to "开")

        listOf("AmbientOcclusion", "LightingQuality", "Skidmarks", "WeatherEffects").forEach { key ->
            assertEquals(ParamControlType.LEVEL_SELECTOR, params.getValue(key).controlType)
            assertEquals(levelOptions, params.getValue(key).options)
        }
        listOf("Mirrors", "Particles").forEach { key ->
            assertEquals(ParamControlType.LEVEL_SELECTOR, params.getValue(key).controlType)
            assertEquals(optionalLevelOptions, params.getValue(key).options)
        }

        val resolutionMultiplier = params.getValue("Graphics/OptimisationResolutionMultiplier")
        assertEquals(ParamControlType.SLIDER, resolutionMultiplier.controlType)
        assertEquals(40f, resolutionMultiplier.minValue)
        assertEquals(100f, resolutionMultiplier.maxValue)
        assertEquals(5f, resolutionMultiplier.step)
        assertEquals("%", resolutionMultiplier.unitSuffix)

        listOf("VerticalSync", "ReduceVisibleRain").forEach { key ->
            assertEquals(ParamControlType.SWITCH, params.getValue(key).controlType)
            assertEquals(switchOptions, params.getValue(key).options)
        }
    }

    @Test
    fun parserAndWriterRoundTripRecommendedOptionsWithoutChangingUnmanagedValues() {
        val existingContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <registry>
              <key name="HKEY_CURRENT_USER">
                <key name="Software">
                  <key name="Feral Interactive">
                    <key name="GRIDLegends">
                      <key name="GraphicsSettings">
                        <value name="AmbientOcclusion" type="integer">1</value>
                        <value name="LightingQuality" type="integer">2</value>
                        <value name="Mirrors" type="integer">3</value>
                        <value name="Particles" type="integer">0</value>
                        <value name="Skidmarks" type="integer">2</value>
                        <value name="WeatherEffects" type="integer">3</value>
                        <value name="VerticalSync" type="integer">0</value>
                        <value name="ReduceVisibleRain" type="integer">1</value>
                        <value name="BatterySaver" type="integer">1</value>
                        <key name="Graphics">
                          <value name="OptimisationResolutionMultiplier" type="integer">75</value>
                          <value name="MetalFX" type="integer">1</value>
                        </key>
                      </key>
                    </key>
                  </key>
                </key>
              </key>
            </registry>
        """.trimIndent()
        val parsed = ConfigParser.parse(ByteArrayInputStream(existingContent.toByteArray()))

        assertEquals(recommendedKeys, ConfigParser.toParamValues(parsed).map { it.key }.toSet())

        val output = ByteArrayOutputStream()
        ConfigWriter.write(
            output = output,
            existingContent = existingContent,
            paramValues = listOf(
                ParamValue("AmbientOcclusion", "3"),
                ParamValue("Graphics/OptimisationResolutionMultiplier", "90")
            )
        )

        val rewritten = output.toString(Charsets.UTF_8.name())
        val reparsed = ConfigParser.parse(ByteArrayInputStream(rewritten.toByteArray()))
        assertEquals("3", reparsed["AmbientOcclusion"])
        assertEquals("90", reparsed["Graphics/OptimisationResolutionMultiplier"])
        assertEquals("1", reparsed["BatterySaver"])
        assertEquals("1", reparsed["Graphics/MetalFX"])
    }

    private companion object {
        val recommendedKeys = setOf(
            "AmbientOcclusion",
            "LightingQuality",
            "Mirrors",
            "Particles",
            "Skidmarks",
            "WeatherEffects",
            "Graphics/OptimisationResolutionMultiplier",
            "VerticalSync",
            "ReduceVisibleRain"
        )
    }
}
