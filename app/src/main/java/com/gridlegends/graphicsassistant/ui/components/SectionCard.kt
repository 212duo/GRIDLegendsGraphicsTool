package com.gridlegends.graphicsassistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gridlegends.graphicsassistant.data.ParamSection
import com.gridlegends.graphicsassistant.ui.theme.*

/**
 * 获取分区对应的标题颜色
 */
fun sectionColor(section: ParamSection): Color {
    return when (section) {
        ParamSection.DISPLAY -> SectionDisplay
        ParamSection.QUALITY -> SectionQuality
        ParamSection.POST_PROCESSING -> SectionPostProcess
        ParamSection.REFLECTIONS -> SectionReflection
        ParamSection.DETAIL -> SectionTrackDetail
    }
}

/**
 * 分区卡片容器
 * 将同组参数包裹在带标题的圆角卡片中
 */
@Composable
fun SectionCard(
    section: ParamSection,
    paramCount: Int = 0,
    modifiedCount: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val accentColor = sectionColor(section)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkCard
        )
    ) {
        Column {
            // 分区标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        accentColor.copy(alpha = 0.1f),
                        RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${section.icon} ${section.title}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                if (modifiedCount > 0) {
                    Text(
                        text = "已修改 $modifiedCount/$paramCount",
                        fontSize = 12.sp,
                        color = NeonCyan
                    )
                }
            }

            // 分割线
            HorizontalDivider(
                color = accentColor.copy(alpha = 0.3f),
                thickness = 1.dp
            )

            // 参数内容
            Column(
                modifier = Modifier.padding(vertical = 4.dp),
                content = content
            )
        }
    }
}
