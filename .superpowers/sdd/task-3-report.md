# 任务 3 报告：生成式预设映射

## 变更

- 在 `GraphicsOptionsTest` 增加生成式预设回归测试，逐个验证全部 9 个新增键：
  - 六个顶层画质整数等于同名 `Graphics/...` 三元组的第一个分量；
  - `VerticalSync` 与 `ReduceVisibleRain` 均为 `0`；
  - 不含 `Graphics/MetalFX`。
- `GraphicsPresets.preset` 现从已构造的 `Graphics/...` 值派生六个顶层整数，保留 `Graphics/OptimisationResolutionMultiplier`，并移除 `Graphics/MetalFX`。

## TDD 证据

### RED

新增测试后，使用 Android Studio JBR 和 Git Bash 执行：

```bash
./gradlew testDebugUnitTest --tests com.gridlegends.graphicsassistant.data.GraphicsOptionsTest
```

结果：失败，`GraphicsOptionsTest > generatedPresetsSynchronizeRecommendedGraphicsOptions` 在 `GraphicsOptionsTest.kt:64` 报 `AssertionError`；共 4 个测试、1 个失败。失败符合预期：基线预设尚未生成顶层新增键，且仍包含 `Graphics/MetalFX`。

### GREEN

实施最小映射修正后重新执行同一聚焦命令：`BUILD SUCCESSFUL in 3s`。

再执行完整单测：

```bash
./gradlew testDebugUnitTest
```

结果：`BUILD SUCCESSFUL in 2s`，24 个任务中 1 个执行、23 个最新。

## 自审

- 六个顶层整数均由对应 `Graphics/...` 字符串的首个逗号分量派生，避免双份等级值漂移。
- 两个新开关固定写为关闭值 `0`。
- `Graphics/MetalFX` 仅从生成式预设映射删除；未修改解析器/写入器对既有文件中该非托管键的保留行为。
- 未修改参数定义、版本或文档。
- `git diff --check` 无错误。

## 疑虑

- 无阻塞疑虑。任务简报所列的键和值映射均由自动化测试覆盖。
