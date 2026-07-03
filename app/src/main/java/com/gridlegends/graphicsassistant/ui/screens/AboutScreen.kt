package com.gridlegends.graphicsassistant.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gridlegends.graphicsassistant.ui.theme.*

/**
 * 关于页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部栏
            TopAppBar(
                title = { Text("关于") },
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // App 图标
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🏁",
                        fontSize = 40.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // App 名称
                Text(
                    text = "GRID Legends 画质助手",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )

                Text(
                    text = "v1.0.0",
                    fontSize = 14.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "突破游戏画质菜单限制\n精细调节 25+ 项画面参数",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 功能介绍卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "功能特性",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FeatureItem("🖥️ 显示设置", "渲染分辨率、帧率目标、垂直同步")
                        FeatureItem("🎨 画面质量", "纹理、阴影、抗锯齿、各向异性过滤")
                        FeatureItem("✨ 后处理效果", "运动模糊、泛光、景深、镜头光晕、环境光遮蔽")
                        FeatureItem("🪞 反射", "车身反射、屏幕空间反射")
                        FeatureItem("🏁 赛道细节", "车辆/赛道精度、观众密度、粒子效果、绘制距离")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 开发者信息卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "开发者",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // GitHub 链接
                        LinkItem(
                            icon = Icons.Default.Code,
                            title = "GitHub",
                            subtitle = "github.com/212duo",
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/212duo")
                                )
                                context.startActivity(intent)
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = DarkSurfaceVariant
                        )

                        // 项目仓库
                        LinkItem(
                            icon = Icons.Default.Star,
                            title = "项目仓库",
                            subtitle = "GRIDLegendsGraphicsTool",
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/212duo/GRIDLegendsGraphicsTool")
                                )
                                context.startActivity(intent)
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = DarkSurfaceVariant
                        )

                        // 提交 Issue
                        LinkItem(
                            icon = Icons.Default.BugReport,
                            title = "反馈问题",
                            subtitle = "提交 Bug 或建议",
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/212duo/GRIDLegendsGraphicsTool/issues")
                                )
                                context.startActivity(intent)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 免责声明
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "免责声明",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentOrange
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• 本工具基于社区文档推测开发，参数名需真机验证\n" +
                                    "• 修改配置可能导致游戏不稳定，请自行承担风险\n" +
                                    "• 使用前建议备份原始配置文件\n" +
                                    "• 本工具与 Codemasters/EA/Feral Interactive 无关",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 底部版权
                Text(
                    text = "Made with ❤️ by 212duo",
                    fontSize = 12.sp,
                    color = TextDisabled
                )
                Text(
                    text = "GRID Legends © Codemasters / EA\nFeral Interactive 移植版",
                    fontSize = 11.sp,
                    color = TextDisabled,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

/**
 * 功能特性项
 */
@Composable
private fun FeatureItem(title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = description,
            fontSize = 13.sp,
            color = TextSecondary
        )
    }
}

/**
 * 链接项
 */
@Composable
private fun LinkItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}
