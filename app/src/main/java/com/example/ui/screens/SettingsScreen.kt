package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AIScanAdvisor
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    aiAdvisor: AIScanAdvisor,
    onNavigateToDeviceCheck: () -> Unit
) {
    var useDepth by remember { mutableStateOf(true) }
    var meshQuality by remember { mutableStateOf("HIGH") }
    var isVoiceEnabled by remember { mutableStateOf(aiAdvisor.isVoiceEnabled) }
    var measurementUnit by remember { mutableStateOf("미터 (m)") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정", color = TextPrimary, fontSize = 18.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("3D 스캔 엔진 설정", color = TextPrimary, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Depth 센서 우선 활용", color = TextPrimary, fontSize = 13.sp)
                            Text("지원 기기에서 ToF Raw Depth 활성화", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = useDepth,
                            onCheckedChange = { useDepth = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = PrimaryBlue)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("AI 음성 실시간 안내", color = TextPrimary, fontSize = 13.sp)
                            Text("스캔 중 다음 위치/경로 TTS 발화", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = isVoiceEnabled,
                            onCheckedChange = {
                                isVoiceEnabled = it
                                aiAdvisor.isVoiceEnabled = it
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = PrimaryBlue)
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("측정 및 지도 환경", color = TextPrimary, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("기본 측정 단위", color = TextPrimary, fontSize = 13.sp)
                        Badge(containerColor = PrimaryBlue) {
                            Text(measurementUnit, color = DarkBackground, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    FilledTonalButton(
                        onClick = onNavigateToDeviceCheck,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = DarkSurfaceElevated, contentColor = TextPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("기기 센서 및 호환성 종합 진단")
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("앱 정보", color = TextPrimary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("SpaceScan AI v1.0.0", color = AccentCyan, fontSize = 12.sp)
                    Text("실제 스마트폰 3D 스캔 & 지리공간 융합 플랫폼", color = TextSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}
