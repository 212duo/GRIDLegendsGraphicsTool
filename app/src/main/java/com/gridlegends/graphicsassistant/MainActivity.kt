package com.gridlegends.graphicsassistant

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.gridlegends.graphicsassistant.data.EditorRouteState
import com.gridlegends.graphicsassistant.data.EditorSessionStore
import com.gridlegends.graphicsassistant.data.EditorSource
import com.gridlegends.graphicsassistant.ui.theme.GridLegendsTheme
import com.gridlegends.graphicsassistant.ui.screens.AboutScreen
import com.gridlegends.graphicsassistant.ui.screens.EditorScreen
import com.gridlegends.graphicsassistant.ui.screens.HomeScreen
import com.gridlegends.graphicsassistant.ui.screens.PresetLibraryScreen

/**
 * 页面路由
 */
private enum class Screen {
    HOME, PRESET_LIBRARY, EDITOR, ABOUT
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GridLegendsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by rememberSaveable { mutableStateOf(Screen.HOME) }
                    var previousScreen by rememberSaveable { mutableStateOf(Screen.HOME) }
                    var editorRoute by rememberSaveable { mutableStateOf<EditorRouteState?>(null) }
                    val editorSessionStore = remember {
                        EditorSessionStore(editorRoute) { nextRoute ->
                            editorRoute = nextRoute
                        }
                    }

                    fun openEditor(source: EditorSource) {
                        editorSessionStore.open(source)
                        currentScreen = Screen.EDITOR
                    }

                    fun navigateToAbout() {
                        previousScreen = currentScreen
                        currentScreen = Screen.ABOUT
                    }

                    fun navigateBack() {
                        currentScreen = when (currentScreen) {
                            Screen.EDITOR -> if (editorRoute?.source is EditorSource.BundledPreset) {
                                Screen.PRESET_LIBRARY
                            } else {
                                Screen.HOME
                            }
                            Screen.PRESET_LIBRARY -> Screen.HOME
                            Screen.ABOUT -> previousScreen
                            Screen.HOME -> Screen.HOME
                        }
                    }

                    BackHandler(
                        enabled = currentScreen != Screen.HOME &&
                            (currentScreen != Screen.EDITOR || editorRoute == null)
                    ) {
                        navigateBack()
                    }

                    when (currentScreen) {
                        Screen.HOME -> {
                            HomeScreen(
                                context = this@MainActivity,
                                onAuthorized = { path, content, accessMode ->
                                    openEditor(EditorSource.LiveConfig(accessMode, path, content))
                                },
                                onPresetExport = { currentScreen = Screen.PRESET_LIBRARY },
                                onAbout = { navigateToAbout() }
                            )
                        }
                        Screen.PRESET_LIBRARY -> {
                            PresetLibraryScreen(
                                onBack = { currentScreen = Screen.HOME },
                                onPresetSelected = { preset ->
                                    openEditor(EditorSource.BundledPreset(preset))
                                }
                            )
                        }
                        Screen.EDITOR -> {
                            val route = editorRoute
                            if (route == null) {
                                LaunchedEffect(Unit) { navigateBack() }
                            } else {
                                EditorScreen(
                                    context = this@MainActivity,
                                    source = route.source,
                                    session = route.session,
                                    sessionStore = editorSessionStore,
                                    onBack = { navigateBack() },
                                    onAbout = { navigateToAbout() }
                                )
                            }
                        }
                        Screen.ABOUT -> {
                            AboutScreen(
                                onBack = { navigateBack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
