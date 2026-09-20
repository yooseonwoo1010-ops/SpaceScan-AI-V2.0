package com.example.mapping

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.data.model.*
import com.example.location.FusedLocationPose
import com.example.scanner.CoordinateTransform
import com.example.ui.theme.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleMapScreen(
    currentProject: Project?,
    locationPose: FusedLocationPose,
    points: List<Point3D>,
    mesh: MeshData,
    geoAnchors: List<GeoAnchor>,
    aiRecommendation: AIRecommendation?,
    modifier: Modifier = Modifier,
    onNavigateToScan: () -> Unit,
    onNavigateTo3D: () -> Unit,
    onNavigateToPlan: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSatellite by remember { mutableStateOf(false) }
    var modelOpacity by remember { mutableFloatStateOf(0.5f) }
    var showLayersMenu by remember { mutableStateOf(false) }

    // Layer checkboxes
    var showBaseMap by remember { mutableStateOf(true) }
    var showUserLocation by remember { mutableStateOf(true) }
    var show3DModel by remember { mutableStateOf(true) }
    var showScanPath by remember { mutableStateOf(true) }
    var showAnchors by remember { mutableStateOf(true) }
    var showAiRoute by remember { mutableStateOf(true) }

    // Center coordinates (either from anchor, project origin, or user GPS)
    val mapCenter = remember(locationPose, currentProject, geoAnchors) {
        val lat = when {
            geoAnchors.isNotEmpty() -> geoAnchors.first().latitude
            currentProject != null && currentProject.geoOriginLat != 0.0 -> currentProject.geoOriginLat
            locationPose.hasFix -> locationPose.latitude
            else -> 37.498095 // Default Seoul Gangnam / Seocho
        }
        val lng = when {
            geoAnchors.isNotEmpty() -> geoAnchors.first().longitude
            currentProject != null && currentProject.geoOriginLng != 0.0 -> currentProject.geoOriginLng
            locationPose.hasFix -> locationPose.longitude
            else -> 127.027610
        }
        LatLng(lat, lng)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(mapCenter, 18.5f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("장소 / 건물 검색", color = TextSecondary, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "검색", tint = TextSecondary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurfaceElevated,
                            unfocusedContainerColor = DarkSurface,
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = DarkSurfaceBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                },
                actions = {
                    IconButton(onClick = { showLayersMenu = !showLayersMenu }) {
                        Icon(Icons.Default.Layers, contentDescription = "레이어", tint = PrimaryBlue)
                    }
                    IconButton(onClick = { isSatellite = !isSatellite }) {
                        Icon(
                            if (isSatellite) Icons.Default.Map else Icons.Default.Satellite,
                            contentDescription = "지도 유형",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Google Map View
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    mapType = if (isSatellite) MapType.SATELLITE else MapType.NORMAL,
                    isMyLocationEnabled = showUserLocation && locationPose.hasFix
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    compassEnabled = true,
                    myLocationButtonEnabled = true
                )
            ) {
                // Geo Anchors Markers
                if (showAnchors) {
                    for (anchor in geoAnchors) {
                        Marker(
                            state = MarkerState(position = LatLng(anchor.latitude, anchor.longitude)),
                            title = anchor.title,
                            snippet = "정확도: %.1fm | 앵커".format(anchor.accuracyMeters)
                        )
                    }
                }

                // AI Target Marker
                if (showAiRoute && aiRecommendation != null && currentProject != null) {
                    val originLat = if (currentProject.geoOriginLat != 0.0) currentProject.geoOriginLat else mapCenter.latitude
                    val originLng = if (currentProject.geoOriginLng != 0.0) currentProject.geoOriginLng else mapCenter.longitude
                    val (targetLat, targetLng) = CoordinateTransform.worldToGeo(
                        aiRecommendation.targetWorldX,
                        aiRecommendation.targetWorldZ,
                        originLat,
                        originLng
                    )
                    Marker(
                        state = MarkerState(position = LatLng(targetLat, targetLng)),
                        title = "✨ AI 추천: ${aiRecommendation.title}",
                        snippet = aiRecommendation.reason
                    )
                }
            }

            // 3D Model Semi-Transparent Overlay Representation
            if (show3DModel && points.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(240.dp)
                        .background(
                            PrimaryBlue.copy(alpha = modelOpacity * 0.25f),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.ViewInAr, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("3D 모델 중첩 배치됨", color = TextPrimary, fontSize = 13.sp)
                        Text("${points.size}개 포인트 (투명도 ${(modelOpacity * 100).toInt()}%)", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }

            // Layers Popup Menu
            if (showLayersMenu) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .width(220.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("지도 레이어 관리", color = TextPrimary, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        LayerCheckbox("실제 지도", showBaseMap) { showBaseMap = it }
                        LayerCheckbox("내 위치", showUserLocation) { showUserLocation = it }
                        LayerCheckbox("3D Scan 모델", show3DModel) { show3DModel = it }
                        LayerCheckbox("위치 앵커", showAnchors) { showAnchors = it }
                        LayerCheckbox("AI 추천 경로", showAiRoute) { showAiRoute = it }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("3D 모델 투명도", color = TextSecondary, fontSize = 11.sp)
                        Slider(
                            value = modelOpacity,
                            onValueChange = { modelOpacity = it },
                            valueRange = 0f..1f,
                            steps = 3,
                            colors = SliderDefaults.colors(thumbColor = PrimaryBlue, activeTrackColor = PrimaryBlue)
                        )
                    }
                }
            }

            // Bottom Selected Project Card Floating Panel
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(currentProject?.name ?: "선택된 프로젝트 없음", color = TextPrimary, fontSize = 16.sp)
                            Text(currentProject?.address ?: "GPS 기반 실시간 위치", color = TextSecondary, fontSize = 12.sp)
                        }
                        Badge(containerColor = StatusGreen) {
                            Text("${currentProject?.scanProgress ?: 0}% 스캔", color = DarkBackground)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onNavigateToScan,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = DarkBackground)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("스캔 계속", color = DarkBackground)
                        }
                        OutlinedButton(
                            onClick = onNavigateTo3D,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            Icon(Icons.Default.ViewInAr, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("3D 보기")
                        }
                        OutlinedButton(
                            onClick = onNavigateToPlan,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LayerCheckbox(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
        )
        Text(title, color = TextPrimary, fontSize = 12.sp)
    }
}
