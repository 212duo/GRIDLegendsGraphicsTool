package com.gridlegends.graphicsassistant.data

import java.io.Serializable

/** Root-owned editor source and its matching saveable session. */
class EditorRouteState private constructor(
    val source: EditorSource,
    val session: EditorSessionState
) : Serializable {
    init {
        require(session.sourceKey == source.sessionKey()) {
            "Editor session does not belong to the current source"
        }
    }

    fun withSession(session: EditorSessionState): EditorRouteState =
        EditorRouteState(source, session)

    companion object {
        fun open(source: EditorSource): EditorRouteState = EditorRouteState(
            source = source,
            session = EditorSessionState(sourceKey = source.sessionKey())
        )
    }
}
