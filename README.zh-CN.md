# GRID Legends 画质工具

[English](README.en.md)

这是一个用于修改 GRID Legends 安卓版画质配置的 Android 工具，使用 Kotlin 和 Jetpack Compose 开发。

应用会读取并修改 GRID Legends 的 `preferences` 配置文件，并内置一组基于真实画质配置文件整理出来的画质预设。

## 功能

- 读取和写入 GRID Legends 的 `preferences` 配置文件。
- 支持游戏真实使用的 registry 风格 XML 结构。
- 能区分同名配置项所在的路径，避免误改 `GraphicsSettings`、`Graphics`、`Performance`、`Setup` 中的同名字段。
- 支持手动调整帧率、分辨率、画质、反射、后处理、HDR、SSR、体积光等项目。
- 内置画质预设：
  - 超低配 120 帧
  - 低配 720p 60 帧
  - 平衡画质
  - 中配专属
  - 高配专属
  - 高配 1080p 120 帧
  - 超极限 2K 120 帧
  - 超越极限 2K 120 HDR
- Android 11-15 默认使用 SAF 目录授权访问，不需要 Shizuku。
- 较新 Android 版本可使用 Shizuku 访问游戏目录。

## 目标游戏

- 包名：`com.feralinteractive.gridlegends_android`
- 配置文件路径：

```text
/sdcard/Android/data/com.feralinteractive.gridlegends_android/files/feral_app_support/preferences
```

## 使用说明

1. 安装并启动 GRID Legends 至少一次，让游戏生成配置文件。
2. 打开本工具并完成目录授权或 Shizuku 授权。
3. 进入画质编辑页。
4. 手动调整参数，或选择一个画质预设并点击“应用预设”。
5. 点击保存后重启游戏，让配置生效。

## 访问方式

- Android 11-15：使用系统文件选择器授权游戏目录。
- Android 16 及以上：使用 Shizuku 授权访问游戏目录。

如果部分 ROM 限制了 `Android/data` 目录选择，SAF 授权可能失败，这种情况下可以考虑使用 Shizuku。

## 构建

这是一个单模块 Android 项目，只使用 Gradle 构建。

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

Release 构建配置位于 `app/build.gradle.kts`。

## 注意事项

- 极限和 HDR 预设负载很高，低端设备可能无法稳定进入游戏。
- 保存前会自动创建备份文件。
- 应用会尽量保留未管理的 XML 字段，只替换当前工具管理的画质项。
- 本工具是非官方开源项目，与 Codemasters、EA、Feral Interactive 无从属或授权关系。
- GRID Legends、Codemasters、EA、Feral Interactive 等名称和商标归其各自权利方所有。
