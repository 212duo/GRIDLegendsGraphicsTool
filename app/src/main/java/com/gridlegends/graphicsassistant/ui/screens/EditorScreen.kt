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
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
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
import com.gridlegends.graphicsassistant.fileaccess.ConfigBackups
import com.gridlegends.graphicsassistant.fileaccess.ConfigFileGateway
import com.gridlegends.graphicsassistant.fileaccess.FileOperator
import com.gridlegends.graphicsassistant.fileaccess.LiveConfigSaver
import com.gridlegends.graphicsassistant.fileaccess.RootFileOperator
import com.gridlegends.graphicsassistant.fileaccess.RootManager
import com.gridlegends.graphicsassistant.fileaccess.SafManager
import com.gridlegends.graphicsassistant.fileaccess.SaveOutcome
import com.gridlegends.graphicsassistant.fileaccess.ShizukuFileOperator
import com.gridlegends.graphicsassistant.ui.components.*
import com.gridlegends.graphicsassistant.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 画质编辑主界面（支持 Root、Shizuku 和 SAF 三种模式）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    context: Activity,
    source: EditorSource,
    session: EditorSessionState,
    sessionStore: EditorSessionStore,
    onBack: () -> Unit,
    onAbout: () -> Unit
) {
    val shizukuOp = remember { ShizukuFileOperator() }
    val rootOp = remember { RootFileOperator(RootManager()) }
    val safOp = remember { FileOperator(context, SafManager(context)) }

    val sourceKey = source.sessionKey()
    val scope = rememberCoroutineScope()
    val presetRepository = remember { PresetRepository(context.applicationContext) }
    var isSaving by remember { mutableStateOf(false) }
    var isApplyingPreset by remember { mutableStateOf(false) }
    val bundledPreset = (source as? EditorSource.BundledPreset)?.preset
    val liveSource = source as? EditorSource.LiveConfig
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var showInstallDialog by rememberSaveable { mutableStateOf(false) }
    var backupNames by remember { mutableStateOf<List<String>?>(null) }
    var restoreCandidate by rememberSaveable { mutableStateOf<String?>(null) }
    var gameRunningGateway by remember { mutableStateOf<ConfigFileGateway?>(null) }

    fun toast(message: String, long: Boolean = false) {
        Toast.makeText(context, message, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }

    /** Blocking: resolves the live config for this source's access mode. */
    fun resolveGateway(live: EditorSource.LiveConfig): ConfigFileGateway? = when (live.accessMode) {
        AccessMode.ROOT -> (live.configPath ?: rootOp.findConfigFile())?.let(rootOp::gateway)
        AccessMode.SHIZUKU -> (live.configPath ?: shizukuOp.findConfigFile())?.let(shizukuOp::gateway)
        AccessMode.SAF -> safOp.gateway()
    }

    fun initializedSession(rawValues: Map<String, String>): EditorSessionState {
        val parsedValues = ConfigParser.toParamValues(rawValues).associateBy { it.key }
        return EditorSessionState(
            sourceKey = sourceKey,
            rawConfigValues = rawValues,
            paramValues = parsedValues,
            isInitialized = true,
            lastExportedSnapshot = parsedValues.mapValues { it.value.value }
        )
    }

    val paramValues = session.paramValues
    val isLoading = !session.isInitialized && session.loadError == null
    val loadError = session.loadError

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri == null) {
            sessionStore.updateSession(sourceKey) { it.cancelExport() }
        } else {
            val currentSession = sessionStore.currentSession(sourceKey)
            val bytes = currentSession?.pendingExportBytes
            val snapshot = currentSession?.pendingExportSnapshot
            if (bytes == null || snapshot == null) {
                sessionStore.updateSession(sourceKey) { it.cancelExport() }
                Toast.makeText(context, "导出状态已丢失，请重新导出", Toast.LENGTH_LONG).show()
            } else {
                try {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                        output.write(bytes)
                        output.flush()
                    } ?: error("无法打开目标文件")
                    sessionStore.updateSession(sourceKey) { it.completeExport() }
                    showInstallDialog = true
                } catch (e: Exception) {
                    sessionStore.updateSession(sourceKey) { it.cancelExport() }
                    Toast.makeText(context, "导出失败：${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 加载配置文件
    LaunchedEffect(sourceKey) {
        val currentSession = sessionStore.currentSession(sourceKey) ?: return@LaunchedEffect
        if (currentSession.isInitialized || currentSession.loadError != null) return@LaunchedEffect
        Log.d("GLGraphics", "EditorScreen: 开始加载 ${source.logSummary()}")
        try {
            val rawValues = withContext(Dispatchers.IO) {
                when (val currentSource = source) {
                    is EditorSource.BundledPreset -> currentSource.preset.rawValues
                    is EditorSource.LiveConfig -> {
                        val content = currentSource.configContent
                            ?: resolveGateway(currentSource)?.read()
                            ?: error("未找到配置文件")
                        ConfigParser.parse(content.byteInputStream())
                    }
                }
            }
            val loadedSession = initializedSession(rawValues)
            sessionStore.updateSession(sourceKey) { loadedSession }
            Log.d("GLGraphics", "EditorScreen: 解析到 ${loadedSession.paramValues.size} 个画质参数")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            sessionStore.updateSession(sourceKey) {
                it.copyForEditor(loadError = "读取配置失败：${e.message}")
            }
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

    fun applyPreset(metadata: PresetMetadata) {
        if (isApplyingPreset) return
        isApplyingPreset = true
        scope.launch {
            presetRepository.load(metadata).fold(
                onSuccess = { preset ->
                    var applied: PresetApplication.Applied? = null
                    sessionStore.updateSession(sourceKey) { current ->
                        val result = PresetApplication.apply(
                            currentValues = current.paramValues,
                            targetRawValues = current.rawConfigValues,
                            presetRawValues = preset.rawValues
                        )
                        applied = result
                        current.copyForEditor(paramValues = result.paramValues)
                    }
                    val skipped = applied?.skippedKeys.orEmpty()
                    val message = if (skipped.isEmpty()) {
                        "已应用「${metadata.name}」，保存后生效"
                    } else {
                        "已应用「${metadata.name}」，当前配置缺少 ${skipped.size} 项，已跳过"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                },
                onFailure = { error ->
                    Toast.makeText(context, "预设读取失败：${error.message}", Toast.LENGTH_LONG).show()
                }
            )
            isApplyingPreset = false
        }
    }

    suspend fun performSave(gateway: ConfigFileGateway, stopGameFirst: Boolean) {
        isSaving = true
        val values = sessionStore.currentSession(sourceKey)?.paramValues?.values?.toList()
        val outcome = if (values == null) {
            SaveOutcome.Failed("编辑状态已丢失，请重新进入编辑")
        } else {
            withContext(Dispatchers.IO) {
                if (stopGameFirst && !gateway.stopGame()) {
                    SaveOutcome.Failed("关闭游戏失败，未保存")
                } else {
                    LiveConfigSaver.save(gateway, values)
                }
            }
        }
        when (outcome) {
            is SaveOutcome.Saved -> {
                val savedRawValues = ConfigParser.parse(outcome.content.byteInputStream())
                sessionStore.updateSession(sourceKey) { current ->
                    current.copyForEditor(
                        rawConfigValues = savedRawValues,
                        paramValues = current.paramValues.mapValues { (_, value) ->
                            value.copy(originalValue = value.value, isModified = false)
                        }
                    )
                }
                toast("保存成功，已自动备份原配置。下次启动游戏生效", long = true)
            }
            is SaveOutcome.Failed -> toast("保存失败：${outcome.message}", long = true)
        }
        isSaving = false
    }

    fun saveLiveConfig(live: EditorSource.LiveConfig) {
        if (isSaving) return
        isSaving = true
        scope.launch {
            val gateway = withContext(Dispatchers.IO) { resolveGateway(live) }
            if (gateway == null) {
                toast("未找到配置文件，请返回首页重新检测", long = true)
                isSaving = false
                return@launch
            }
            if (withContext(Dispatchers.IO) { gateway.isGameRunning() } == true) {
                gameRunningGateway = gateway
                isSaving = false
                return@launch
            }
            performSave(gateway, stopGameFirst = false)
        }
    }

    fun openBackupList(live: EditorSource.LiveConfig) {
        scope.launch {
            val names = withContext(Dispatchers.IO) {
                resolveGateway(live)?.listBackups()?.let(ConfigBackups::newestFirst)
            }
            when {
                names == null -> toast("无法读取备份列表，请检查权限")
                names.isEmpty() -> toast("暂无备份。每次保存前都会自动备份原配置")
                else -> backupNames = names
            }
        }
    }

    fun restoreBackup(live: EditorSource.LiveConfig, backupName: String) {
        if (isSaving) return
        isSaving = true
        scope.launch {
            val outcome = withContext(Dispatchers.IO) {
                resolveGateway(live)?.let { LiveConfigSaver.restore(it, backupName) }
                    ?: SaveOutcome.Failed("未找到配置文件")
            }
            when (outcome) {
                is SaveOutcome.Saved -> {
                    val restoredSession = initializedSession(
                        ConfigParser.parse(outcome.content.byteInputStream())
                    )
                    sessionStore.updateSession(sourceKey) { restoredSession }
                    toast("已恢复备份，恢复前的配置也已备份")
                }
                is SaveOutcome.Failed -> toast("恢复失败：${outcome.message}", long = true)
            }
            isSaving = false
        }
    }

    fun exportPreset(preset: LoadedPreset) {
        if (isSaving) return
        val exportParamValues = sessionStore.currentSession(sourceKey)?.paramValues
        if (exportParamValues == null) {
            toast("导出状态已丢失，请重新导出", long = true)
            return
        }
        isSaving = true
        scope.launch {
            val exportSnapshot = exportParamValues.mapValues { it.value.value }
            withContext(Dispatchers.Default) {
                ConfigExporter.build(preset, exportParamValues)
            }.fold(
                onSuccess = { bytes ->
                    val pendingSession = sessionStore.updateSession(sourceKey) { current ->
                        current.beginExport(bytes, exportSnapshot)
                    }
                    if (pendingSession == null) {
                        toast("导出状态已丢失，请重新导出", long = true)
                    } else {
                        exportLauncher.launch("preferences")
                    }
                },
                onFailure = { error -> toast("导出内容校验失败：${error.message}", long = true) }
            )
            isSaving = false
        }
    }

    fun runBottomAction() {
        when (source) {
            is EditorSource.BundledPreset -> exportPreset(source.preset)
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
                    IconButton(
                        onClick = {
                            sessionStore.updateSession(sourceKey) { current ->
                                current.copyForEditor(
                                    paramValues = current.paramValues.mapValues { (_, value) ->
                                        value.copy(value = value.originalValue, isModified = false)
                                    }
                                )
                            }
                        },
                        enabled = hasModifications && !isSaving
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "撤销未保存修改",
                            tint = TextSecondary
                        )
                    }
                    if (liveSource != null) {
                        IconButton(
                            onClick = { openBackupList(liveSource) },
                            enabled = !isSaving && !isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "恢复备份",
                                tint = TextSecondary
                            )
                        }
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
                                PresetCard(
                                    isApplying = isApplyingPreset,
                                    onApplyPreset = ::applyPreset
                                )
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
                                                        sessionStore.updateSession(sourceKey) { current ->
                                                            val currentValue = current.paramValues[param.key]
                                                                ?: return@updateSession current
                                                            current.copyForEditor(
                                                                paramValues = current.paramValues.toMutableMap().apply {
                                                                    put(
                                                                        param.key,
                                                                        currentValue.copy(
                                                                            value = newValue,
                                                                            isModified = newValue != currentValue.originalValue
                                                                        )
                                                                    )
                                                                }
                                                            )
                                                        }
                                                    }
                                                )
                                                if (param.key == FRAME_SIZE_MAX_KEY) {
                                                    DynamicResolutionRangeHint(paramValues)
                                                }
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
                Column {
                  if (liveSource?.accessMode == AccessMode.SAF) {
                      // SAF cannot detect whether the game is still running in the background.
                      Text(
                          text = "保存前请从最近任务中彻底关闭游戏，否则游戏退出时可能覆盖本次修改",
                          color = TextSecondary,
                          fontSize = 12.sp,
                          modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
                      )
                  }
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
    }

    gameRunningGateway?.let { gateway ->
        AlertDialog(
            onDismissRequest = { gameRunningGateway = null },
            title = { Text("游戏正在运行") },
            text = { Text("游戏退出时会写回自己的配置，可能覆盖本次修改。建议先关闭游戏再保存。") },
            confirmButton = {
                TextButton(onClick = {
                    gameRunningGateway = null
                    scope.launch { performSave(gateway, stopGameFirst = true) }
                }) { Text("关闭游戏并保存") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { gameRunningGateway = null }) { Text("取消") }
                    TextButton(onClick = {
                        gameRunningGateway = null
                        scope.launch { performSave(gateway, stopGameFirst = false) }
                    }) { Text("仍然保存") }
                }
            }
        )
    }

    backupNames?.let { names ->
        AlertDialog(
            onDismissRequest = { backupNames = null },
            title = { Text("恢复备份") },
            text = {
                Column {
                    Text(
                        text = "每次保存前都会自动备份，保留最近 ${ConfigBackups.KEEP_COUNT} 份。",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    names.forEach { name ->
                        TextButton(
                            onClick = {
                                backupNames = null
                                restoreCandidate = name
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = backupLabel(name), modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { backupNames = null }) { Text("关闭") }
            }
        )
    }

    val pendingRestore = restoreCandidate
    if (pendingRestore != null && liveSource != null) {
        AlertDialog(
            onDismissRequest = { restoreCandidate = null },
            title = { Text("恢复到这份备份？") },
            text = {
                Text("将恢复 ${backupLabel(pendingRestore)} 的配置，未保存的修改会丢失。恢复前的当前配置会再备份一份。")
            },
            confirmButton = {
                TextButton(onClick = {
                    restoreCandidate = null
                    restoreBackup(liveSource, pendingRestore)
                }) { Text("恢复") }
            },
            dismissButton = {
                TextButton(onClick = { restoreCandidate = null }) { Text("取消") }
            }
        )
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

private const val FRAME_SIZE_MIN_KEY = "Graphics/FrameSizeMinimumPercentage"
private const val FRAME_SIZE_MAX_KEY = "Graphics/FrameSizeMaximumPercentage"

/** Informational only: several bundled real presets intentionally use min > max. */
@Composable
private fun DynamicResolutionRangeHint(paramValues: Map<String, ParamValue>) {
    val min = paramValues[FRAME_SIZE_MIN_KEY]?.value?.toIntOrNull() ?: return
    val max = paramValues[FRAME_SIZE_MAX_KEY]?.value?.toIntOrNull() ?: return
    if (min <= max) return
    Text(
        text = "动态分辨率下限（$min%）高于上限（$max%）。部分真实预设也采用这种组合，效果以游戏实际表现为准。",
        color = AccentOrangeLight,
        fontSize = 12.sp,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
    )
}

private fun backupLabel(name: String): String =
    ConfigBackups.timestampOf(name)
        ?.let { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(it)) }
        ?: name

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
 * 画质预设卡片：数据来自 20 份内置真实 preferences
 */
@Composable
private fun PresetCard(
    isApplying: Boolean,
    onApplyPreset: (PresetMetadata) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedPresetId by rememberSaveable { mutableStateOf(PresetCatalog.all.first().id) }
    val selectedPreset = PresetCatalog.all.firstOrNull { it.id == selectedPresetId }
        ?: PresetCatalog.all.first()

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
                        text = "${selectedPreset.group.title} · ${selectedPreset.name}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.92f)
                ) {
                    PresetGroup.entries.forEach { group ->
                        Text(
                            text = group.title,
                            color = NeonCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                        PresetCatalog.all.filter { it.group == group }.forEach { preset ->
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
                                    selectedPresetId = preset.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = selectedPreset.description,
                color = TextSecondary,
                fontSize = 13.sp
            )
            Text(
                text = "数值来自真实 preferences 文件，只修改画质字段及 Graphics/Performance 档位。",
                color = TextDisabled,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onApplyPreset(selectedPreset) },
                enabled = !isApplying,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = DarkBackground
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isApplying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = DarkBackground,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "应用预设",
                        fontWeight = FontWeight.Bold
                    )
                }
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
