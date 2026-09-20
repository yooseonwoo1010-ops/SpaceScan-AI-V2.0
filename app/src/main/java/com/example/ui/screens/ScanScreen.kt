package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ai.AIScanAdvisor
import com.example.data.model.*
import com.example.mapping.IndoorMapCanvas
import com.example.scanner.*
import com.example.ui.theme.*
import java.util.UUID

@Composable
fun ScanScreen(
    currentProject: Project?,
    scanEngine: ScanEngine,
    aiAdvisor: AIScanAdvisor,
    onSaveSegment: (ScanSegmentData) -> Unit,
    onSaveKeyframe: (ScanImage) -> Unit,
    onNavigateTo3D: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val scanState by scanEngine.scanState.collectAsState()
    val livePoints by scanEngine.livePoints.collectAsState()
    val pathRecords by scanEngine.pathRecords.collectAsState()
    val deviceCap by scanEngine.deviceCapability.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    val currentFloor = currentProject?.currentFloor ?: "1F"

    // Evaluate live AI recommendation
    val currentAiRec = remember(livePoints.size, scanState.currentPosX, scanState.currentPosZ) {
        aiAdvisor.evaluateNextScanTarget(
            projectId = currentProject?.id ?: "unknown",
            floor = currentFloor,
            currentPosX = scanState.currentPosX,
            currentPosZ = scanState.currentPosZ,
            points = livePoints,
            bbox = scanState.boundingBox,
            floorPlan = null
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // 1. Top Survey Status Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Badge(containerColor = PrimaryBlue) {
                    Text(currentFloor, color = DarkBackground, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (scanState.isDemoMode) "DEMO SCAN" else "REAL SCAN",
                    color = if (scanState.isDemoMode) StatusYellow else StatusGreen,
                    fontSize = 12.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusPill(
                    label = "TRACKING",
                    isActive = scanState.trackingState == TrackingState.TRACKING,
                    activeColor = StatusGreen
                )
                StatusPill(
                    label = "DEPTH",
                    isActive = scanState.isDepthActive,
                    activeColor = StatusCyan
                )
                Text(
                    "${scanState.pointCount} PT",
                    color = TextPrimary,
                    fontSize = 11.sp
                )
            }
        }

        // 2. Camera Preview + AR Overlay Area (Occupies primary viewport)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.3f)
                .background(Color.Black)
        ) {
            if (hasCameraPermission && deviceCap.hasCamera && !scanState.isDemoMode) {
                // Real Android CameraX Preview
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                            } catch (e: Exception) {
                                // Camera in use or unavailable
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    }
                )
            } else if (!hasCameraPermission && !scanState.isDemoMode) {
                // Permission Request Fallback
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = StatusYellow, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("3D 공간 스캔을 위해 카메라 권한이 필요합니다.", color = TextPrimary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("권한 허용", color = DarkBackground)
                    }
                }
            } else {
                // Simulated Camera Canvas for Demo Mode
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F172A))
                ) {
                    Text(
                        "DEMO MODE 시뮬레이터 카메라 활성",
                        modifier = Modifier.align(Alignment.Center),
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            // Real-time Overlay Hud (Grid, Center Crosshair, Direction Arrow)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, AccentCyan.copy(alpha = 0.25f))
            ) {
                // Center reticle
                Icon(
                    Icons.Default.Adjust,
                    contentDescription = null,
                    tint = AccentCyan.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.Center)
                )

                // Scan Status Color Legend on Overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .background(DarkSurface.copy(alpha = 0.75f), shape = RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    LegendItem(StatusGreen, "완료 영역")
                    LegendItem(StatusBlue, "현재 스캔 중")
                    LegendItem(StatusRed, "재스캔 필요")
                    LegendItem(StatusPurple, "AI 추천 방향")
                }

                // AR Navigation Arrow towards AI Target
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .background(DarkSurfaceElevated.copy(alpha = 0.85f), shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, tint = StatusPurple, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "${currentAiRec.targetRoom} 방향 (%.1fm)".format(currentAiRec.distanceMeters),
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // 3. 2D Indoor Map Mini-View (Showing real top-down scan progress)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.0f)
                .background(DarkSurface)
        ) {
            IndoorMapCanvas(
                modifier = Modifier.fillMaxSize(),
                points = livePoints,
                cameraPath = pathRecords,
                currentX = scanState.currentPosX,
                currentZ = scanState.currentPosZ,
                currentYaw = scanState.yawDeg,
                aiRecommendation = currentAiRec,
                scanImages = emptyList()
            )
        }

        // 4. AI Real-time Guidance Banner Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(StatusPurple.copy(alpha = 0.2f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StatusPurple, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("AI 실시간 안내: ${currentAiRec.title}", color = TextPrimary, fontSize = 13.sp)
                    Text(currentAiRec.reason, color = TextSecondary, fontSize = 11.sp, maxLines = 2)
                }
                IconButton(onClick = {
                    aiAdvisor.speakGuidance("${currentAiRec.title}. ${currentAiRec.reason}")
                }) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "음성 안내", tint = PrimaryBlue)
                }
            }
        }

        // 5. Scan Controls & Keyframe Trigger
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!scanState.isScanning) {
                Button(
                    onClick = { scanEngine.startScanning(isDemo = currentProject?.isDemo == true) },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DarkBackground)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("스캔 시작", color = DarkBackground)
                }
            } else {
                Button(
                    onClick = {
                        val segmentData = scanEngine.stopScanning()
                        onSaveSegment(segmentData)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, tint = TextPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("스캔 정지 및 저장", color = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Keyframe Image Capture Button (📷)
            IconButton(
                onClick = {
                    val keyframe = ScanImage(
                        id = UUID.randomUUID().toString(),
                        projectId = currentProject?.id ?: "unknown",
                        scanSegmentId = "active_seg",
                        imageUri = "",
                        worldX = scanState.currentPosX,
                        worldY = scanState.currentPosY,
                        worldZ = scanState.currentPosZ,
                        yaw = scanState.yawDeg,
                        pitch = scanState.pitchDeg,
                        mapX = scanState.currentPosX,
                        mapY = scanState.currentPosZ,
                        floor = currentFloor,
                        roomId = currentAiRec.targetRoom,
                        quality = scanState.quality
                    )
                    onSaveKeyframe(keyframe)
                },
                modifier = Modifier
                    .background(DarkSurfaceElevated, CircleShape)
                    .size(48.dp)
            ) {
                Icon(Icons.Default.Camera, contentDescription = "키프레임 사진 저장", tint = PrimaryBlue)
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
                onClick = onNavigateTo3D,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ViewInAr, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("3D")
            }
        }
    }
}

@Composable
private fun StatusPill(label: String, isActive: Boolean, activeColor: Color) {
    Row(
        modifier = Modifier
            .background(DarkSurfaceElevated, shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(if (isActive) activeColor else StatusGray, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            "$label ${if (isActive) "✓" else "✕"}",
            color = if (isActive) TextPrimary else TextMuted,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 1.dp)) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, color = TextSecondary, fontSize = 9.sp)
    }
}
