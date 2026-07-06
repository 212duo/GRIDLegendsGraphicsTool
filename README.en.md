# GRID Legends Graphics Tool

[![简体中文](https://img.shields.io/badge/%E7%AE%80%E4%BD%93%E4%B8%AD%E6%96%87-README-blue)](README.md)

An Android graphics configuration editor for GRID Legends, built with Kotlin and Jetpack Compose.

The app edits the GRID Legends Android `preferences` file and provides ready-to-use graphics presets derived from real configuration samples.

## Features

- Reads and writes the GRID Legends `preferences` file.
- Supports the actual registry-style XML structure used by the game.
- Handles duplicate setting names safely by writing values to the correct section path.
- Provides manual editing for frame rate, resolution, quality, reflections, post-processing, HDR, SSR, and volumetrics.
- Includes built-in presets:
  - Ultra Low 120 FPS
  - Low 720p 60 FPS
  - Balanced Quality
  - Mid-range
  - High-end
  - High-end 1080p 120 FPS
  - Extreme 4K 120 FPS
  - Extreme 4K 120 FPS HDR
- Supports Root, Shizuku, and SAF access modes.
- Android 11-15 uses SAF folder authorization by default; if the system picker blocks `Android/data`, you can switch to Shizuku or Root.
- Android 16 and above uses Shizuku by default to access the game directory.

## Target Game

- Package: `com.feralinteractive.gridlegends_android`
- Config file:

```text
/sdcard/Android/data/com.feralinteractive.gridlegends_android/files/feral_app_support/preferences
```

## Usage

1. Install and launch GRID Legends at least once so the game creates the config file.
2. Open this tool and grant folder, Shizuku, or Root access.
3. Enter the graphics editor.
4. Adjust parameters manually, or select a preset and tap Apply Preset.
5. Save changes and restart the game.

## Access Modes

- Root: if the device uses Magisk, KernelSU, APatch, or a similar root solution, choose Root access on the home screen.
- Android 11-15: use the system file picker by default; if it shows "Can't use this folder", switch to Shizuku or Root on the home screen.
- Android 16 and above: use Shizuku by default to access the game directory.

If a ROM blocks `Android/data` selection through SAF, authorization may fail. In that case, Shizuku or Root can be used as an alternative.

## Build

This is a single-module Android project. Use Gradle only.

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

The release build is configured in `app/build.gradle.kts`.

## Notes

- Extreme and HDR presets are heavy and may be unstable on low-end devices.
- A backup file is created before saving.
- The app preserves unmanaged XML values when writing changes.
- This is an unofficial open-source tool and is not affiliated with or endorsed by Codemasters, EA, or Feral Interactive.
- GRID Legends, Codemasters, EA, Feral Interactive, and related names or trademarks belong to their respective owners.

## Tech Stack

- Kotlin
- Jetpack Compose
- AndroidX DocumentFile
- Root shell
- Shizuku API
- XmlPullParser
