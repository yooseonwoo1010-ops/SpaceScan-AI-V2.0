package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.viewer.ThreeDViewerCanvas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreeDScreen(
    currentProject: Project?,
    points: List<Point3D>,
    mesh: MeshData,
    boundingBox: BoundingBox,
    scanImages: List<ScanImage>,
    onNavigateToScan: () -> Unit
) {
    var selectedFloor by remember { mutableStateOf("전체") }

    val floorOptions = remember(currentProject) {
        val count = currentProject?.floorCount ?: 3
        val list = mutableListOf("전체")
        for (i in 1..count) {
            list.add("${i}F")
        }
        list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("${currentProject?.name ?: "공간 3D"} - 3D 모델 뷰어", color = TextPrimary, fontSize = 16.sp)
                        Text(
                            "포인트 ${points.size}개 · 정점 ${mesh.vertices.size / 3}개 · 실제 메트릭 비율 보존",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToScan) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "스캔으로 이동", tint = PrimaryBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Floor Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("층 선택:", color = TextSecondary, fontSize = 12.sp)
                floorOptions.forEach { floor ->
                    val isSelected = selectedFloor == floor
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFloor = floor },
                        label = { Text(floor, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = DarkBackground,
                            containerColor = DarkSurfaceElevated,
                            labelColor = TextPrimary
                        )
                    )
                }
            }

            // 3D Canvas
            if (points.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ViewInAr, contentDescription = null, tint = TextMuted, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("아직 스캔된 3D 공간이 없습니다.", color = TextPrimary, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("스캔 화면에서 공간을 스캔하거나 DEMO 모드를 실행하세요.", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onNavigateToScan,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("스캔 시작하기", color = DarkBackground)
                        }
                    }
                }
            } else {
                ThreeDViewerCanvas(
                    modifier = Modifier.fillMaxSize(),
                    points = points,
                    mesh = mesh,
                    boundingBox = boundingBox,
                    scanImages = scanImages,
                    currentFloor = selectedFloor
                )
            }
        }
    }
}
