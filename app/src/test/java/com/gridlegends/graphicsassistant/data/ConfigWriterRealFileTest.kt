package com.gridlegends.graphicsassistant.data

import java.io.ByteArrayOutputStream
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigWriterRealFileTest {

    @Test
    fun editingOneValueOnlyChangesThatValueAndItsGraphicsProfile() {
        val content = presetContent("mid_dedicated")
        val raw = parse(content)
        val values = editableValues(raw).apply {
            put("Shadows", getValue("Shadows").copy(value = "3", isModified = true))
        }

        val rewritten = write(content, values.values)
        val changed = changedKeys(raw, parse(rewritten))

        assertEquals(setOf("Shadows", "Graphics/Shadows"), changed)
        assertEquals("3, 0, 0", parse(rewritten)["Graphics/Shadows"])
    }

    @Test
    fun writingUnchangedValuesKeepsTheFileByteForByte() {
        PresetCatalog.all.forEach { preset ->
            val content = presetContent(preset.id)
            val rewritten = write(content, editableValues(parse(content)).values)
            assertEquals("${preset.id} changed without edits", content, rewritten)
        }
    }

    @Test
    fun explicitProfileValueWinsOverSynchronization() {
        val content = presetContent("mid_dedicated")
        val raw = parse(content)
        val values = editableValues(raw).apply {
            put("Shadows", getValue("Shadows").copy(value = "3", isModified = true))
            put("Graphics/Shadows", ParamValue("Graphics/Shadows", "2, 0, 0", raw.getValue("Graphics/Shadows"), true))
        }

        val reparsed = parse(write(content, values.values))

        assertEquals("3", reparsed["Shadows"])
        assertEquals("2, 0, 0", reparsed["Graphics/Shadows"])
    }

    @Test
    fun missingCurrentValueIsReportedInsteadOfWrittenIntoAnotherBlock() {
        val content = registryFile(
            current = """<value name="Shadows" type="integer">1</value>""",
            batterySaver = """<value name="Particles" type="string">0, 0, 0</value>"""
        )

        val result = ConfigWriter.renderVerified(
            existingContent = content,
            paramValues = listOf(ParamValue("Particles", "3", "0", true))
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("Particles"))
    }

    @Test
    fun valuesFromOtherRegistryBlocksDoNotLeakIntoCurrentKeys() {
        val content = registryFile(
            current = """<value name="Shadows" type="integer">1</value>""",
            batterySaver = """<value name="Particles" type="string">0, 0, 0</value>"""
        )

        val raw = parse(content)

        assertEquals("1", raw["Shadows"])
        assertEquals(null, raw["Particles"])
    }

    @Test
    fun renderVerifiedSucceedsForRealEdits() {
        val content = presetContent("high_dedicated")
        val raw = parse(content)
        val values = editableValues(raw).apply {
            put("FramerateLimit", getValue("FramerateLimit").copy(value = "60", isModified = true))
            put("Setup/FixedScreenHeight", getValue("Setup/FixedScreenHeight").copy(value = "900", isModified = true))
            put(
                "Graphics/OptimisationResolutionMultiplier",
                getValue("Graphics/OptimisationResolutionMultiplier").copy(value = "80", isModified = true)
            )
        }

        val rewritten = ConfigWriter.renderVerified(content, values.values.toList()).getOrThrow()
        val reparsed = parse(rewritten)

        assertEquals("60", reparsed["FramerateLimit"])
        assertEquals("60", reparsed["Graphics/FramerateLimit"])
        assertEquals("900", reparsed["Setup/FixedScreenHeight"])
        assertEquals("80", reparsed["Graphics/OptimisationResolutionMultiplier"])
        assertEquals(
            setOf(
                "FramerateLimit",
                "Graphics/FramerateLimit",
                "Setup/FixedScreenHeight",
                "Graphics/OptimisationResolutionMultiplier"
            ),
            changedKeys(raw, reparsed)
        )
    }

    private fun write(content: String, values: Collection<ParamValue>): String {
        val output = ByteArrayOutputStream()
        ConfigWriter.write(output, values.toList(), content)
        return output.toString(Charsets.UTF_8.name())
    }

    private fun parse(content: String): Map<String, String> =
        ConfigParser.parse(content.byteInputStream())

    private fun editableValues(raw: Map<String, String>): MutableMap<String, ParamValue> =
        ConfigParser.toParamValues(raw).associateBy { it.key }.toMutableMap()

    private fun changedKeys(before: Map<String, String>, after: Map<String, String>): Set<String> =
        (before.keys + after.keys).filter { before[it] != after[it] }.toSet()

    private fun registryFile(current: String, batterySaver: String): String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <registry>
            <key name="HKEY_CURRENT_USER">
                <key name="Software">
                    <key name="Feral Interactive">
                        <key name="GRIDLegends">
                            <key name="GraphicsSettings">
                                $current
                                <key name="BatterySaver">
                                    $batterySaver
                                </key>
                            </key>
                        </key>
                    </key>
                </key>
            </key>
        </registry>
    """.trimIndent()

    companion object {
        fun presetContent(id: String): String =
            File("src/main/assets/presets/$id/preferences").readText(Charsets.UTF_8)
    }
}
