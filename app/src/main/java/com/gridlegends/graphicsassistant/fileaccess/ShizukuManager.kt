package com.gridlegends.graphicsassistant.fileaccess

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import rikka.shizuku.Shizuku

/**
 * Shizuku 状态管理器
 * 处理 Shizuku 的安装检测、权限请求和状态监听
 */
class ShizukuManager(private val context: Context) {

    companion object {
        private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        private const val REQUEST_CODE = 1001
    }

    /** Shizuku 授权结果回调 */
    var onResult: ((granted: Boolean) -> Unit)? = null

    /** Shizuku 状态变化监听器 */
    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        onStateChanged?.invoke()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        onStateChanged?.invoke()
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE) {
                val granted = grantResult == PackageManager.PERMISSION_GRANTED
                onResult?.invoke(granted)
            }
        }

    /** 状态变化回调（UI 刷新用） */
    var onStateChanged: (() -> Unit)? = null

    init {
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
    }

    /**
     * 是否默认使用 Shizuku 模式。
     * Android 16 (API 36) 及以上默认使用 Shizuku；Android 11-15 可在 SAF 受限时手动切换。
     */
    fun shouldUseShizuku(): Boolean {
        return Build.VERSION.SDK_INT >= 36
    }

    /**
     * Shizuku 是否已安装
     */
    fun isShizukuInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Shizuku 服务是否在运行
     */
    fun isShizukuRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 是否已获得 Shizuku 授权
     */
    fun isPermissionGranted(): Boolean {
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 请求 Shizuku 权限
     */
    fun requestPermission() {
        try {
            Shizuku.requestPermission(REQUEST_CODE)
        } catch (_: Exception) {
            onResult?.invoke(false)
        }
    }

    /**
     * 综合检查状态
     */
    fun getStatus(): ShizukuStatus {
        if (!isShizukuInstalled()) return ShizukuStatus.NOT_INSTALLED
        if (!isShizukuRunning()) return ShizukuStatus.NOT_RUNNING
        if (!isPermissionGranted()) return ShizukuStatus.NOT_AUTHORIZED
        return ShizukuStatus.READY
    }

    /**
     * 释放资源
     */
    fun destroy() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
    }
}

/**
 * Shizuku 状态枚举
 */
enum class ShizukuStatus {
    /** 未安装 Shizuku */
    NOT_INSTALLED,
    /** Shizuku 服务未启动 */
    NOT_RUNNING,
    /** 未授权 */
    NOT_AUTHORIZED,
    /** 就绪可用 */
    READY
}
