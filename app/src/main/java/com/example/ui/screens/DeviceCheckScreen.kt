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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scanner.DeviceCapability
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceCheckScreen(deviceCap: DeviceCapability) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("기기 호환성 및 센서 진단", color = TextPrimary, fontSize = 18.sp) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("하드웨어 센서 & API 상태", color = TextPrimary, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    SensorRow("카메라 (CameraX)", deviceCap.hasCamera, "공간 촬영 및 키프레임 수집")
                    SensorRow("ARCore 플랫폼", deviceCap.isArCoreSupported, "실시간 6자유도 Visual-Inertial Odometry")
                    SensorRow("Depth 하드웨어 센서", deviceCap.hasDepthSensor, "ToF / Raw Depth 3D 거리 측정")
                    SensorRow("GPS / GNSS 수신기", deviceCap.hasGps, "실외 위도/경도 절대 좌표")
                    SensorRow("나침반 & 회전 센서 (IMU)", deviceCap.hasCompass, "3축 방위각 및 기울기 추적")
                    SensorRow("Geospatial & VPS", deviceCap.isArCoreSupported && deviceCap.hasGps, "지리공간 앵커 및 시각적 위치 확인")
                    SensorRow("로컬 Room 데이터베이스", deviceCap.isStorageReady, "3D 포인트, 메쉬, 프로젝트 영구 저장")
                    SensorRow("3D 가속 그래픽 렌더러", deviceCap.isRendererReady, "OpenGL ES / 3D Canvas 가속")
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val isAdvanced = deviceCap.hasCamera && deviceCap.isArCoreSupported
                    Text(
                        if (isAdvanced) "✓ 고급 3D 공간 스캔 가능" else "ℹ 표준 센서 측정 모드",
                        color = if (isAdvanced) StatusGreen else StatusYellow,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        if (isAdvanced) {
                            "기기에서 카메라, ARCore 및 센서 퓨전이 완전히 지원되어 고정밀 3D 스캔 및 메쉬 재구성을 수행할 수 있습니다."
                        } else {
                            "ARCore 미지원 또는 에뮬레이터 환경에서는 센서/카메라 기본 모드 및 DEMO 시뮬레이터 모드로 완벽하게 동작합니다."
                        },
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SensorRow(name: String, isAvailable: Boolean, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = TextPrimary, fontSize = 13.sp)
            Text(desc, color = TextMuted, fontSize = 10.sp)
        }
        Badge(containerColor = if (isAvailable) StatusGreen else StatusRed) {
            Text(if (isAvailable) "✓ 지원" else "✕ 미지원", color = if (isAvailable) DarkBackground else Color.White, fontSize = 10.sp)
        }
    }
}
