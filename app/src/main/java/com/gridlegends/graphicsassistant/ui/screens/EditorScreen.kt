package com.gridlegends.graphicsassistant.ui.screens

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gridlegends.graphicsassistant.data.*
import com.gridlegends.graphicsassistant.fileaccess.AccessMode
import com.gridlegends.graphicsassistant.fileaccess.FileOperator
import com.gridlegends.graphicsassistant.fileaccess.RootFileOperator
import com.gridlegends.graphicsassistant.fileaccess.RootManager
import com.gridlegends.graphicsassistant.fileaccess.SafManager
import com.gridlegends.graphicsassistant.fileaccess.ShizukuFileOperator
import com.gridlegends.graphicsassistant.fileaccess.ShizukuManager
import com.gridlegends.graphicsassistant.ui.components.*
import com.gridlegends.graphicsassistant.ui.theme.*

/**
 * 画质编辑主界面（支持 Root、Shizuku 和 SAF 三种模式）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    context: Activity,
    source: EditorSource,
    onBack: () -> Unit,
    onAbout: () -> Unit
) {
    val shizukuManager = remember { ShizukuManager(context) }
    val rootManager = remember { RootManager() }
    val safManager = remember { SafManager(context) }

    val shizukuOp = remember { ShizukuFileOperator(context, shizukuManager) }
    val rootOp = remember { RootFileOperator(rootManager) }
    val safOp = remember { FileOperator(context, safManager) }

    val sourceKey = source.stableKey()
    var session by rememberSaveable(sourceKey) {
        mutableStateOf(EditorSessionState(sourceKey = sourceKey))
    }
    var isSaving by remember { mutableStateOf(false) }
    val bundledPreset = (source as? EditorSource.BundledPreset)?.preset
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var showInstallDialog by rememberSaveable { mutableStateOf(false) }

    val paramValues = session.paramValues
    val rawConfigValues = session.rawConfigValues
    val isLoading = !session.isInitialized && session.loadError == null
    val loadError = session.loadError

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri == null) {
            session = session.cancelExport()
        } else {
            val bytes = session.pendingExportBytes
            val snapshot = session.pendingExportSnapshot
            if (bytes == null || snapshot == null) {
                session = session.cancelExport()
                Toast.makeText(context, "导出状态已丢失，请重新导出", Toast.LENGTH_LONG).show()
            } else {
                try {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                        output.write(bytes)
                        output.flush()
                    } ?: error("无法打开目标文件")
                    session = session.completeExport()
                    showInstallDialog = true
                } catch (e: Exception) {
                    session = session.cancelExport()
                    Toast.makeText(context, "导出失败：${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 加载配置文件
    LaunchedEffect(sourceKey) {
        if (session.isInitialized || session.loadError != null) return@LaunchedEffect
        Log.d("GLGraphics", "EditorScreen: 开始加载 ${source.logSummary()}")
        try {
            val rawValues = when (val currentSource = source) {
                is EditorSource.BundledPreset -> currentSource.preset.rawValues
                is EditorSource.LiveConfig -> if (currentSource.configContent != null) {
                    Log.d(
                        "GLGraphics",
                        "EditorScreen: 使用传入的 configContent (${currentSource.configContent.length} 字符)"
                    )
                    ConfigParser.parse(currentSource.configContent.byteInputStream())
                } else {
                    when (currentSource.accessMode) {
                        AccessMode.ROOT -> currentSource.configPath?.let(rootOp::readConfigFromPath)
                            ?: rootOp.readConfig()
                        AccessMode.SHIZUKU -> currentSource.configPath?.let(shizukuOp::readConfigFromPath)
                            ?: shizukuOp.readConfig()
                        AccessMode.SAF -> safOp.readConfig()
                    }
                } ?: error("未找到配置文件")
            }
            val parsedValues = ConfigParser.toParamValues(rawValues).associateBy { it.key }
            session = EditorSessionState(
                sourceKey = sourceKey,
                rawConfigValues = rawValues,
                paramValues = parsedValues,
                isInitialized = true,
                lastExportedSnapshot = parsedValues.mapValues { it.value.value }
            )
            Log.d("GLGraphics", "EditorScreen: 解析到 ${parsedValues.size} 个画质参数")
        } catch (e: Exception) {
            session = session.copyForEditor(
                loadError = "读取配置失败：${e.message}"
            )
            Log.e("GLGraphics", "EditorScreen: 读取异常", e)
        }
    }

    val currentSnapshot = paramValues.mapValues { it.value.value }
    val hasModifications = paramValues.values.any { it.isModified }
    val hasUnexportedChanges = bundledPreset != null &&
        session.lastExportedSnapshot.isNotEmpty() &&
        currentSnapshot != session.lastExportedSnapshot

    fun requestBack() {
        if (hasUnexportedChanges) showDiscardDialog = true else onBack()
    }

    fun applyPreset(preset: GraphicsPreset) {
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
        session = session.copyForEditor(paramValues = next)
    }

    fun saveLiveConfig(liveSource: EditorSource.LiveConfig) {
        isSaving = true
        try {
            val success = when (liveSource.accessMode) {
                AccessMode.ROOT -> liveSource.configPath?.let {
                    rootOp.writeConfigToPath(it, paramValues)
                } ?: rootOp.writeConfig(paramValues)
                AccessMode.SHIZUKU -> liveSource.configPath?.let {
                    shizukuOp.writeConfigToPath(it, paramValues)
                } ?: shizukuOp.writeConfig(paramValues)
                AccessMode.SAF -> safOp.writeConfig(paramValues)
            }
            if (success) {
                session = session.copyForEditor(
                    paramValues = paramValues.mapValues { (_, value) ->
                        value.copy(originalValue = value.value, isModified = false)
                    }
                )
                Toast.makeText(
                    context,
                    "保存成功！请重启游戏使设置生效",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(context, "保存失败，请检查权限", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "保存失败：${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            isSaving = false
        }
    }

    fun runBottomAction() {
        when (source) {
            is EditorSource.BundledPreset -> {
                isSaving = true
                ConfigExporter.build(source.preset, paramValues).fold(
                    onSuccess = { bytes ->
                        session = session.beginExport(bytes, currentSnapshot)
                        exportLauncher.launch("preferences")
                    },
                    onFailure = { error ->
                        Toast.makeText(
                            context,
                            "导出内容校验失败：${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
                isSaving = false
            }
            is EditorSource.LiveConfig -> saveLiveConfig(source)
        }
    }

    val showBottomAction = when {
        isLoading || loadError != null -> false
        bundledPreset != null -> true
        else -> hasModifications
    }

    BackHandler { requestBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部栏
            TopAppBar(
                title = { Text(if (bundledPreset != null) "制作画质文件" else "画质编辑") },
                navigationIcon = {
                    IconButton(onClick = ::requestBack) {
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
                            session = session.copyForEditor(
                                paramValues = paramValues.mapValues { (_, v) ->
                                    v.copy(value = v.originalValue, isModified = false)
                                }
                            )
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
                        if (bundledPreset != null) {
                            item(key = "preset_source") {
                                PresetSourceBanner(bundledPreset)
                            }
                        } else {
                            item(key = "graphics_presets") {
                                PresetCard(onApplyPreset = ::applyPreset)
                            }
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
                                                        session = session.copyForEditor(
                                                            paramValues = paramValues.toMutableMap().apply {
                                                                put(
                                                                    param.key,
                                                                    paramValue.copy(
                                                                        value = newValue,
                                                                        isModified = newValue != paramValue.originalValue
                                                                    )
                                                                )
                                                            }
                                                            )
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (bundledPreset == null) {
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
                        }

                        // 底部间距
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }

        // 底部保存按钮
        if (showBottomAction) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter),
                color = DarkSurface,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = ::runBottomAction,
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
                            text = if (bundledPreset != null) "导出 preferences" else "保存设置",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("放弃未导出的修改？") },
            text = { Text("返回后，本次尚未导出的修改会丢失。") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onBack()
                }) { Text("放弃") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("继续编辑") }
            }
        )
    }

    if (showInstallDialog) {
        AlertDialog(
            onDismissRequest = { showInstallDialog = false },
            title = { Text("导出成功") },
            text = {
                Text(
                    "请先关闭游戏并备份原文件，再将导出的 preferences 复制到：\n\n" +
                        "Android/data/com.feralinteractive.gridlegends_android/" +
                        "files/feral_app_support/preferences"
                )
            },
            confirmButton = {
                TextButton(onClick = { showInstallDialog = false }) { Text("知道了") }
            }
        )
    }
}

private fun EditorSource.stableKey(): String = when (this) {
    is EditorSource.LiveConfig -> "live:$accessMode:${configPath.orEmpty()}"
    is EditorSource.BundledPreset -> "preset:${preset.metadata.id}"
}

private fun EditorSessionState.copyForEditor(
    rawConfigValues: Map<String, String> = this.rawConfigValues,
    paramValues: Map<String, ParamValue> = this.paramValues,
    isInitialized: Boolean = this.isInitialized,
    loadError: String? = this.loadError,
    lastExportedSnapshot: Map<String, String> = this.lastExportedSnapshot
): EditorSessionState = EditorSessionState(
    sourceKey = sourceKey,
    rawConfigValues = rawConfigValues,
    paramValues = paramValues,
    isInitialized = isInitialized,
    loadError = loadError,
    pendingExportBytes = pendingExportBytes,
    pendingExportSnapshot = pendingExportSnapshot,
    lastExportedSnapshot = lastExportedSnapshot
)

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
