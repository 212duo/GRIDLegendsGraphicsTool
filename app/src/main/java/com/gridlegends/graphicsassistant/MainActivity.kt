package com.gridlegends.graphicsassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
                    // 保存找到的配置文件路径，在页面间传递
                    var configFilePath by remember { mutableStateOf<String?>(null) }
                    var configFileContent by remember { mutableStateOf<String?>(null) }

                    when (currentScreen) {
                        Screen.HOME -> {
                            HomeScreen(
                                context = this@MainActivity,
                                onAuthorized = { path, content ->
                                    configFilePath = path
                                    configFileContent = content
                                    currentScreen = Screen.EDITOR
                                },
                                onAbout = { currentScreen = Screen.ABOUT }
                            )
                        }
                        Screen.EDITOR -> {
                            EditorScreen(
                                context = this@MainActivity,
                                configPath = configFilePath,
                                configContent = configFileContent,
                                onBack = { currentScreen = Screen.HOME },
                                onAbout = { currentScreen = Screen.ABOUT }
                            )
                        }
                        Screen.ABOUT -> {
                            AboutScreen(
                                onBack = { currentScreen = Screen.HOME }
                            )
                        }
                    }
                }
            }
        }
    }
}
