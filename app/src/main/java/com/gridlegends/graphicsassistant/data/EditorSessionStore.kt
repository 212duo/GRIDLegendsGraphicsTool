package com.gridlegends.graphicsassistant.data

/**
 * Synchronous root-level access to the current editor route.
 *
 * The in-memory route is updated before Compose is notified, so activity-result callbacks can
 * observe the latest session without waiting for recomposition.
 */
class EditorSessionStore(
    initialRoute: EditorRouteState?,
    private val onRouteChanged: (EditorRouteState) -> Unit = {}
) {
    private var currentRoute: EditorRouteState? = initialRoute

    @Synchronized
    fun open(source: EditorSource): EditorRouteState {
        val nextRoute = EditorRouteState.open(source)
        currentRoute = nextRoute
        onRouteChanged(nextRoute)
        return nextRoute
    }

    @Synchronized
    fun currentSession(sourceKey: String): EditorSessionState? =
        currentRoute?.takeIf { it.session.sourceKey == sourceKey }?.session

    @Synchronized
    fun updateSession(
        sourceKey: String,
        transform: (EditorSessionState) -> EditorSessionState
    ): EditorSessionState? {
        val route = currentRoute?.takeIf { it.session.sourceKey == sourceKey } ?: return null
        val nextRoute = route.withSession(transform(route.session))
        currentRoute = nextRoute
        onRouteChanged(nextRoute)
        return nextRoute.session
    }
}
