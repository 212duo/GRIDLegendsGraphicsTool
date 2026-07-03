package com.gridlegends.graphicsassistant.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** GRID Legends 画质助手字体配置 */
val Typography = Typography(
    // 大标题
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        color = TextPrimary
    ),
    // 中标题
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = TextPrimary
    ),
    // 小标题
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = TextPrimary
    ),
    // 分区标题
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = TextPrimary
    ),
    // 参数名
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = TextPrimary
    ),
    // 正文
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = TextSecondary
    ),
    // 参数说明
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = TextSecondary
    ),
    // 标签
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = TextPrimary
    ),
    // 小标签
    labelSmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        color = TextDisabled
    )
)
