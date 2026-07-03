# GRID Legends 画质助手 ProGuard 规则

# 保留数据模型类
-keep class com.gridlegends.graphicsassistant.data.** { *; }

# 保留 Compose
-dontwarn androidx.compose.**

# ShizukuFileOperator reflects Shizuku.newProcess by name in release builds.
-keepclassmembers class rikka.shizuku.Shizuku {
    private static rikka.shizuku.ShizukuRemoteProcess newProcess(java.lang.String[], java.lang.String[], java.lang.String);
}
-keep class rikka.shizuku.ShizukuRemoteProcess { *; }
