package com.gridlegends.graphicsassistant.ui.screens

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gridlegends.graphicsassistant.fileaccess.SafManager
import com.gridlegends.graphicsassistant.fileaccess.ShizukuFileOperator
import com.gridlegends.graphicsassistant.fileaccess.logBuffer
import com.gridlegends.graphicsassistant.fileaccess.ShizukuManager
import com.gridlegends.graphicsassistant.fileaccess.ShizukuStatus
import com.gridlegends.graphicsassistant.ui.theme.*

/**
 * 首页 - 授权引导（支持 Shizuku 和 SAF 两种模式）
 */
@Composable
fun HomeScreen(
    context: Activity,
    onAuthorized: (configPath: String?, configContent: String?, useShizuku: Boolean) -> Unit,
    onAbout: () -> Unit
) {
    val safManager = remember { SafManager(context) }
    val shizukuManager = remember { ShizukuManager(context) }
    val defaultUseShizuku = remember { shizukuManager.shouldUseShizuku() }
    var useShizuku by remember { mutableStateOf(defaultUseShizuku) }

    // Shizuku 状态
    var shizukuStatus by remember { mutableStateOf(shizukuManager.getStatus()) }
    var isGameInstalled by remember { mutableStateOf(safManager.isGameInstalled()) }
    var configFound by remember { mutableStateOf(false) }

    // SAF 授权状态
    var safAuthorized by remember { mutableStateOf(safManager.isAuthorized()) }

    val shizukuOp = remember { ShizukuFileOperator(context, shizukuManager) }
    // 保存找到的配置文件路径和内容
    var foundConfigPath by remember { mutableStateOf<String?>(null) }
    var foundConfigContent by remember { mutableStateOf<String?>(null) }

    // Shizuku 状态变化监听
    DisposableEffect(Unit) {
        shizukuManager.onStateChanged = {
            shizukuStatus = shizukuManager.getStatus()
        }
        shizukuManager.onResult = { granted ->
            if (granted) {
                shizukuStatus = ShizukuStatus.READY
            } else {
                Toast.makeText(context, "Shizuku 授权被拒绝", Toast.LENGTH_SHORT).show()
            }
        }
        onDispose {
            shizukuManager.destroy()
        }
    }

    // 检查配置文件
    LaunchedEffect(useShizuku, shizukuStatus, safAuthorized) {
        Log.d("GLGraphics", "HomeScreen: 检查配置 useShizuku=$useShizuku, shizukuStatus=$shizukuStatus, safAuthorized=$safAuthorized")
        foundConfigPath = null
        foundConfigContent = null
        if (useShizuku) {
            if (shizukuStatus == ShizukuStatus.READY) {
                val found = shizukuOp.findConfigFile()
                Log.d("GLGraphics", "HomeScreen: findConfigFile -> $found")
                foundConfigPath = found
                if (found != null) {
                    foundConfigContent = shizukuOp.readFile(found)
                    Log.d("GLGraphics", "HomeScreen: readFile -> ${foundConfigContent?.length ?: "null"} 字符")
                    configFound = foundConfigContent != null
                } else {
                    configFound = false
                }
            } else {
                configFound = false
            }
        } else if (safAuthorized) {
            configFound = safManager.findConfigFile() != null
            Log.d("GLGraphics", "HomeScreen: SAF configFound=$configFound")
        } else {
            configFound = false
        }
    }

    // SAF 目录选择器
    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                safManager.saveAuthorization(uri)
                safAuthorized = true
                configFound = safManager.findConfigFile() != null
            }
        }
    }

    // 判断是否已就绪（可以进入编辑）
    val isReady = if (useShizuku) {
        shizukuStatus == ShizukuStatus.READY && configFound
    } else {
        safAuthorized && configFound
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 标题
            Text(
                text = "GRID Legends",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            )
            Text(
                text = "画质助手",
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 游戏安装状态
            StatusCard(
                title = "游戏安装状态",
                isOk = isGameInstalled,
                okText = "已检测到游戏",
                failText = "未检测到游戏，请先安装 GRID Legends"
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (useShizuku) {
                // ===== Shizuku 模式 (Android 16+) =====
                ShizukuStatusCard(
                    status = shizukuStatus,
                    onRequestPermission = {
                        shizukuManager.requestPermission()
                    }
                )

                if (shizukuStatus == ShizukuStatus.READY) {
                    Spacer(modifier = Modifier.height(16.dp))
                    StatusCard(
                        title = "配置文件",
                        isOk = configFound,
                        okText = "已找到配置文件",
                        failText = "未找到配置文件，请确认游戏已运行过至少一次"
                    )

                    // 未找到配置文件时显示诊断
                    if (!configFound) {
                        Spacer(modifier = Modifier.height(12.dp))
                        var diagText by remember { mutableStateOf("") }
                        var isDiagLoading by remember { mutableStateOf(false) }

                        OutlinedButton(
                            onClick = {
                                isDiagLoading = true
                                diagText = "正在扫描..."
                                try {
                                    diagText = shizukuOp.diagnose()
                                } catch (e: Exception) {
                                    diagText = "诊断失败: ${e.message}"
                                } finally {
                                    isDiagLoading = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = NeonCyan
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "扫描游戏目录（调试）", fontSize = 14.sp)
                        }

                        if (diagText.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF1A1A2E)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = diagText,
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(12.dp),
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }

                        // 显示 Shizuku 操作日志
                        Spacer(modifier = Modifier.height(8.dp))
                        var showLogs by remember { mutableStateOf(false) }
                        OutlinedButton(
                            onClick = { showLogs = !showLogs },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = AccentOrange
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = if (showLogs) "隐藏日志" else "查看运行日志 (${logBuffer.size}条)", fontSize = 14.sp)
                        }
                        if (showLogs && logBuffer.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF1A1A2E)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = logBuffer.joinToString("\n"),
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(12.dp),
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (shizukuStatus == ShizukuStatus.NOT_INSTALLED) {
                    // 提示安装 Shizuku
                    Text(
                        text = "请先安装 Shizuku 应用\n可在 Google Play 或 GitHub 获取",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                if (isReady) {
                    Button(
                        onClick = {
                            Log.d("GLGraphics", "HomeScreen: 进入编辑 path=$foundConfigPath, content长度=${foundConfigContent?.length}")
                            onAuthorized(foundConfigPath, foundConfigContent, true)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentOrange,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "进入画质编辑",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (!defaultUseShizuku) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { useShizuku = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "改用系统文件夹授权", fontSize = 14.sp)
                    }
                }
            } else {
                // ===== SAF 模式 (Android 15 及以下) =====
                StatusCard(
                    title = "配置目录授权",
                    isOk = safAuthorized,
                    okText = "已授权访问",
                    failText = "需要授权访问游戏配置目录"
                )

                if (safAuthorized) {
                    Spacer(modifier = Modifier.height(16.dp))
                    StatusCard(
                        title = "配置文件",
                        isOk = configFound,
                        okText = "已找到配置文件",
                        failText = "未找到配置文件，请确认游戏已运行过至少一次"
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (!safAuthorized) {
                    Button(
                        onClick = {
                            safLauncher.launch(safManager.createOpenDocumentTreeIntent())
                        },
                        enabled = isGameInstalled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = DarkBackground
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "授权访问游戏目录",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "点击后在弹出的文件选择器中选择：\nAndroid/data/com.feralinteractive.gridlegends_android",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                } else if (isReady) {
                    Button(
                        onClick = {
                            Log.d("GLGraphics", "HomeScreen: 进入编辑 path=$foundConfigPath, content长度=${foundConfigContent?.length}")
                            onAuthorized(foundConfigPath, foundConfigContent, false)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentOrange,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "进入画质编辑",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // 已授权但没找到配置文件
                    OutlinedButton(
                        onClick = {
                            safLauncher.launch(safManager.createOpenDocumentTreeIntent())
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = NeonCyan
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "重新授权目录", fontSize = 16.sp)
                    }
                }

                if (!defaultUseShizuku) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            shizukuStatus = shizukuManager.getStatus()
                            useShizuku = true
                        },
                        enabled = isGameInstalled,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AccentOrange
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "无法选择文件夹？改用 Shizuku",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Android 14/15 如果提示“无法使用此文件夹”，请返回后使用 Shizuku 方式访问。",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 关于按钮
            TextButton(
                onClick = onAbout,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "关于",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

/**
 * Shizuku 状态卡片（带操作按钮）
 */
@Composable
private fun ShizukuStatusCard(
    status: ShizukuStatus,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (status) {
                        ShizukuStatus.READY -> Icons.Default.CheckCircle
                        else -> Icons.Default.Warning
                    },
                    contentDescription = null,
                    tint = if (status == ShizukuStatus.READY) NeonCyan else WarningRed,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Shizuku 授权",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = when (status) {
                            ShizukuStatus.NOT_INSTALLED -> "未安装 Shizuku"
                            ShizukuStatus.NOT_RUNNING -> "Shizuku 服务未启动"
                            ShizukuStatus.NOT_AUTHORIZED -> "等待授权"
                            ShizukuStatus.READY -> "已就绪"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (status == ShizukuStatus.READY) NeonCyan else WarningRed
                    )
                }
            }

            // 需要操作时显示按钮
            if (status == ShizukuStatus.NOT_AUTHORIZED) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = DarkBackground
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "授权 Shizuku", fontWeight = FontWeight.Bold)
                }
            }

            if (status == ShizukuStatus.NOT_RUNNING) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "请打开 Shizuku 应用并启动服务",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

/**
 * 通用状态指示卡片
 */
@Composable
private fun StatusCard(
    title: String,
    isOk: Boolean,
    okText: String,
    failText: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isOk) NeonCyan else WarningRed,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Text(
                    text = if (isOk) okText else failText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isOk) NeonCyan else WarningRed
                )
            }
        }
    }
}
