package com.gridlegends.graphicsassistant

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.gridlegends.graphicsassistant.data.EditorSource
import com.gridlegends.graphicsassistant.ui.theme.GridLegendsTheme
import com.gridlegends.graphicsassistant.ui.screens.AboutScreen
import com.gridlegends.graphicsassistant.ui.screens.EditorScreen
import com.gridlegends.graphicsassistant.ui.screens.HomeScreen

/**
 * 页面路由
 */
private enum class Screen {
    HOME, EDITOR, ABOUT
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GridLegendsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by remember { mutableStateOf(Screen.HOME) }
                    var previousScreen by remember { mutableStateOf(Screen.HOME) }
                    var editorSource by remember { mutableStateOf<EditorSource?>(null) }

                    fun navigateToAbout() {
                        previousScreen = currentScreen
                        currentScreen = Screen.ABOUT
                    }

                    fun navigateBack() {
                        currentScreen = when (currentScreen) {
                            Screen.EDITOR -> Screen.HOME
                            Screen.ABOUT -> previousScreen
                            Screen.HOME -> Screen.HOME
                        }
                    }

                    BackHandler(
                        enabled = currentScreen != Screen.HOME &&
                            (currentScreen != Screen.EDITOR || editorSource == null)
                    ) {
                        navigateBack()
                    }

                    when (currentScreen) {
                        Screen.HOME -> {
                            HomeScreen(
                                context = this@MainActivity,
                                onAuthorized = { path, content, accessMode ->
                                    editorSource = EditorSource.LiveConfig(accessMode, path, content)
                                    currentScreen = Screen.EDITOR
                                },
                                onAbout = { navigateToAbout() }
                            )
                        }
                        Screen.EDITOR -> {
                            val source = editorSource
                            if (source == null) {
                                LaunchedEffect(Unit) { navigateBack() }
                            } else {
                                EditorScreen(
                                    context = this@MainActivity,
                                    source = source,
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
