package com.gridlegends.graphicsassistant.ui.screens

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gridlegends.graphicsassistant.data.*
import com.gridlegends.graphicsassistant.fileaccess.FileOperator
import com.gridlegends.graphicsassistant.fileaccess.SafManager
import com.gridlegends.graphicsassistant.fileaccess.ShizukuFileOperator
import com.gridlegends.graphicsassistant.fileaccess.ShizukuManager
import com.gridlegends.graphicsassistant.fileaccess.ShizukuStatus
import com.gridlegends.graphicsassistant.ui.components.*
import com.gridlegends.graphicsassistant.ui.theme.*

/**
 * 画质编辑主界面（支持 Shizuku 和 SAF 两种模式）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    context: Activity,
    configPath: String?,
    configContent: String?,
    onBack: () -> Unit,
    onAbout: () -> Unit
) {
    val shizukuManager = remember { ShizukuManager(context) }
    val safManager = remember { SafManager(context) }
    val useShizuku = remember { shizukuManager.shouldUseShizuku() }

    val shizukuOp = remember { ShizukuFileOperator(context, shizukuManager) }
    val safOp = remember { FileOperator(context, safManager) }

    // 参数当前值状态
    var paramValues by remember { mutableStateOf(emptyMap<String, ParamValue>()) }
    var rawConfigValues by remember { mutableStateOf(emptyMap<String, String>()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    // 加载配置文件
    LaunchedEffect(Unit) {
        Log.d("GLGraphics", "EditorScreen: 开始加载 configPath=$configPath, configContent=${configContent?.length ?: "null"} 字符, useShizuku=$useShizuku")
        try {
            val rawValues = if (configContent != null) {
                Log.d("GLGraphics", "EditorScreen: 使用传入的 configContent (${configContent.length} 字符)")
                ConfigParser.parse(configContent.byteInputStream())
            } else if (useShizuku && configPath != null) {
                Log.d("GLGraphics", "EditorScreen: configContent 为空，用 configPath 直接读取: $configPath")
                shizukuOp.readConfigFromPath(configPath)
            } else if (useShizuku) {
                Log.d("GLGraphics", "EditorScreen: 回退到 shizukuOp.readConfig()")
                shizukuOp.readConfig()
            } else {
                Log.d("GLGraphics", "EditorScreen: 使用 SAF readConfig()")
                safOp.readConfig()
            }
            Log.d("GLGraphics", "EditorScreen: rawValues=${rawValues?.size ?: "null"} 个参数")
            if (rawValues != null) {
                rawConfigValues = rawValues
                val values = ConfigParser.toParamValues(rawValues)
                paramValues = values.associateBy { it.key }
                Log.d("GLGraphics", "EditorScreen: 解析到 ${paramValues.size} 个画质参数")
            } else {
                loadError = "未找到配置文件"
                Log.w("GLGraphics", "EditorScreen: rawValues 为 null")
            }
        } catch (e: Exception) {
            loadError = "读取配置失败：${e.message}"
            Log.e("GLGraphics", "EditorScreen: 读取异常", e)
        } finally {
            isLoading = false
        }
    }

    // 是否有修改
    val hasModifications = paramValues.values.any { it.isModified }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部栏
            TopAppBar(
                title = { Text("画质编辑") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    // 恢复默认按钮
                    IconButton(
                        onClick = {
                            paramValues = paramValues.mapValues { (_, v) ->
                                v.copy(value = v.originalValue, isModified = false)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "恢复默认",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )

            // 内容区域
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonCyan)
                    }
                }
                loadError != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = loadError!!,
                            color = WarningRed,
                            fontSize = 16.sp
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        item(key = "graphics_presets") {
                            PresetCard(
                                onApplyPreset = { preset ->
                                    val next = paramValues.toMutableMap()
                                    preset.values.forEach { (key, value) ->
                                        val originalValue = next[key]?.originalValue
                                            ?: rawConfigValues[key]
                                            ?: value
                                        next[key] = ParamValue(
                                            key = key,
                                            value = value,
                                            originalValue = originalValue,
                                            isModified = value != originalValue
                                        )
                                    }
                                    paramValues = next
                                }
                            )
                        }

                        // 按分区显示参数
                        ParamSection.entries.forEach { section ->
                            val sectionParams = ParamDefinitions.getParamsBySection(section)
                            if (sectionParams.isNotEmpty()) {
                                item(key = "section_${section.name}") {
                                    SectionCard(
                                        section = section,
                                        paramCount = sectionParams.size,
                                        modifiedCount = sectionParams.count {
                                            paramValues[it.key]?.isModified == true
                                        }
                                    ) {
                                        // 分区内的参数
                                        sectionParams.forEach { param ->
                                            val paramValue = paramValues[param.key]
                                            if (paramValue != null) {
                                                ParamItem(
                                                    param = param,
                                                    paramValue = paramValue,
                                                    onValueChange = { newValue ->
                                                        paramValues = paramValues.toMutableMap().apply {
                                                            put(
                                                                param.key,
                                                                paramValue.copy(
                                                                    value = newValue,
                                                                    isModified = newValue != paramValue.originalValue
                                                                )
                                                            )
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item(key = "about_link") {
                            TextButton(
                                onClick = onAbout,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "关于本工具",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // 底部间距
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }

        // 底部保存按钮
        if (hasModifications) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter),
                color = DarkSurface,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = {
                        isSaving = true
                        try {
                            val success = if (useShizuku && configPath != null) {
                                shizukuOp.writeConfigToPath(configPath, paramValues)
                            } else if (useShizuku) {
                                shizukuOp.writeConfig(paramValues)
                            } else {
                                safOp.writeConfig(paramValues)
                            }
                            if (success) {
                                // 更新原始值
                                paramValues = paramValues.mapValues { (_, v) ->
                                    v.copy(originalValue = v.value, isModified = false)
                                }
                                Toast.makeText(
                                    context,
                                    "保存成功！请重启游戏使设置生效",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "保存失败，请检查权限",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "保存失败：${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        } finally {
                            isSaving = false
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentOrange,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "保存设置",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * 单个参数项
 */
@Composable
private fun PresetCard(
    onApplyPreset: (GraphicsPreset) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf(GraphicsPresets.all.first()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "画质预设",
                color = NeonCyan,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = selectedPreset.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.92f)
                ) {
                    GraphicsPresets.all.forEach { preset ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(text = preset.name, color = TextPrimary)
                                    Text(
                                        text = preset.description,
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            },
                            onClick = {
                                selectedPreset = preset
                                expanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = selectedPreset.description,
                color = TextSecondary,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onApplyPreset(selectedPreset) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = DarkBackground
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "应用预设",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ParamItem(
    param: GraphicsParam,
    paramValue: ParamValue,
    onValueChange: (String) -> Unit
) {
    when (param.controlType) {
        ParamControlType.SLIDER -> {
            ParamSlider(
                param = param,
                paramValue = paramValue,
                onValueChange = onValueChange
            )
        }
        ParamControlType.SWITCH -> {
            ParamSwitch(
                param = param,
                paramValue = paramValue,
                onValueChange = onValueChange
            )
        }
        ParamControlType.RADIO_GROUP,
        ParamControlType.LEVEL_SELECTOR -> {
            ParamRadioGroup(
                param = param,
                paramValue = paramValue,
                onValueChange = onValueChange
            )
        }
    }
}
