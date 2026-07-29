# 预设导出 1.2 发布加固实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 保留 20 份权威预设原文件，同时修复配置日志泄露、系统保存器期间状态丢失、宽松 UTF-8 解码和首页滚动布局问题，并产出可发布的 1.2.0 APK。

**架构：** 使用 `rememberSaveable` 保存页面路由、编辑来源和一个可序列化的 `EditorSessionState`，让待导出字节及编辑快照在 Activity/进程重建后恢复；把状态转换封装为纯 Kotlin 数据模型并用 JVM 测试覆盖。预设仓库使用严格 UTF-8 decoder，并显式传播协程取消；预设资源本身保持逐字节不变。

**技术栈：** Kotlin、Jetpack Compose、Activity Result API、Java Serializable、Kotlin Coroutines、JUnit 4、Gradle Android Plugin。

---

## 全局约束

- 20 份 `app/src/main/assets/presets/*/preferences` 不得修改；未编辑导出必须与 asset 逐字节一致。
- 不清洗或重置任何 donor 字段；编辑导出只改 App 管理的画质参数。
- Logcat 不得包含 XML 正文、完整参数 Map、`EditorSource.toString()` 或安装分析 ID。
- 系统保存器建议文件名保持无扩展名 `preferences`，MIME 保持 `application/octet-stream`。
- Root、Shizuku、SAF 直接编辑流程及备份行为保持不变。
- UI 文案使用简体中文，颜色继续使用 `ui/theme/Color.kt` 常量。
- 版本保持 `versionCode = 7`、`versionName = "1.2.0"`。

## 文件结构

- 创建 `app/src/main/java/com/gridlegends/graphicsassistant/data/EditorSessionState.kt`：可保存的编辑会话及导出状态转换。
- 创建 `app/src/test/java/com/gridlegends/graphicsassistant/data/EditorSessionStateTest.kt`：序列化、待导出和成功快照回归测试。
- 创建 `app/src/test/java/com/gridlegends/graphicsassistant/data/PresetRepositoryTest.kt`：严格 UTF-8 解码回归测试。
- 修改 `app/src/main/java/com/gridlegends/graphicsassistant/data/EditorSource.kt`：可序列化来源及不含正文的日志摘要。
- 修改 `app/src/main/java/com/gridlegends/graphicsassistant/data/PresetModels.kt`、`GraphicsParam.kt`：让保存状态所需模型可序列化。
- 修改 `app/src/main/java/com/gridlegends/graphicsassistant/MainActivity.kt`：保存路由和编辑来源。
- 修改 `app/src/main/java/com/gridlegends/graphicsassistant/ui/screens/EditorScreen.kt`：接入可恢复会话、待导出内容和安全日志。
- 创建 `app/src/main/java/com/gridlegends/graphicsassistant/fileaccess/SafeCommandLog.kt`：只暴露执行元数据的命令日志摘要。
- 修改 `app/src/main/java/com/gridlegends/graphicsassistant/fileaccess/RootManager.kt`、`RootFileOperator.kt`、`ShizukuFileOperator.kt`：禁止命令、配置输出或 Base64 配置正文进入日志。
- 创建 `app/src/test/java/com/gridlegends/graphicsassistant/fileaccess/SafeCommandLogTest.kt`：命令日志脱敏回归测试。
- 修改 `app/src/main/java/com/gridlegends/graphicsassistant/data/PresetRepository.kt`：严格解码并传播取消。
- 修改 `app/src/main/java/com/gridlegends/graphicsassistant/ui/screens/HomeScreen.kt`：移除滚动列中的 weighted spacer。
- 修改 `app/src/main/java/com/gridlegends/graphicsassistant/ui/screens/PresetLibraryScreen.kt`、`README.zh-CN.md`、`README.en.md`：说明导出完整配置且保留其他字段。
- 修改 `app/build.gradle.kts`：仅增加 JVM 回归测试所需的 JUnit 依赖。

### 任务 1：建立可保存的编辑会话模型

**文件：**

- 修改：`app/build.gradle.kts`
- 修改：`app/src/main/java/com/gridlegends/graphicsassistant/data/EditorSource.kt`
- 修改：`app/src/main/java/com/gridlegends/graphicsassistant/data/PresetModels.kt`
- 修改：`app/src/main/java/com/gridlegends/graphicsassistant/data/GraphicsParam.kt`
- 创建：`app/src/main/java/com/gridlegends/graphicsassistant/data/EditorSessionState.kt`
- 创建：`app/src/test/java/com/gridlegends/graphicsassistant/data/EditorSessionStateTest.kt`

- [ ] **步骤 1：增加 JUnit 依赖并编写失败测试**

在 `dependencies` 中增加：

```kotlin
testImplementation("junit:junit:4.13.2")
```

测试必须先覆盖以下行为：

```kotlin
@Test
fun pendingExportAndBundledSourceSurviveJavaSerialization() {
    // 构造包含 baseBytes、rawValues、当前 ParamValue、pending bytes 和 snapshot 的来源/会话；
    // 用 ObjectOutputStream/ObjectInputStream 往返；
    // 断言预设 ID、字节、当前值、pending snapshot 和 lastExportedSnapshot 均保持。
}

@Test
fun successfulExportClearsPendingAndRecordsSnapshot() {
    // beginExport 后 completeExport；
    // 断言 pending bytes/snapshot 清空，lastExportedSnapshot 等于导出快照。
}

@Test
fun logSummaryNeverContainsEmbeddedConfigBody() {
    // LiveConfig 中放入带唯一 secret 的 XML；
    // 断言 logSummary 只包含模式、路径和长度，不包含 secret/XML。
}
```

- [ ] **步骤 2：运行测试验证红灯**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio1\jbr'
& 'C:\Program Files\Git\bin\bash.exe' ./gradlew testDebugUnitTest --tests '*EditorSessionStateTest'
```

预期：因为 `EditorSessionState`、`beginExport`、`completeExport` 或 `logSummary` 尚不存在而失败。

- [ ] **步骤 3：编写最少实现**

实现一个 `Serializable` 的不可变 `EditorSessionState`，至少包含：

```kotlin
data class EditorSessionState(
    val sourceKey: String,
    val rawConfigValues: Map<String, String> = emptyMap(),
    val paramValues: Map<String, ParamValue> = emptyMap(),
    val isInitialized: Boolean = false,
    val loadError: String? = null,
    val pendingExportBytes: ByteArray? = null,
    val pendingExportSnapshot: Map<String, String>? = null,
    val lastExportedSnapshot: Map<String, String> = emptyMap()
) : Serializable
```

提供纯函数式的 `beginExport(bytes, snapshot)`、`cancelExport()`、`completeExport()`，并让 `EditorSource`、`LoadedPreset`、`PresetMetadata`、`PresetSummary` 和 `ParamValue` 满足 Java 序列化。`EditorSource.logSummary()` 只能返回元数据和内容长度。

- [ ] **步骤 4：运行测试验证绿灯**

运行与步骤 2 相同的聚焦测试，预期全部通过且输出无错误。

- [ ] **步骤 5：提交**

```bash
git add app/build.gradle.kts app/src/main/java/com/gridlegends/graphicsassistant/data \
        app/src/test/java/com/gridlegends/graphicsassistant/data/EditorSessionStateTest.kt
git commit -m "fix: 保存预设编辑和导出会话"
```

### 任务 2：接入 Activity 重建恢复并移除配置正文日志

**文件：**

- 修改：`app/src/main/java/com/gridlegends/graphicsassistant/MainActivity.kt`
- 修改：`app/src/main/java/com/gridlegends/graphicsassistant/ui/screens/EditorScreen.kt`
- 创建：`app/src/main/java/com/gridlegends/graphicsassistant/fileaccess/SafeCommandLog.kt`
- 修改：`app/src/main/java/com/gridlegends/graphicsassistant/fileaccess/RootManager.kt`
- 修改：`app/src/main/java/com/gridlegends/graphicsassistant/fileaccess/RootFileOperator.kt`
- 修改：`app/src/main/java/com/gridlegends/graphicsassistant/fileaccess/ShizukuFileOperator.kt`
- 创建：`app/src/test/java/com/gridlegends/graphicsassistant/fileaccess/SafeCommandLogTest.kt`

- [ ] **步骤 1：用任务 1 的失败场景检查现有 UI 状态点**

确认现有代码仍有以下不符合项，作为本任务红灯证据记录到报告：

```powershell
rg -n "var currentScreen by remember|var editorSource by remember|pendingExportBytes by remember|source=\$source|out=\$\{|err=\$\{|exec: \$command|execFallback: \$command" app/src/main/java/com/gridlegends/graphicsassistant
```

预期：至少命中 `MainActivity.kt` 的普通 `remember`、`EditorScreen.kt` 的普通 pending state 或完整 source 日志，以及 Root/Shizuku 执行器记录的命令/输出片段。

- [ ] **步骤 2：保存页面路由和来源**

在 `MainActivity` 中使用 `rememberSaveable` 保存：

```kotlin
currentScreen
previousScreen
editorSource
```

保持现有 `EDITOR + null source` 自动返回和返回目标逻辑不变。

- [ ] **步骤 3：用 EditorSessionState 保存编辑与待导出状态**

`EditorScreen` 以稳定 `sourceKey` 创建 `rememberSaveable` 会话。加载成功、参数修改、恢复默认、应用预设、开始导出、取消导出、写入成功和写入失败都通过复制会话状态更新。系统保存器回调必须：

- URI 为空时清理 pending 状态且不提示错误；
- URI、bytes、snapshot 齐全时写入并在成功后调用 `completeExport()`；
- URI 非空但 pending 状态不完整时明确提示“导出状态已丢失，请重新导出”；
- 写入失败不更新 `lastExportedSnapshot`，允许再次点击导出。

- [ ] **步骤 4：替换不安全日志**

把：

```kotlin
Log.d("GLGraphics", "EditorScreen: 开始加载 source=$source")
```

替换为只记录 `source.logSummary()` 的日志；不得把完整来源对象、XML 或参数 Map 传给 Logcat。

先在 `SafeCommandLogTest` 中用包含唯一 Base64/XML sentinel 的命令、标准输出和错误输出调用以下待实现纯函数：

```kotlin
internal fun safeCommandSummary(
    mode: String,
    command: String,
    output: String,
    error: String,
    exitCode: Int
): String
```

断言结果遵循 `"ROOT exec commandLength=17 exit=0 outputLength=23 errorLength=0"` 这一精确结构（数字按测试数据的实际长度计算），且不包含任一 sentinel。先运行并确认因函数不存在而 RED，再实现只使用模式、退出码和三个长度的摘要。Root/Shizuku 执行日志必须调用它；不得记录原命令或输出片段。注意写配置命令本身包含整份配置的 Base64，因此仅移除 `cat` 输出不够。

- [ ] **步骤 5：运行聚焦测试和 Debug 编译**

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio1\jbr'
& 'C:\Program Files\Git\bin\bash.exe' ./gradlew testDebugUnitTest assembleDebug
rg -n "source=\$source|source\.toString\(\)|out=\$\{|err=\$\{|exec: \$command|execFallback: \$command" app/src/main/java/com/gridlegends/graphicsassistant
```

预期：单元测试和 Debug 构建成功；`rg` 无命中。

- [ ] **步骤 6：提交**

```bash
git add app/src/main/java/com/gridlegends/graphicsassistant/MainActivity.kt \
        app/src/main/java/com/gridlegends/graphicsassistant/ui/screens/EditorScreen.kt \
        app/src/main/java/com/gridlegends/graphicsassistant/fileaccess/SafeCommandLog.kt \
        app/src/main/java/com/gridlegends/graphicsassistant/fileaccess/RootManager.kt \
        app/src/main/java/com/gridlegends/graphicsassistant/fileaccess/RootFileOperator.kt \
        app/src/main/java/com/gridlegends/graphicsassistant/fileaccess/ShizukuFileOperator.kt \
        app/src/test/java/com/gridlegends/graphicsassistant/fileaccess/SafeCommandLogTest.kt
git commit -m "fix: 恢复系统保存器期间的编辑状态"
```

### 任务 3：严格解码、布局和完整配置提示

**文件：**

- 修改：`app/src/main/java/com/gridlegends/graphicsassistant/data/PresetRepository.kt`
- 创建：`app/src/test/java/com/gridlegends/graphicsassistant/data/PresetRepositoryTest.kt`
- 修改：`app/src/main/java/com/gridlegends/graphicsassistant/ui/screens/HomeScreen.kt`
- 修改：`app/src/main/java/com/gridlegends/graphicsassistant/ui/screens/PresetLibraryScreen.kt`
- 修改：`README.zh-CN.md`
- 修改：`README.en.md`

- [ ] **步骤 1：编写严格 UTF-8 的失败测试**

测试内部纯函数 `decodeUtf8Strict(bytes)`：

```kotlin
@Test
fun validUtf8IsDecodedExactly() {
    assertEquals("画质 preferences", decodeUtf8Strict("画质 preferences".toByteArray()))
}

@Test(expected = CharacterCodingException::class)
fun malformedUtf8IsRejected() {
    decodeUtf8Strict(byteArrayOf(0xC3.toByte(), 0x28))
}
```

- [ ] **步骤 2：运行测试验证红灯**

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio1\jbr'
& 'C:\Program Files\Git\bin\bash.exe' ./gradlew testDebugUnitTest --tests '*PresetRepositoryTest'
```

预期：`decodeUtf8Strict` 尚不存在，测试失败。

- [ ] **步骤 3：实现严格解码并传播取消**

使用：

```kotlin
Charsets.UTF_8.newDecoder()
    .onMalformedInput(CodingErrorAction.REPORT)
    .onUnmappableCharacter(CodingErrorAction.REPORT)
    .decode(ByteBuffer.wrap(bytes))
    .toString()
```

`PresetRepository.load()` 捕获普通异常并返回 `Result.failure`，但 `CancellationException` 必须原样抛出。

- [ ] **步骤 4：修复布局并增加透明提示**

- 将 `HomeScreen` 滚动 `Column` 中的 `Spacer(Modifier.weight(1f))` 改为固定高度间距；
- 在预设库说明和中英文 README 中明确：导出的是完整 `preferences`，会保留预设文件中的其他设置；
- 不修改任何 asset，不新增清洗选项。

- [ ] **步骤 5：运行测试和构建**

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio1\jbr'
& 'C:\Program Files\Git\bin\bash.exe' ./gradlew testDebugUnitTest assembleDebug
git diff --check
```

预期：单元测试和构建成功；`git diff --check` 无输出。

- [ ] **步骤 6：提交**

```bash
git add app/src/main/java/com/gridlegends/graphicsassistant/data/PresetRepository.kt \
        app/src/test/java/com/gridlegends/graphicsassistant/data/PresetRepositoryTest.kt \
        app/src/main/java/com/gridlegends/graphicsassistant/ui/screens/HomeScreen.kt \
        app/src/main/java/com/gridlegends/graphicsassistant/ui/screens/PresetLibraryScreen.kt \
        README.zh-CN.md README.en.md
git commit -m "fix: 加固预设读取和导出提示"
```

### 任务 4：发布前全量验证

**文件：**

- 验证：整个 `:app`、20 份预设、Debug/Release APK、Git 历史和 GitHub 发布状态。

- [ ] **步骤 1：运行完整 JVM 测试和干净构建**

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio1\jbr'
& 'C:\Program Files\Git\bin\bash.exe' ./gradlew clean testDebugUnitTest assembleDebug assembleRelease
```

预期：所有任务成功且退出码为 0。

- [ ] **步骤 2：验证资源、版本和签名**

- 20/20 asset 与 `D:\download\极限画质（高中低）` 的源文件 SHA-256 一致；
- Catalog 与 asset 目录 20/20 对应，分组仍为 7/3/5/5；
- Debug/Release APK 包名为 `com.gridlegends.graphicsassistant`；
- `versionCode=7`、`versionName=1.2.0`；
- Release APK 通过 `apksigner verify --verbose --print-certs`。

- [ ] **步骤 3：执行可用的设备或模拟器场景**

检查 `adb devices -l` 和本机 AVD。若存在可用目标，至少验证：预设库进入、选择预设、修改、取消保存器、无修改/修改导出、旋转后状态保留，以及保存器期间 Activity 重建。Root、Shizuku、SAF 或游戏未安装的场景必须如实记录，不能用静态检查冒充真机通过。

- [ ] **步骤 4：最终整分支审查**

以 `origin/main` 与当前 HEAD 的完整 diff 生成审查包，分派最终代码审查者。所有 Critical/Important 发现必须修复并重新审查。

- [ ] **步骤 5：推送和发布**

在确认远端 `main` 仍可从本分支快进后：

```bash
git push origin HEAD:main
git tag -a v1.2.0 -m "v1.2.0"
git push origin v1.2.0
gh release create v1.2.0 <release-apk> --title "v1.2.0" --notes-file <release-notes>
```

Release 上传经过签名验证的 APK，并在发布说明中列出无需授权的预设编辑导出、20 份内置配置、现有直接编辑模式保持不变，以及未能执行的设备专属场景（若有）。
