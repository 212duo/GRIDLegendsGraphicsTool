package com.gridlegends.graphicsassistant.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gridlegends.graphicsassistant.data.LoadedPreset
import com.gridlegends.graphicsassistant.data.PresetCatalog
import com.gridlegends.graphicsassistant.data.PresetGroup
import com.gridlegends.graphicsassistant.data.PresetMetadata
import com.gridlegends.graphicsassistant.data.PresetRepository
import com.gridlegends.graphicsassistant.ui.theme.AccentOrange
import com.gridlegends.graphicsassistant.ui.theme.DarkBackground
import com.gridlegends.graphicsassistant.ui.theme.DarkCard
import com.gridlegends.graphicsassistant.ui.theme.DarkSurface
import com.gridlegends.graphicsassistant.ui.theme.NeonCyan
import com.gridlegends.graphicsassistant.ui.theme.TextPrimary
import com.gridlegends.graphicsassistant.ui.theme.TextSecondary
import com.gridlegends.graphicsassistant.ui.theme.WarningRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetLibraryScreen(
    onBack: () -> Unit,
    onPresetSelected: (LoadedPreset) -> Unit
) {
    val context = LocalContext.current
    val repository = remember(context) { PresetRepository(context) }
    var loadedPresets by remember { mutableStateOf<Map<String, LoadedPreset>>(emptyMap()) }
    var loadErrors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(repository) {
        val loaded = mutableMapOf<String, LoadedPreset>()
        val errors = mutableMapOf<String, String>()

        PresetCatalog.all.forEach { metadata ->
            repository.load(metadata).fold(
                onSuccess = { loaded[metadata.id] = it },
                onFailure = { errors[metadata.id] = it.message ?: "无法读取预设" }
            )
        }

        loadedPresets = loaded
        loadErrors = errors
        isLoading = false
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("选择画质预设") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "无需授权游戏目录。选择一个真实配置作为基础，修改后导出完整 preferences，并保留预设文件中的其他设置。",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(4.dp)
                )
            }

            if (isLoading) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = NeonCyan
                    )
                }
            }

            PresetGroup.entries.forEach { group ->
                item(key = "group_${group.name}") {
                    Text(
                        text = group.title,
                        color = NeonCyan,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                    )
                }
                items(
                    items = PresetCatalog.all.filter { it.group == group },
                    key = { it.id }
                ) { metadata ->
                    PresetLibraryCard(
                        metadata = metadata,
                        loadedPreset = loadedPresets[metadata.id],
                        error = loadErrors[metadata.id],
                        onSelect = onPresetSelected
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetLibraryCard(
    metadata: PresetMetadata,
    loadedPreset: LoadedPreset?,
    error: String?,
    onSelect: (LoadedPreset) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = metadata.name,
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(metadata.description, color = TextSecondary, fontSize = 13.sp)

            loadedPreset?.summary?.let { summary ->
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PresetTag("${summary.renderHeight}p")
                    PresetTag("${summary.frameRate} 帧")
                    if (summary.hdrEnabled) PresetTag("HDR", AccentOrange)
                }
            }

            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text("预设不可用：$it", color = WarningRed, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { loadedPreset?.let(onSelect) },
                enabled = loadedPreset != null,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = DarkBackground
                )
            ) {
                Text("基于此预设编辑", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PresetTag(text: String, color: Color = NeonCyan) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
