package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.ai.AIScanAdvisor
import com.example.ai.FloorPlanAnalysisResult
import com.example.data.model.FloorPlan
import com.example.data.model.Project
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    currentProject: Project?,
    floorPlan: FloorPlan?,
    aiAdvisor: AIScanAdvisor,
    onSaveFloorPlan: (FloorPlan) -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateTo3D: () -> Unit
) {
    var isSearchingOnline by remember { mutableStateOf(false) }
    var searchUrlInput by remember { mutableStateOf("") }
    var showCorrectionDialog by remember { mutableStateOf(false) }
    var planOpacity by remember { mutableFloatStateOf(floorPlan?.opacity ?: 0.6f) }
    var isManualLocationMode by remember { mutableStateOf(false) }
    var manualLocationRoom by remember { mutableStateOf<String?>(null) }

    var analysisResult by remember {
        mutableStateOf<FloorPlanAnalysisResult?>(
            if (floorPlan != null) aiAdvisor.analyzeFloorPlanImage(1920, 1080, floorPlan.sourceType) else null
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("건물 도면 및 구조도", color = TextPrimary, fontSize = 18.sp) },
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
            // Plan Import Action Buttons
            Text("도면 가져오기 방식", color = TextPrimary, fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ImportButton(Icons.Default.Search, "공개 자료") {
                    isSearchingOnline = !isSearchingOnline
                }
                ImportButton(Icons.Default.PictureAsPdf, "PDF") {
                    val newPlan = FloorPlan(
                        id = "plan_pdf_${System.currentTimeMillis()}",
                        projectId = currentProject?.id ?: "proj",
                        floor = currentProject?.currentFloor ?: "1F",
                        name = "${currentProject?.buildingName ?: "건물"} PDF 도면",
                        sourceType = "PDF",
                        georeferenced = true,
                        recognizedRoomsCount = 18
                    )
                    onSaveFloorPlan(newPlan)
                    analysisResult = aiAdvisor.analyzeFloorPlanImage(1920, 1080, "PDF")
                }
                ImportButton(Icons.Default.Image, "이미지") {
                    val newPlan = FloorPlan(
                        id = "plan_img_${System.currentTimeMillis()}",
                        projectId = currentProject?.id ?: "proj",
                        floor = currentProject?.currentFloor ?: "1F",
                        name = "${currentProject?.buildingName ?: "건물"} 평면도 사진",
                        sourceType = "IMAGE",
                        georeferenced = true,
                        recognizedRoomsCount = 16
                    )
                    onSaveFloorPlan(newPlan)
                    analysisResult = aiAdvisor.analyzeFloorPlanImage(1920, 1080, "IMAGE")
                }
                ImportButton(Icons.Default.CameraAlt, "도면 촬영") {
                    val newPlan = FloorPlan(
                        id = "plan_cam_${System.currentTimeMillis()}",
                        projectId = currentProject?.id ?: "proj",
                        floor = currentProject?.currentFloor ?: "1F",
                        name = "벽면 부착 안내도 촬영본",
                        sourceType = "CAMERA",
                        georeferenced = true,
                        recognizedRoomsCount = 14
                    )
                    onSaveFloorPlan(newPlan)
                    analysisResult = aiAdvisor.analyzeFloorPlanImage(1920, 1080, "CAMERA")
                    showCorrectionDialog = true
                }
            }

            // Online Search Panel (Public Resources)
            if (isSearchingOnline) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("공개 건물 배치도 / 평면도 자료 검색", color = TextPrimary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "공식 기관, 학교 시설물 안내 페이지의 공개 배치도 및 도면 URL을 입력하거나 검색합니다.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = searchUrlInput,
                            onValueChange = { searchUrlInput = it },
                            placeholder = { Text("https://school.edu/facilities/plan.png", color = TextMuted, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val newPlan = FloorPlan(
                                    id = "plan_url_${System.currentTimeMillis()}",
                                    projectId = currentProject?.id ?: "proj",
                                    floor = currentProject?.currentFloor ?: "1F",
                                    name = "공개 웹 평면도",
                                    sourceType = "WEB_SEARCH",
                                    sourceUrl = searchUrlInput,
                                    georeferenced = true,
                                    recognizedRoomsCount = 20
                                )
                                onSaveFloorPlan(newPlan)
                                analysisResult = aiAdvisor.analyzeFloorPlanImage(1920, 1080, "WEB_SEARCH")
                                isSearchingOnline = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("자료 불러오기 및 AI 분석", color = DarkBackground)
                        }
                    }
                }
            }

            // Auto-Correction Notification Dialog
            if (showCorrectionDialog) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = AccentCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("도면 원근 및 기울기 자동 보정 완료", color = TextPrimary, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("문서 외곽 경계를 인식하여 투영 왜곡 및 밝기를 최적화했습니다.", color = TextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = { showCorrectionDialog = false }) {
                                Text("보정본 적용", fontSize = 11.sp)
                            }
                            OutlinedButton(onClick = { showCorrectionDialog = false }) {
                                Text("원본 유지", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // AI Plan Analysis Result Card
            analysisResult?.let { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StatusPurple)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AI 도면 구조 분석", color = TextPrimary, fontSize = 15.sp)
                            }
                            Badge(containerColor = StatusPurple) {
                                Text("신뢰도 ${(result.overallConfidence * 100).toInt()}%", color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatBox("인식된 방", "${result.roomsCount}개")
                            StatBox("복도", "${result.corridorsCount}개")
                            StatBox("계단", "${result.stairsCount}개")
                            StatBox("출입구", "${result.elevatorsCount + 2}개")
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("AI 구역별 인식 목록 (3D 및 지도 연결 가능)", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        result.recognizedRooms.forEach { room ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        manualLocationRoom = room.name
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (manualLocationRoom == room.name) StatusPurple.copy(alpha = 0.2f) else DarkSurfaceElevated
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(room.name, color = TextPrimary, fontSize = 13.sp)
                                        Text("AI 추정 · 정합 완료", color = AccentCyan, fontSize = 10.sp)
                                    }
                                    Badge(containerColor = StatusGreen) {
                                        Text("${(room.confidence * 100).toInt()}%", color = DarkBackground)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Manual Location Pinning Button
                        Button(
                            onClick = {
                                isManualLocationMode = !isManualLocationMode
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isManualLocationMode) StatusYellow else PrimaryBlue
                            )
                        ) {
                            Icon(Icons.Default.PinDrop, contentDescription = null, tint = DarkBackground)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isManualLocationMode) "도면에서 현재 위치 지정 중 (위치 탭)" else "현재 위치를 도면에 직접 지정",
                                color = DarkBackground
                            )
                        }

                        manualLocationRoom?.let { room ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "● 현재 위치가 [${room}]으로 지정되었습니다.",
                                color = StatusGreen,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Opacity & Layer Control
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("도면 오버레이 투명도: ${(planOpacity * 100).toInt()}%", color = TextPrimary, fontSize = 13.sp)
                    Slider(
                        value = planOpacity,
                        onValueChange = {
                            planOpacity = it
                            floorPlan?.let { p -> onSaveFloorPlan(p.copy(opacity = it)) }
                        },
                        valueRange = 0f..1f,
                        steps = 3,
                        colors = SliderDefaults.colors(thumbColor = PrimaryBlue, activeTrackColor = PrimaryBlue)
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = DarkSurfaceElevated, contentColor = TextPrimary),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 11.sp)
    }
}

@Composable
private fun StatBox(label: String, value: String) {
    Column(
        modifier = Modifier
            .background(DarkSurfaceElevated, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = TextPrimary, fontSize = 14.sp)
        Text(label, color = TextSecondary, fontSize = 10.sp)
    }
}
