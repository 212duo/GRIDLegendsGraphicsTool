# 内置画质预设编辑与导出实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 在不访问游戏目录的情况下，让用户从 20 份内置真实画质配置中选择、修改并导出无扩展名的 `preferences` 文件。

**架构：** 20 份完整配置作为 assets 中的权威预设源，由 `PresetRepository` 读取和验证；编辑器通过 `EditorSource` 区分直接游戏配置与内置预设。未修改时逐字节导出原文件，修改后由 `ConfigExporter` 基于完整原文定点替换并重新解析复核，再通过系统单文件保存器写入用户选择的位置。

**技术栈：** Kotlin、Jetpack Compose Material 3、Android Activity Result API、Android assets、Storage Access Framework、现有 `ConfigParser`/`ConfigWriter`、Gradle Android 插件。

---

## 执行前置条件

当前主工作区存在大量用户已有的 staged/unstaged 改动，且本功能会接触其中的 `MainActivity.kt`、`HomeScreen.kt`、`EditorScreen.kt` 等文件。执行者必须先确保工作位于包含这些改动的、可恢复的独立工作树中。

- [ ] 运行 `git status --short`，记录当前状态。
- [ ] 调用 `using-git-worktrees` 技能建立隔离工作树；如果未提交改动无法安全带入隔离工作树，停止并向用户确认基线处理方式。
- [ ] 不得自动执行 `git stash`、`git reset`、`git checkout --`，也不得替用户提交已有改动。
- [ ] 在干净的功能工作树中使用 `codex/preset-export` 分支执行以下任务。

## 文件结构

### 新建文件

- `app/src/main/assets/presets/*/preferences`：20 份完整真实配置，资源目录名使用任务 1 的固定 ID，并保持源文件字节不变。
- `app/src/main/java/com/gridlegends/graphicsassistant/data/PresetModels.kt`：预设分组、元数据、摘要和已加载预设模型。
- `app/src/main/java/com/gridlegends/graphicsassistant/data/PresetCatalog.kt`：固定的 20 个预设、分组和展示顺序。
- `app/src/main/java/com/gridlegends/graphicsassistant/data/PresetRepository.kt`：读取 assets、解析 XML、验证全部可编辑字段。
- `app/src/main/java/com/gridlegends/graphicsassistant/data/EditorSource.kt`：区分直接配置和内置预设编辑会话。
- `app/src/main/java/com/gridlegends/graphicsassistant/data/ConfigExporter.kt`：构建、复核导出字节。
- `app/src/main/java/com/gridlegends/graphicsassistant/ui/screens/PresetLibraryScreen.kt`：分组展示 20 个预设并加载选中项。
- `app/src/main/java/com/gridlegends/graphicsassistant/ui/components/PresetSourceBanner.kt`：编辑页当前预设摘要。

### 修改文件

- `app/src/main/java/com/gridlegends/graphicsassistant/MainActivity.kt`：新增预设库路由和 `EditorSource` 会话状态。
- `app/src/main/java/com/gridlegends/graphicsassistant/ui/screens/HomeScreen.kt`：新增无需授权的主入口，并使首页可滚动。
- `app/src/main/java/com/gridlegends/graphicsassistant/ui/screens/EditorScreen.kt`：按来源加载、预设模式导出、返回确认和安装说明。
- `app/src/main/java/com/gridlegends/graphicsassistant/ui/screens/AboutScreen.kt`：说明离线预设导出能力。
- `README.md`、`README.zh-CN.md`、`README.en.md`：记录两种使用流程和手动安装路径。

不修改 `ConfigWriter.generateFreshConfig`；新功能明确不走从零生成分支。

### 任务 1：导入 20 份真实预设资源

**文件：**

- 创建：`app/src/main/assets/presets/*/preferences`
- 来源：`D:\download\极限画质（高中低）\*\preferences`

- [ ] **步骤 1：创建稳定的 ASCII 资源目录并逐字节复制文件**

在 PowerShell 中运行以下机械复制脚本。它只复制文件，不移动或删除源文件：

```powershell
$presetSourceRoot = (Resolve-Path -LiteralPath 'D:\download\极限画质（高中低）').Path
$taskWorkspace = (Get-Location).Path
$presetAssetRoot = Join-Path $taskWorkspace 'app\src\main\assets\presets'
New-Item -ItemType Directory -Force -Path $presetAssetRoot | Out-Null

$presetMappings = @(
    @{ Id = 'low_very_low'; Source = '极低专属' },
    @{ Id = 'low_very_low_120'; Source = '极低120帧' },
    @{ Id = 'low_ultra_low'; Source = '超低配专属' },
    @{ Id = 'low_ultra_low_120'; Source = '超低配120帧' },
    @{ Id = 'low_dedicated'; Source = '低配专属' },
    @{ Id = 'low_720p_60_no_crowds'; Source = '低720p+60帧（无观众）' },
    @{ Id = 'low_power_balanced'; Source = '平衡优化版（低功耗）' },
    @{ Id = 'mid_dedicated'; Source = '中配专属' },
    @{ Id = 'mid_extreme_balanced'; Source = '极致平衡（中配专属）' },
    @{ Id = 'mid_balanced'; Source = '平衡画质' },
    @{ Id = 'high_dedicated'; Source = '高配专属' },
    @{ Id = 'high_optimized_70'; Source = '极致优化（70帧）' },
    @{ Id = 'high_optimized_90'; Source = '极致优化（90帧）' },
    @{ Id = 'high_optimized_balanced'; Source = '极致优化平衡（高配）' },
    @{ Id = 'high_1080p_120'; Source = '高配专属1080p120帧' },
    @{ Id = 'extreme_2k_30'; Source = '超极限2k+30帧' },
    @{ Id = 'extreme_2k_120'; Source = '超极限画质2K+120帧' },
    @{ Id = 'extreme_final'; Source = '超越极限（最终版）' },
    @{ Id = 'extreme_final_120'; Source = '超越极限（最终版120帧）' },
    @{ Id = 'extreme_final_120_hdr'; Source = '超越极限最终版120帧HDR（仅限支持HDR使用）' }
)

foreach ($mapping in $presetMappings) {
    $destinationDirectory = Join-Path $presetAssetRoot $mapping.Id
    New-Item -ItemType Directory -Force -Path $destinationDirectory | Out-Null
    $sourceFile = Join-Path (Join-Path $presetSourceRoot $mapping.Source) 'preferences'
    $destinationFile = Join-Path $destinationDirectory 'preferences'
    Copy-Item -LiteralPath $sourceFile -Destination $destinationFile
}
```

- [ ] **步骤 2：验证数量和哈希完全一致**

```powershell
$hashMismatches = foreach ($mapping in $presetMappings) {
    $sourceFile = Join-Path (Join-Path $presetSourceRoot $mapping.Source) 'preferences'
    $assetFile = Join-Path (Join-Path $presetAssetRoot $mapping.Id) 'preferences'
    if ((Get-FileHash -Algorithm SHA256 -LiteralPath $sourceFile).Hash -ne
        (Get-FileHash -Algorithm SHA256 -LiteralPath $assetFile).Hash) {
        $mapping.Id
    }
}

$assetCount = (Get-ChildItem -LiteralPath $presetAssetRoot -Recurse -File -Filter preferences).Count
"assetCount=$assetCount hashMismatches=$($hashMismatches.Count)"
```

预期输出：`assetCount=20 hashMismatches=0`。

- [ ] **步骤 3：提交资源文件**

```bash
git add app/src/main/assets/presets
git commit -m "feat: 内置完整画质预设文件"
```

### 任务 2：建立预设目录与读取验证层

**文件：**

- 创建：`app/src/main/java/com/gridlegends/graphicsassistant/data/PresetModels.kt`
- 创建：`app/src/main/java/com/gridlegends/graphicsassistant/data/PresetCatalog.kt`
- 创建：`app/src/main/java/com/gridlegends/graphicsassistant/data/PresetRepository.kt`

- [ ] **步骤 1：定义职责单一的预设模型**

创建 `PresetModels.kt`：

```kotlin
package com.gridlegends.graphicsassistant.data

enum class PresetGroup(val title: String) {
    LOW("低配"),
    MID("中配"),
    HIGH("高配"),
    EXTREME("极限")
}

data class PresetMetadata(
    val id: String,
    val name: String,
    val group: PresetGroup,
    val description: String,
    val assetPath: String = "presets/$id/preferences"
)

data class PresetSummary(
    val frameRate: String,
    val renderHeight: String,
    val hdrEnabled: Boolean
)

data class LoadedPreset(
    val metadata: PresetMetadata,
    val baseBytes: ByteArray,
    val baseContent: String,
    val rawValues: Map<String, String>,
    val summary: PresetSummary
)
```

- [ ] **步骤 2：声明全部 20 个预设及固定顺序**

创建 `PresetCatalog.kt`：

```kotlin
package com.gridlegends.graphicsassistant.data

object PresetCatalog {
    val all: List<PresetMetadata> = listOf(
        preset("low_very_low", "极低专属", PresetGroup.LOW, "最低画质负载，适合性能有限的设备。"),
        preset("low_very_low_120", "极低120帧", PresetGroup.LOW, "极低画质与高帧率组合。"),
        preset("low_ultra_low", "超低配专属", PresetGroup.LOW, "优先降低发热和画面负载。"),
        preset("low_ultra_low_120", "超低配120帧", PresetGroup.LOW, "超低画质并提高目标帧率。"),
        preset("low_dedicated", "低配专属", PresetGroup.LOW, "适合低配设备的常规方案。"),
        preset("low_720p_60_no_crowds", "低720p+60帧（无观众）", PresetGroup.LOW, "720p、60 帧并关闭观众。"),
        preset("low_power_balanced", "平衡优化版（低功耗）", PresetGroup.LOW, "兼顾可玩性、功耗和温度。"),
        preset("mid_dedicated", "中配专属", PresetGroup.MID, "适合中端设备的基础方案。"),
        preset("mid_extreme_balanced", "极致平衡（中配专属）", PresetGroup.MID, "在中端设备上平衡清晰度和流畅度。"),
        preset("mid_balanced", "平衡画质", PresetGroup.MID, "画质与性能相对均衡。"),
        preset("high_dedicated", "高配专属", PresetGroup.HIGH, "适合高性能设备的常规高画质。"),
        preset("high_optimized_70", "极致优化（70帧）", PresetGroup.HIGH, "以 70 帧为目标的高配优化。"),
        preset("high_optimized_90", "极致优化（90帧）", PresetGroup.HIGH, "以 90 帧为目标的高配优化。"),
        preset("high_optimized_balanced", "极致优化平衡（高配）", PresetGroup.HIGH, "高配设备的综合平衡方案。"),
        preset("high_1080p_120", "高配专属1080p120帧", PresetGroup.HIGH, "1080p 与 120 帧的高负载组合。"),
        preset("extreme_2k_30", "超极限2k+30帧", PresetGroup.EXTREME, "以高分辨率换取较低目标帧率。"),
        preset("extreme_2k_120", "超极限画质2K+120帧", PresetGroup.EXTREME, "高分辨率与 120 帧的极高负载方案。"),
        preset("extreme_final", "超越极限（最终版）", PresetGroup.EXTREME, "完整极限画质配置。"),
        preset("extreme_final_120", "超越极限（最终版120帧）", PresetGroup.EXTREME, "极限画质并提高到 120 帧。"),
        preset(
            "extreme_final_120_hdr",
            "超越极限最终版120帧HDR（仅限支持HDR使用）",
            PresetGroup.EXTREME,
            "仅限支持 HDR 的高性能设备。"
        )
    )

    init {
        check(all.size == 20) { "内置预设数量必须为 20" }
        check(all.map { it.id }.distinct().size == all.size) { "内置预设 ID 必须唯一" }
        check(all.groupingBy { it.group }.eachCount() == mapOf(
            PresetGroup.LOW to 7,
            PresetGroup.MID to 3,
            PresetGroup.HIGH to 5,
            PresetGroup.EXTREME to 5
        )) { "内置预设分组数量不正确" }
    }

    private fun preset(
        id: String,
        name: String,
        group: PresetGroup,
        description: String
    ) = PresetMetadata(id, name, group, description)
}
```

- [ ] **步骤 3：实现 assets 读取、解析与字段完整性验证**

创建 `PresetRepository.kt`：

```kotlin
package com.gridlegends.graphicsassistant.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PresetRepository(private val context: Context) {

    suspend fun load(metadata: PresetMetadata): Result<LoadedPreset> =
        withContext(Dispatchers.IO) {
            runCatching {
                val bytes = context.assets.open(metadata.assetPath).use { it.readBytes() }
                val content = bytes.toString(Charsets.UTF_8)
                val rawValues = ConfigParser.parse(bytes.inputStream())
                val missingKeys = ParamDefinitions.allParams
                    .map { it.key }
                    .filterNot(rawValues::containsKey)

                check(missingKeys.isEmpty()) {
                    "缺少可编辑字段：${missingKeys.joinToString()}"
                }

                LoadedPreset(
                    metadata = metadata,
                    baseBytes = bytes,
                    baseContent = content,
                    rawValues = rawValues,
                    summary = PresetSummary(
                        frameRate = requireNotNull(rawValues["FramerateLimit"]) {
                            "缺少 FramerateLimit"
                        },
                        renderHeight = requireNotNull(rawValues["Setup/FixedScreenHeight"]) {
                            "缺少 Setup/FixedScreenHeight"
                        },
                        hdrEnabled = rawValues["HDR"] == "1"
                    )
                )
            }
        }
}
```

- [ ] **步骤 4：运行构建检查模型和协程 API**

运行：

```bash
./gradlew assembleDebug
```

预期：`BUILD SUCCESSFUL`。此时新代码尚未接入 UI，但必须独立编译。

- [ ] **步骤 5：提交目录与读取层**

```bash
git add app/src/main/java/com/gridlegends/graphicsassistant/data/PresetModels.kt \
        app/src/main/java/com/gridlegends/graphicsassistant/data/PresetCatalog.kt \
        app/src/main/java/com/gridlegends/graphicsassistant/data/PresetRepository.kt
git commit -m "feat: 添加内置预设目录和读取验证"
```

### 任务 3：实现编辑来源模型和安全导出器

**文件：**

- 创建：`app/src/main/java/com/gridlegends/graphicsassistant/data/EditorSource.kt`
- 创建：`app/src/main/java/com/gridlegends/graphicsassistant/data/ConfigExporter.kt`

- [ ] **步骤 1：用密封类型表达编辑来源**

创建 `EditorSource.kt`：

```kotlin
package com.gridlegends.graphicsassistant.data

import com.gridlegends.graphicsassistant.fileaccess.AccessMode

sealed interface EditorSource {
    data class LiveConfig(
        val accessMode: AccessMode,
        val configPath: String?,
        val configContent: String?
    ) : EditorSource

    data class BundledPreset(
        val preset: LoadedPreset
    ) : EditorSource
}
```

- [ ] **步骤 2：实现“原样导出或替换后复核”**

创建 `ConfigExporter.kt`：

```kotlin
package com.gridlegends.graphicsassistant.data

import java.io.ByteArrayOutputStream

object ConfigExporter {

    fun build(
        preset: LoadedPreset,
        paramValues: Map<String, ParamValue>
    ): Result<ByteArray> = runCatching {
        val modifiedValues = paramValues.values.filter { it.isModified }
        if (modifiedValues.isEmpty()) {
            return@runCatching preset.baseBytes.copyOf()
        }

        val output = ByteArrayOutputStream()
        ConfigWriter.write(
            output = output,
            paramValues = paramValues.values.toList(),
            existingContent = preset.baseContent
        )
        val bytes = output.toByteArray()
        val reparsedValues = ConfigParser.parse(bytes.inputStream())
        val failedKeys = modifiedValues.filter { modified ->
            reparsedValues[modified.key] != modified.value
        }.map { it.key }

        check(failedKeys.isEmpty()) {
            "以下字段未成功写入：${failedKeys.joinToString()}"
        }
        bytes
    }
}
```

- [ ] **步骤 3：运行编译检查**

```bash
./gradlew assembleDebug
```

预期：`BUILD SUCCESSFUL`。

- [ ] **步骤 4：提交来源模型和导出器**

```bash
git add app/src/main/java/com/gridlegends/graphicsassistant/data/EditorSource.kt \
        app/src/main/java/com/gridlegends/graphicsassistant/data/ConfigExporter.kt
git commit -m "feat: 添加预设配置安全导出器"
```

### 任务 4：构建分组预设库页面

**文件：**

- 创建：`app/src/main/java/com/gridlegends/graphicsassistant/ui/screens/PresetLibraryScreen.kt`

- [ ] **步骤 1：实现加载状态、分组列表和错误状态**

创建 `PresetLibraryScreen.kt`，文件内容如下：

```kotlin
package com.gridlegends.graphicsassistant.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gridlegends.graphicsassistant.data.*
import com.gridlegends.graphicsassistant.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetLibraryScreen(
    onBack: () -> Unit,
    onPresetSelected: (LoadedPreset) -> Unit
) {
    val context = LocalContext.current
    val repository = remember(context) { PresetRepository(context) }
    var loadedPresets by remember { mutableStateOf<Map<String, LoadedPreset>>(emptyMap()) }
    var loadErrors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val loaded = mutableMapOf<String, LoadedPreset>()
        val errors = mutableMapOf<String, String>()
        PresetCatalog.all.forEach { metadata ->
            repository.load(metadata).fold(
                onSuccess = { loaded[metadata.id] = it },
                onFailure = { errors[metadata.id] = it.message ?: "无法读取预设" }
            )
        }
        loadedPresets = loaded
        loadErrors = errors
        isLoading = false
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("选择画质预设") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "无需授权游戏目录。选择一个真实配置作为基础，修改后导出 preferences。",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(4.dp)
                )
            }

            if (isLoading) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = NeonCyan
                    )
                }
            }

            PresetGroup.entries.forEach { group ->
                item(key = "group_${group.name}") {
                    Text(
                        text = group.title,
                        color = NeonCyan,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                    )
                }
                items(
                    items = PresetCatalog.all.filter { it.group == group },
                    key = { it.id }
                ) { metadata ->
                    PresetLibraryCard(
                        metadata = metadata,
                        loadedPreset = loadedPresets[metadata.id],
                        error = loadErrors[metadata.id],
                        onSelect = onPresetSelected
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetLibraryCard(
    metadata: PresetMetadata,
    loadedPreset: LoadedPreset?,
    error: String?,
    onSelect: (LoadedPreset) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(metadata.name, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(metadata.description, color = TextSecondary, fontSize = 13.sp)

            loadedPreset?.summary?.let { summary ->
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PresetTag("${summary.renderHeight}p")
                    PresetTag("${summary.frameRate} 帧")
                    if (summary.hdrEnabled) PresetTag("HDR", AccentOrange)
                }
            }

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text("预设不可用：$it", color = WarningRed, fontSize = 12.sp)
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { loadedPreset?.let(onSelect) },
                enabled = loadedPreset != null,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = DarkBackground
                )
            ) {
                Text("基于此预设编辑", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PresetTag(text: String, color: Color = NeonCyan) {
    Surface(color = color.copy(alpha = 0.14f), shape = RoundedCornerShape(6.dp)) {
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
```

- [ ] **步骤 2：运行构建检查 Compose API 和资源加载代码**

```bash
./gradlew assembleDebug
```

预期：`BUILD SUCCESSFUL`。

- [ ] **步骤 3：提交预设库页面**

```bash
git add app/src/main/java/com/gridlegends/graphicsassistant/ui/screens/PresetLibraryScreen.kt
git commit -m "feat: 添加分组画质预设库页面"
```

### 任务 5：让编辑器支持内置预设导出模式

**文件：**

- 创建：`app/src/main/java/com/gridlegends/graphicsassistant/ui/components/PresetSourceBanner.kt`
- 修改：`app/src/main/java/com/gridlegends/graphicsassistant/ui/screens/EditorScreen.kt:41-350`
- 修改：`app/src/main/java/com/gridlegends/graphicsassistant/MainActivity.kt:32-78`

- [ ] **步骤 1：添加当前预设摘要组件**

创建 `PresetSourceBanner.kt`：

```kotlin
package com.gridlegends.graphicsassistant.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gridlegends.graphicsassistant.data.LoadedPreset
import com.gridlegends.graphicsassistant.ui.theme.*

@Composable
fun PresetSourceBanner(preset: LoadedPreset) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("当前基础预设", color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text(preset.metadata.name, color = NeonCyan, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "${preset.summary.renderHeight}p · ${preset.summary.frameRate} 帧" +
                    if (preset.summary.hdrEnabled) " · HDR" else "",
                color = TextPrimary,
                fontSize = 13.sp
            )
        }
    }
}
```

- [ ] **步骤 2：把编辑器参数改为单一 `EditorSource`**

将 `EditorScreen` 签名改为：

```kotlin
fun EditorScreen(
    context: Activity,
    source: EditorSource,
    onBack: () -> Unit,
    onAbout: () -> Unit
)
```

在状态区增加：

```kotlin
val bundledPreset = (source as? EditorSource.BundledPreset)?.preset
var pendingExportBytes by remember { mutableStateOf<ByteArray?>(null) }
var pendingExportSnapshot by remember { mutableStateOf<Map<String, String>?>(null) }
var lastExportedSnapshot by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
var showDiscardDialog by remember { mutableStateOf(false) }
var showInstallDialog by remember { mutableStateOf(false) }
```

将加载逻辑改为对来源进行穷举：

```kotlin
LaunchedEffect(source) {
    isLoading = true
    loadError = null
    rawConfigValues = emptyMap()
    paramValues = emptyMap()
    try {
        val rawValues = when (val currentSource = source) {
            is EditorSource.BundledPreset -> currentSource.preset.rawValues
            is EditorSource.LiveConfig -> if (currentSource.configContent != null) {
                ConfigParser.parse(currentSource.configContent.byteInputStream())
            } else {
                when (currentSource.accessMode) {
                    AccessMode.ROOT -> currentSource.configPath?.let(rootOp::readConfigFromPath)
                        ?: rootOp.readConfig()
                    AccessMode.SHIZUKU -> currentSource.configPath?.let(shizukuOp::readConfigFromPath)
                        ?: shizukuOp.readConfig()
                    AccessMode.SAF -> safOp.readConfig()
                } ?: error("未找到配置文件")
            }
        }
        val parsedValues = ConfigParser.toParamValues(rawValues).associateBy { it.key }
        rawConfigValues = rawValues
        paramValues = parsedValues
        lastExportedSnapshot = parsedValues.mapValues { it.value.value }
    } catch (e: Exception) {
        loadError = "读取配置失败：${e.message}"
    } finally {
        isLoading = false
    }
}
```

计算未导出状态：

```kotlin
val currentSnapshot = paramValues.mapValues { it.value.value }
val hasModifications = paramValues.values.any { it.isModified }
val hasUnexportedChanges = bundledPreset != null &&
    lastExportedSnapshot.isNotEmpty() &&
    currentSnapshot != lastExportedSnapshot
```

- [ ] **步骤 3：接入系统单文件保存器**

在 `EditorScreen` 的 UI 构建前增加 launcher：

```kotlin
val exportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/octet-stream")
) { uri ->
    val bytes = pendingExportBytes
    val snapshot = pendingExportSnapshot
    pendingExportBytes = null
    pendingExportSnapshot = null

    if (uri != null && bytes != null && snapshot != null) {
        try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                output.write(bytes)
                output.flush()
            } ?: error("无法打开目标文件")
            lastExportedSnapshot = snapshot
            showInstallDialog = true
        } catch (e: Exception) {
            Toast.makeText(context, "导出失败：${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
```

补充 import：

```kotlin
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
```

- [ ] **步骤 4：区分顶部标题、预设卡和底部操作**

顶部标题使用：

```kotlin
title = { Text(if (bundledPreset != null) "制作画质文件" else "画质编辑") }
```

导航按钮统一调用：

```kotlin
fun requestBack() {
    if (hasUnexportedChanges) showDiscardDialog = true else onBack()
}

BackHandler { requestBack() }
```

在列表顶部根据来源二选一：

```kotlin
if (bundledPreset != null) {
    item(key = "preset_source") {
        PresetSourceBanner(bundledPreset)
    }
} else {
    item(key = "graphics_presets") {
        PresetCard(onApplyPreset = ::applyPreset)
    }
}
```

现有“关于本工具”列表项只在直接游戏配置模式显示，防止预设编辑状态因离开编辑器组合树而丢失：

```kotlin
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
            Text("关于本工具", color = TextSecondary, fontSize = 14.sp)
        }
    }
}
```

将现有匿名 `onApplyPreset` 内容提取为以下局部函数：

```kotlin
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
    paramValues = next
}
```

底部按钮显示条件改为：

```kotlin
val showBottomAction = when {
    isLoading || loadError != null -> false
    bundledPreset != null -> true
    else -> hasModifications
}
```

点击行为按来源分支：

```kotlin
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
            paramValues = paramValues.mapValues { (_, value) ->
                value.copy(originalValue = value.value, isModified = false)
            }
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
    val preset = bundledPreset
    if (preset != null) {
        isSaving = true
        ConfigExporter.build(preset, paramValues).fold(
            onSuccess = { bytes ->
                pendingExportBytes = bytes
                pendingExportSnapshot = currentSnapshot
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
    } else {
        saveLiveConfig(source as EditorSource.LiveConfig)
    }
}
```

底部 `Button` 使用 `onClick = ::runBottomAction`。

按钮文案使用：

```kotlin
Text(if (bundledPreset != null) "导出 preferences" else "保存设置")
```

- [ ] **步骤 5：增加放弃修改和安装说明对话框**

在 `EditorScreen` 的根 `Box` 之后增加：

```kotlin
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
```

- [ ] **步骤 6：先让现有直接编辑路由适配 `EditorSource.LiveConfig`**

在 `MainActivity.kt` 用单一状态替换三个分散状态：

```kotlin
var editorSource by remember { mutableStateOf<EditorSource?>(null) }
```

现有 `onAuthorized` 改为：

```kotlin
onAuthorized = { path, content, accessMode ->
    editorSource = EditorSource.LiveConfig(accessMode, path, content)
    currentScreen = Screen.EDITOR
}
```

编辑器路由改为：

```kotlin
Screen.EDITOR -> editorSource?.let { source ->
    EditorScreen(
        context = this@MainActivity,
        source = source,
        onBack = { navigateBack() },
        onAbout = { navigateToAbout() }
    )
}
```

将 `MainActivity` 的全局返回处理限制为非编辑页，避免绕过编辑器的放弃确认：

```kotlin
BackHandler(enabled = currentScreen != Screen.HOME && currentScreen != Screen.EDITOR) {
    navigateBack()
}
```

- [ ] **步骤 7：运行 Debug 构建并回归直接编辑入口**

```bash
./gradlew assembleDebug
```

预期：`BUILD SUCCESSFUL`。人工打开一次现有 SAF、Shizuku 或 Root 中可用的任一路径，确认仍能进入编辑器；未修改时不显示“保存设置”，修改后显示并能走原有保存分支。

- [ ] **步骤 8：提交来源感知编辑器**

```bash
git add app/src/main/java/com/gridlegends/graphicsassistant/ui/components/PresetSourceBanner.kt \
        app/src/main/java/com/gridlegends/graphicsassistant/ui/screens/EditorScreen.kt \
        app/src/main/java/com/gridlegends/graphicsassistant/MainActivity.kt
git commit -m "feat: 支持基于内置预设导出配置"
```

### 任务 6：接通首页、预设库和编辑器路由

**文件：**

- 修改：`app/src/main/java/com/gridlegends/graphicsassistant/ui/screens/HomeScreen.kt:46-170`
- 修改：`app/src/main/java/com/gridlegends/graphicsassistant/MainActivity.kt:21-85`

- [ ] **步骤 1：在首页增加不受游戏状态限制的主要入口**

给 `HomeScreen` 增加回调：

```kotlin
fun HomeScreen(
    context: Activity,
    onAuthorized: (configPath: String?, configContent: String?, accessMode: AccessMode) -> Unit,
    onPresetExport: () -> Unit,
    onAbout: () -> Unit
)
```

在标题下、游戏安装状态卡之前插入：

```kotlin
Spacer(modifier = Modifier.height(24.dp))
Button(
    onClick = onPresetExport,
    modifier = Modifier
        .fillMaxWidth()
        .height(56.dp),
    colors = ButtonDefaults.buttonColors(
        containerColor = NeonCyan,
        contentColor = DarkBackground
    ),
    shape = RoundedCornerShape(12.dp)
) {
    Text("从预设制作画质文件", fontSize = 16.sp, fontWeight = FontWeight.Bold)
}
Spacer(modifier = Modifier.height(8.dp))
Text(
    text = "无需授权游戏目录，制作后导出到下载目录。",
    color = TextSecondary,
    fontSize = 13.sp,
    textAlign = TextAlign.Center
)
Spacer(modifier = Modifier.height(32.dp))
```

不要用 `isGameInstalled`、`safAuthorized`、`shizukuStatus` 或 `rootStatus` 控制该按钮的 `enabled`。

- [ ] **步骤 2：避免新增入口导致小屏首页溢出**

给首页主 `Column` 增加滚动：

```kotlin
modifier = Modifier
    .fillMaxSize()
    .verticalScroll(rememberScrollState())
    .padding(24.dp),
verticalArrangement = Arrangement.Top
```

补充 import：

```kotlin
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
```

- [ ] **步骤 3：新增 `PRESET_LIBRARY` 路由并连通会话**

在 `MainActivity.kt`：

```kotlin
private enum class Screen {
    HOME, PRESET_LIBRARY, EDITOR, ABOUT
}
```

首页回调：

```kotlin
onPresetExport = { currentScreen = Screen.PRESET_LIBRARY }
```

预设库路由：

```kotlin
Screen.PRESET_LIBRARY -> PresetLibraryScreen(
    onBack = { currentScreen = Screen.HOME },
    onPresetSelected = { preset ->
        editorSource = EditorSource.BundledPreset(preset)
        currentScreen = Screen.EDITOR
    }
)
```

更新返回逻辑：

```kotlin
fun navigateBack() {
    currentScreen = when (currentScreen) {
        Screen.EDITOR -> if (editorSource is EditorSource.BundledPreset) {
            Screen.PRESET_LIBRARY
        } else {
            Screen.HOME
        }
        Screen.PRESET_LIBRARY -> Screen.HOME
        Screen.ABOUT -> previousScreen
        Screen.HOME -> Screen.HOME
    }
}
```

新增 import：

```kotlin
import com.gridlegends.graphicsassistant.data.EditorSource
import com.gridlegends.graphicsassistant.ui.screens.PresetLibraryScreen
```

- [ ] **步骤 4：运行构建并人工验证无需授权的主路径**

```bash
./gradlew assembleDebug
```

预期：`BUILD SUCCESSFUL`。

人工验证：清除 App 数据或使用未授权设备状态，打开 App，确认“从预设制作画质文件”始终可用；进入后四组数量分别为 7、3、5、5；任选一个预设能进入“制作画质文件”页面。

- [ ] **步骤 5：提交完整导航链路**

```bash
git add app/src/main/java/com/gridlegends/graphicsassistant/MainActivity.kt \
        app/src/main/java/com/gridlegends/graphicsassistant/ui/screens/HomeScreen.kt
git commit -m "feat: 接通无需授权的预设制作流程"
```

### 任务 7：更新产品说明和风险提示

**文件：**

- 修改：`app/src/main/java/com/gridlegends/graphicsassistant/ui/screens/AboutScreen.kt:102-176`
- 修改：`README.md`
- 修改：`README.zh-CN.md`
- 修改：`README.en.md`

- [ ] **步骤 1：在关于页补充导出能力**

将副标题改为：

```kotlin
text = "用于制作、导出和直接修改 GRID Legends 安卓版画质配置"
```

在“主要功能”中增加：

```kotlin
FeatureItem("离线导出", "无需游戏目录权限，可从 20 份真实预设制作并导出 preferences")
```

在注意事项增加：

```kotlin
NoticeItem("手动替换 preferences 前请关闭游戏并备份原文件。")
```

- [ ] **步骤 2：在中英文 README 中记录两条使用路线**

中文 README 明确写出：

```markdown
### 无需授权：制作并导出配置

1. 在首页选择“从预设制作画质文件”。
2. 从低配、中配、高配、极限四组共 20 份真实配置中选择一个预设。
3. 修改需要的参数，或不修改直接导出。
4. 将导出的无扩展名 `preferences` 手动复制到：
   `/sdcard/Android/data/com.feralinteractive.gridlegends_android/files/feral_app_support/preferences`

替换前请关闭游戏并备份原文件。
```

`README.en.md` 增加语义一致的英文段落，并保留现有 Root、Shizuku、SAF 直接编辑说明。

- [ ] **步骤 3：运行构建并检查 Markdown 格式**

```bash
./gradlew assembleDebug
git diff --check
```

预期：构建输出 `BUILD SUCCESSFUL`，`git diff --check` 无输出且退出码为 0。

- [ ] **步骤 4：提交文档和关于页**

```bash
git add app/src/main/java/com/gridlegends/graphicsassistant/ui/screens/AboutScreen.kt \
        README.md README.zh-CN.md README.en.md
git commit -m "docs: 补充画质文件导出说明"
```

### 任务 8：执行完整验证与收尾检查

**文件：**

- 验证：`app/src/main/assets/presets/*/preferences`
- 验证：整个 `:app` Debug 构建和人工流程

- [ ] **步骤 1：重新验证 20 份 asset 与源文件哈希**

重新运行任务 1 的哈希脚本。

预期：`assetCount=20 hashMismatches=0`。

- [ ] **步骤 2：验证目录声明和资源目录一一对应**

运行：

```powershell
$catalog = Get-Content -Raw -Encoding UTF8 \
    'app\src\main\java\com\gridlegends\graphicsassistant\data\PresetCatalog.kt'
$catalogIds = [regex]::Matches($catalog, 'preset\(\s*"([^"]+)"') |
    ForEach-Object { $_.Groups[1].Value } |
    Sort-Object -Unique
$assetIds = Get-ChildItem -LiteralPath 'app\src\main\assets\presets' -Directory |
    Select-Object -ExpandProperty Name |
    Sort-Object -Unique
$differences = Compare-Object $catalogIds $assetIds
"catalog=$($catalogIds.Count) assets=$($assetIds.Count) differences=$($differences.Count)"
```

预期：`catalog=20 assets=20 differences=0`。

- [ ] **步骤 3：执行干净的 Debug 构建**

```bash
./gradlew clean assembleDebug
```

预期：`BUILD SUCCESSFUL`。仓库没有自动测试、lint 或 typecheck 命令，不添加不存在的验证步骤。

- [ ] **步骤 4：执行预设导出人工验收**

逐项记录结果：

1. 未安装游戏、未授权 SAF、无 Shizuku、无 Root 时仍可进入预设库；
2. 四组数量为 7、3、5、5，合计 20；
3. 每个预设均能进入编辑器，显示的帧率、分辨率、HDR 与文件内容一致；
4. 不修改直接导出，导出文件 SHA-256 与对应 asset 一致；
5. 修改帧率、分辨率和一个开关后导出，重新进入 App 的直接解析流程或用现有解析器确认三个目标值；
6. 比较修改前后 XML，除目标字段外，其余内容不变；
7. 系统建议文件名为 `preferences`，实际文件没有 `.xml` 后缀；
8. 取消系统保存器后编辑内容仍保留；
9. 修改后返回会弹出放弃确认，成功导出后立即返回不再提示未导出修改；
10. 导出成功对话框包含关闭游戏、备份和准确目标路径；
11. 现有 SAF、Shizuku、Root 中至少各执行一次可用环境下的进入/保存回归；没有对应环境时明确记录为未验证，不宣称通过。

- [ ] **步骤 5：检查最终差异和提交历史**

```bash
git diff --check
git status --short
git log --oneline --decorate -8
```

预期：`git diff --check` 无输出；计划内文件没有未提交改动；提交按任务 1–7 分布且不包含用户无关文件。

- [ ] **步骤 6：仅在验证中产生修复时提交**

如果步骤 1–5 发现并修复问题，只暂存本计划允许修改的文件后提交：

```bash
git add app/src/main/assets/presets \
        app/src/main/java/com/gridlegends/graphicsassistant/data/PresetModels.kt \
        app/src/main/java/com/gridlegends/graphicsassistant/data/PresetCatalog.kt \
        app/src/main/java/com/gridlegends/graphicsassistant/data/PresetRepository.kt \
        app/src/main/java/com/gridlegends/graphicsassistant/data/EditorSource.kt \
        app/src/main/java/com/gridlegends/graphicsassistant/data/ConfigExporter.kt \
        app/src/main/java/com/gridlegends/graphicsassistant/ui/screens/PresetLibraryScreen.kt \
        app/src/main/java/com/gridlegends/graphicsassistant/ui/components/PresetSourceBanner.kt \
        app/src/main/java/com/gridlegends/graphicsassistant/MainActivity.kt \
        app/src/main/java/com/gridlegends/graphicsassistant/ui/screens/HomeScreen.kt \
        app/src/main/java/com/gridlegends/graphicsassistant/ui/screens/EditorScreen.kt \
        app/src/main/java/com/gridlegends/graphicsassistant/ui/screens/AboutScreen.kt \
        README.md README.zh-CN.md README.en.md
git commit -m "fix: 修正预设导出验收问题"
```

如果没有产生修复，不创建空提交。
