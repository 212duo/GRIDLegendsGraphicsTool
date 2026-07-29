package com.gridlegends.graphicsassistant.fileaccess

internal fun safeCommandSummary(
    mode: String,
    command: String,
    output: String,
    error: String,
    exitCode: Int
): String = "$mode exec commandLength=${command.length} exit=$exitCode " +
    "outputLength=${output.length} errorLength=${error.length}"

internal fun safeFileLookupSummary(
    mode: String,
    operation: String,
    candidatePath: String,
    output: String,
    found: Boolean,
    exitCode: Int
): String = "$mode lookup operation=$operation candidateLength=${candidatePath.length} " +
    "found=$found outputLength=${output.length} exit=$exitCode"

internal fun safePathResultSummary(
    mode: String,
    operation: String,
    path: String?
): String = "$mode pathResult operation=$operation found=${path != null} " +
    "pathLength=${path?.length ?: 0}"
