# GRID Legends Graphics Tool

[![简体中文](https://img.shields.io/badge/%E7%AE%80%E4%BD%93%E4%B8%AD%E6%96%87-README-blue)](README.md)

An Android tool for creating, exporting, and directly editing GRID Legends graphics configurations, built with Kotlin and Jetpack Compose.

The app provides two distinct workflows: create and export a `preferences` file from bundled real configurations without game-directory permission, or directly read and edit the game configuration through Root, Shizuku, or SAF.

## Features

- Reads and writes the GRID Legends `preferences` file.
- Supports the actual registry-style XML structure used by the game.
- Handles duplicate setting names safely by writing values to the correct section path.
- Provides manual editing for frame rate, resolution, quality, reflections, post-processing, HDR, SSR, and volumetrics.
- Includes 20 real configurations across Low, Medium, High, and Extreme groups for creating and exporting graphics files.
- Exports the complete, extensionless `preferences` file from bundled presets without game-directory permission, retaining the other settings in the preset file.
- Supports Root, Shizuku, and SAF access modes for directly editing the configuration in the game directory.
- Android 11-15 uses SAF folder authorization by default; if the system picker blocks `Android/data`, you can switch to Shizuku or Root.
- Android 16 and above uses Shizuku by default to access the game directory.

## Target Game

- Package: `com.feralinteractive.gridlegends_android`
- Config file:

```text
/sdcard/Android/data/com.feralinteractive.gridlegends_android/files/feral_app_support/preferences
```

## Usage

### No permission required: create and export a configuration

1. Select **Create a graphics file from presets** on the home screen.
2. Choose one preset from the 20 real configurations in the Low, Medium, High, and Extreme groups.
3. Adjust the parameters you need, or export without making changes.
4. Manually copy the exported extensionless `preferences` file to:

   ```text
   /sdcard/Android/data/com.feralinteractive.gridlegends_android/files/feral_app_support/preferences
   ```

Close the game and back up the original file before manually replacing it.
The export is a complete `preferences` file and retains the other settings in the selected preset file.

### Permission required: directly edit the game configuration

1. Install and launch GRID Legends at least once so the game creates the config file.
2. Open this tool and grant folder, Shizuku, or Root access.
3. Enter the graphics editor.
4. Adjust parameters manually, or select a preset and tap Apply Preset.
5. Save changes and restart the game. This workflow directly edits the `preferences` file in the game directory.

## Access Modes

- Root: if the device uses Magisk, KernelSU, APatch, or a similar root solution, choose Root access on the home screen.
- Android 11-15: use the system file picker by default; if it shows "Can't use this folder", switch to Shizuku or Root on the home screen.
- Android 16 and above: use Shizuku by default to access the game directory.

If a ROM blocks `Android/data` selection through SAF, authorization may fail. In that case, Shizuku or Root can be used as an alternative.

## Build

This is a single-module Android project built only with Gradle.

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

Release build configuration is in `app/build.gradle.kts`.

## Notes

- Extreme and HDR presets are demanding; lower-end devices may not run the game reliably.
- The direct-edit workflow creates a backup before saving.
- Close the game and back up the original file before manually replacing an exported `preferences` file.
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
