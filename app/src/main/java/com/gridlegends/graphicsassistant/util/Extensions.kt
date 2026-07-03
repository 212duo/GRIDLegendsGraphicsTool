package com.gridlegends.graphicsassistant.util

import android.content.Context
import android.widget.Toast

/**
 * 工具函数和扩展
 */

/** 短时 Toast 提示 */
fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

/** 长时 Toast 提示 */
fun Context.longToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

/**
 * 安全解析浮点数
 */
fun String?.parseFloatOrNull(): Float? {
    return this?.toFloatOrNull()
}

/**
 * 安全解析整数
 */
fun String?.parseIntOrNull(): Int? {
    return this?.toIntOrNull()
}

/**
 * 限制值在指定范围内
 */
fun Float.clamp(min: Float, max: Float): Float {
    return coerceIn(min, max)
}

/**
 * 根据步长对齐值
 */
fun Float.snapToStep(step: Float): Float {
    if (step <= 0f) return this
    return Math.round(this / step) * step
}
