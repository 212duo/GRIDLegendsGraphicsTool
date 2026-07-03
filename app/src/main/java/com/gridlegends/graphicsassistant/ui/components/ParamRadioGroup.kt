package com.gridlegends.graphicsassistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gridlegends.graphicsassistant.data.GraphicsParam
import com.gridlegends.graphicsassistant.data.ParamValue
import com.gridlegends.graphicsassistant.ui.theme.*

/**
 * 单选按钮组参数控件
 * 用于目标帧率、抗锯齿模式等枚举参数
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParamRadioGroup(
    param: GraphicsParam,
    paramValue: ParamValue,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isModified = paramValue.isModified

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isModified) Modifier.background(ModifiedHighlight)
                else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 参数名
        Text(
            text = param.displayName,
            style = MaterialTheme.typography.titleMedium,
            color = if (isModified) NeonCyan else TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 单选按钮组
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            param.options.forEach { (value, label) ->
                val selected = paramValue.value == value
                FilterChip(
                    selected = selected,
                    onClick = { onValueChange(value) },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                        selectedLabelColor = NeonCyan,
                        containerColor = DarkSurfaceVariant,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = DarkSurfaceVariant,
                        selectedBorderColor = NeonCyan,
                        enabled = true,
                        selected = selected
                    ),
                    modifier = Modifier.defaultMinSize(minWidth = 64.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 参数说明
        Text(
            text = param.description,
            style = MaterialTheme.typography.bodyMedium,
            color = TextDisabled
        )
    }
}
