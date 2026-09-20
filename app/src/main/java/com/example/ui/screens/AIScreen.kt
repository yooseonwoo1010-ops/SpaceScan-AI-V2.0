package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AIScanAdvisor
import com.example.data.model.AIRecommendation
import com.example.data.model.Project
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIScreen(
    currentProject: Project?,
    recommendations: List<AIRecommendation>,
    aiAdvisor: AIScanAdvisor,
    onNavigateToScan: () -> Unit
) {
    var isVoiceEnabled by remember { mutableStateOf(aiAdvisor.isVoiceEnabled) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StatusPurple)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SpaceScan AI 어드바이저", color = TextPrimary, fontSize = 18.sp)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isVoiceEnabled = !isVoiceEnabled
                        aiAdvisor.isVoiceEnabled = isVoiceEnabled
                    }) {
                        Icon(
                            if (isVoiceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "음성 안내",
                            tint = if (isVoiceEnabled) PrimaryBlue else TextMuted
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Project Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(currentProject?.name ?: "프로젝트 미선택", color = TextPrimary, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${currentProject?.currentFloor ?: "1F"} · 전체 커버리지 ${currentProject?.scanProgress ?: 0}%",
                            color = AccentCyan,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { (currentProject?.scanProgress ?: 0) / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = PrimaryBlue,
                            trackColor = DarkSurfaceBorder
                        )
                    }
                }
            }

            item {
                Text(
                    "AI 스캔 추천 및 경로 안내 (${recommendations.size})",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (recommendations.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Insights, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("스캔 데이터가 쌓이면 AI 추천이 시작됩니다.", color = TextSecondary, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onNavigateToScan,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Text("스캔 시작하기", color = DarkBackground)
                            }
                        }
                    }
                }
            } else {
                items(recommendations) { rec ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(StatusPurple.copy(alpha = 0.2f), shape = CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Place, contentDescription = null, tint = StatusPurple, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(rec.targetRoom, color = TextPrimary, fontSize = 15.sp)
                                }
                                Badge(
                                    containerColor = when (rec.priority) {
                                        "HIGH" -> StatusRed
                                        "MEDIUM" -> StatusYellow
                                        else -> StatusBlue
                                    }
                                ) {
                                    Text(rec.priority, color = DarkBackground, fontSize = 10.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(rec.title, color = AccentCyan, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(rec.reason, color = TextSecondary, fontSize = 12.sp)

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("예상 이동: %.1fm".format(rec.distanceMeters), color = TextMuted, fontSize = 11.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    IconButton(onClick = {
                                        aiAdvisor.speakGuidance("${rec.title}. ${rec.reason}")
                                    }) {
                                        Icon(Icons.Default.VolumeUp, contentDescription = "음성", tint = TextPrimary)
                                    }
                                    Button(
                                        onClick = onNavigateToScan,
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("AR 경로 보기", color = DarkBackground, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
