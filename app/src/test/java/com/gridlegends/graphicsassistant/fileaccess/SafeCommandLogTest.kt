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

    @Test
    fun lookupSummaryDoesNotExposeCandidatePathOrCommandOutput() {
        val sentinel = "lookup-secret-9d12f3c7"
        val candidatePath = "/sdcard/Android/data/$sentinel/preferences"
        val output = "$candidatePath\n<Settings>$sentinel</Settings>"

        val summary = safeFileLookupSummary(
            mode = "SHIZUKU",
            operation = "recursive",
            candidatePath = candidatePath,
            output = output,
            found = true,
            exitCode = 0
        )

        assertEquals(
            "SHIZUKU lookup operation=recursive candidateLength=${candidatePath.length} " +
                "found=true outputLength=${output.length} exit=0",
            summary
        )
        assertFalse(summary.contains(sentinel))
        assertFalse(summary.contains(candidatePath))
        assertFalse(summary.contains("<Settings>"))
    }

    @Test
    fun pathResultSummaryDoesNotExposeReturnedPath() {
        val sentinel = "returned-path-secret-a5c30d7b"
        val path = "/sdcard/Android/data/$sentinel/preferences"

        val summary = safePathResultSummary(
            mode = "ROOT",
            operation = "home-find",
            path = path
        )

        assertEquals(
            "ROOT pathResult operation=home-find found=true pathLength=${path.length}",
            summary
        )
        assertFalse(summary.contains(sentinel))
        assertFalse(summary.contains(path))
    }
}
