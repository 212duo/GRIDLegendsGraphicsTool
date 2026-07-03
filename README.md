# GRID Legends Graphics Tool

An Android graphics configuration editor for GRID Legends, built with Kotlin and Jetpack Compose.

This app edits the GRID Legends Android `preferences` file and provides ready-to-use graphics presets derived from real configuration samples.

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
  - Extreme 2K 120 FPS
  - Extreme 2K 120 FPS HDR
- Supports SAF access on Android 11-15.
- Supports Shizuku-based access for newer Android versions.

## Target Game

- Package: `com.feralinteractive.gridlegends_android`
- Config file:

```text
/sdcard/Android/data/com.feralinteractive.gridlegends_android/files/feral_app_support/preferences
```

## Build

This is a single-module Android project. Use Gradle only.

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

The release build is configured in `app/build.gradle.kts`.

## Notes

- The UI text is Simplified Chinese.
- The app preserves unmanaged XML values when writing changes.
- A backup file is created before saving.
- Some extreme presets may be unstable on low-end devices. Restart the game after saving.

## Tech Stack

- Kotlin
- Jetpack Compose
- AndroidX DocumentFile
- Shizuku API
- XmlPullParser
