package com.gridlegends.graphicsassistant.fileaccess

internal fun safeCommandSummary(
    mode: String,
    command: String,
    output: String,
    error: String,
    exitCode: Int
): String = "$mode exec commandLength=${command.length} exit=$exitCode " +
    "outputLength=${output.length} errorLength=${error.length}"
