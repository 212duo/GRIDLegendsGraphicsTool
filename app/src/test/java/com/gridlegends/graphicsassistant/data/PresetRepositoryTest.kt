package com.gridlegends.graphicsassistant.data

import java.nio.charset.CharacterCodingException
import org.junit.Assert.assertEquals
import org.junit.Test

class PresetRepositoryTest {

    @Test
    fun validUtf8IsDecodedExactly() {
        assertEquals("画质 preferences", decodeUtf8Strict("画质 preferences".toByteArray()))
    }

    @Test(expected = CharacterCodingException::class)
    fun malformedUtf8IsRejected() {
        decodeUtf8Strict(byteArrayOf(0xC3.toByte(), 0x28))
    }
}
