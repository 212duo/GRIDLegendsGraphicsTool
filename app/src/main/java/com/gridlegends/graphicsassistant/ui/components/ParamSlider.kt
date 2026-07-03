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
 * 滑块参数控件
 * 用于渲染分辨率等连续值参数
 */
@Composable
fun ParamSlider(
    param: GraphicsParam,
    paramValue: ParamValue,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentValue = paramValue.value.toFloatOrNull() ?: param.minValue
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
        // 参数名和当前值
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = param.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = if (isModified) NeonCyan else TextPrimary
            )
            Text(
                text = "${currentValue.toInt()}${param.unitSuffix}",
                style = MaterialTheme.typography.labelLarge,
                color = if (isModified) NeonCyan else TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 滑块
        Slider(
            value = currentValue,
            onValueChange = { newValue ->
                val snapped = (Math.round(newValue / param.step) * param.step)
                    .coerceIn(param.minValue, param.maxValue)
                onValueChange(snapped.toInt().toString())
            },
            valueRange = param.minValue..param.maxValue,
            steps = ((param.maxValue - param.minValue) / param.step - 1).toInt().coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = NeonCyan,
                activeTrackColor = NeonCyan,
                inactiveTrackColor = DarkSurfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // 参数说明
        Text(
            text = param.description,
            style = MaterialTheme.typography.bodyMedium,
            color = TextDisabled
        )
    }
}
