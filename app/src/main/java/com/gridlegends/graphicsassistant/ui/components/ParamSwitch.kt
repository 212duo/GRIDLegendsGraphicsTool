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
 * 开关参数控件
 * 用于垂直同步、运动模糊等开/关参数
 */
@Composable
fun ParamSwitch(
    param: GraphicsParam,
    paramValue: ParamValue,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isOn = paramValue.value == "1"
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = param.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isModified) NeonCyan else TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = param.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDisabled
                )
            }

            Switch(
                checked = isOn,
                onCheckedChange = { checked ->
                    onValueChange(if (checked) "1" else "0")
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NeonCyan,
                    checkedTrackColor = NeonCyan.copy(alpha = 0.3f),
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = DarkSurfaceVariant
                )
            )
        }
    }
}
