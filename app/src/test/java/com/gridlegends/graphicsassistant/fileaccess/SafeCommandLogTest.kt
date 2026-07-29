package com.gridlegends.graphicsassistant.fileaccess

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SafeCommandLogTest {
    @Test
    fun summaryUsesOnlyModeExitCodeAndBodyLengths() {
        val commandSentinel = "Q09ORklHX0NPTU1BTkRfU0VDUkVU"
        val outputSentinel = "<Settings>output-secret</Settings>"
        val errorSentinel = "<Error>error-secret</Error>"
        val command = "echo '$commandSentinel' | base64 -d"
        val output = "result:$outputSentinel"
        val error = "failure:$errorSentinel"

        val summary = safeCommandSummary(
            mode = "ROOT",
            command = command,
            output = output,
            error = error,
            exitCode = 0
        )

        assertEquals(
            "ROOT exec commandLength=${command.length} exit=0 " +
                "outputLength=${output.length} errorLength=${error.length}",
            summary
        )
        assertFalse(summary.contains(commandSentinel))
        assertFalse(summary.contains(outputSentinel))
        assertFalse(summary.contains(errorSentinel))
    }
}
