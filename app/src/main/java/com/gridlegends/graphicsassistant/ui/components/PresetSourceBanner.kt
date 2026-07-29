package com.gridlegends.graphicsassistant.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gridlegends.graphicsassistant.data.LoadedPreset
import com.gridlegends.graphicsassistant.ui.theme.DarkCard
import com.gridlegends.graphicsassistant.ui.theme.NeonCyan
import com.gridlegends.graphicsassistant.ui.theme.TextPrimary
import com.gridlegends.graphicsassistant.ui.theme.TextSecondary

@Composable
fun PresetSourceBanner(preset: LoadedPreset) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("当前基础预设", color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                preset.metadata.name,
                color = NeonCyan,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${preset.summary.renderHeight}p · ${preset.summary.frameRate} 帧" +
                    if (preset.summary.hdrEnabled) " · HDR" else "",
                color = TextPrimary,
                fontSize = 13.sp
            )
        }
    }
}
