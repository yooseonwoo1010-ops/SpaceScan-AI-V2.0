package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ai.AIScanAdvisor
import com.example.data.model.*
import com.example.data.repository.ProjectRepository
import com.example.location.LocationEngine
import com.example.mapping.GoogleMapScreen
import com.example.scanner.ScanEngine
import com.example.ui.screens.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

enum class ScreenTab(val title: String, val icon: ImageVector) {
    HOME("홈", Icons.Default.Home),
    MAP("지도", Icons.Default.Map),
    SCAN("스캔", Icons.Default.CameraAlt),
    THREE_D("3D", Icons.Default.ViewInAr),
    AI("AI", Icons.Default.AutoAwesome),
    PLAN("도면", Icons.Default.Description),
    PHOTOS("사진", Icons.Default.PhotoLibrary),
    SETTINGS("설정", Icons.Default.Settings),
    DEVICE_CHECK("진단", Icons.Default.Speed),
    NEW_PROJECT("새 프로젝트", Icons.Default.Add)
}

class MainActivity : ComponentActivity() {

    private lateinit var projectRepository: ProjectRepository
    private lateinit var scanEngine: ScanEngine
    private lateinit var locationEngine: LocationEngine
    private lateinit var aiAdvisor: AIScanAdvisor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        projectRepository = ProjectRepository(this)
        scanEngine = ScanEngine(this)
        locationEngine = LocationEngine(this)
        aiAdvisor = AIScanAdvisor(this)

        setContent {
            MyApplicationTheme {
                MainAppScreen(
                    projectRepository = projectRepository,
                    scanEngine = scanEngine,
                    locationEngine = locationEngine,
                    aiAdvisor = aiAdvisor
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationEngine.startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        locationEngine.stopLocationUpdates()
        scanEngine.stopSensors()
    }

    override fun onDestroy() {
        super.onDestroy()
        aiAdvisor.release()
    }
}

@Composable
fun MainAppScreen(
    projectRepository: ProjectRepository,
    scanEngine: ScanEngine,
    locationEngine: LocationEngine,
    aiAdvisor: AIScanAdvisor
) {
    val coroutineScope = rememberCoroutineScope()
    var currentTab by remember { mutableStateOf(ScreenTab.HOME) }

    val projects by projectRepository.getAllProjectsFlow().collectAsState(initial = emptyList())
    val currentProjectId by projectRepository.currentProjectId.collectAsState()

    val currentProject = remember(projects, currentProjectId) {
        projects.find { it.id == currentProjectId } ?: projects.firstOrNull()
    }

    val livePoints by scanEngine.livePoints.collectAsState()
    val liveMesh by scanEngine.liveMesh.collectAsState()
    val scanState by scanEngine.scanState.collectAsState()
    val locationPose by locationEngine.locationPose.collectAsState()
    val deviceCap by scanEngine.deviceCapability.collectAsState()

    // Query project child entities
    val scanImages by if (currentProject != null) {
        projectRepository.getImagesFlow(currentProject.id).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }

    val geoAnchors by if (currentProject != null) {
        projectRepository.getAnchorsFlow(currentProject.id).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }

    val floorPlans by if (currentProject != null) {
        projectRepository.getFloorPlansFlow(currentProject.id).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }

    val aiRecommendations by if (currentProject != null) {
        projectRepository.getRecommendationsFlow(currentProject.id).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }

    val activeFloorPlan = floorPlans.firstOrNull()

    // Request Location permission on startup
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            locationEngine.startLocationUpdates()
        }
    }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    Scaffold(
        bottomBar = {
            if (currentTab != ScreenTab.NEW_PROJECT) {
                NavigationBar(
                    containerColor = DarkSurface,
                    tonalElevation = 8.dp
                ) {
                    listOf(
                        ScreenTab.HOME,
                        ScreenTab.MAP,
                        ScreenTab.SCAN,
                        ScreenTab.THREE_D,
                        ScreenTab.AI
                    ).forEach { tab ->
                        val isSelected = currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    tab.icon,
                                    contentDescription = tab.title,
                                    tint = if (isSelected) PrimaryBlue else TextSecondary
                                )
                            },
                            label = {
                                Text(
                                    tab.title,
                                    color = if (isSelected) PrimaryBlue else TextSecondary,
                                    fontSize = 11.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = DarkSurfaceElevated
                            )
                        )
                    }
                }
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                ScreenTab.HOME -> {
                    HomeScreen(
                        projects = projects,
                        onSelectProject = { project ->
                            projectRepository.selectProject(project.id)
                        },
                        onNavigateToNewProject = { currentTab = ScreenTab.NEW_PROJECT },
                        onNavigateToScan = { project ->
                            projectRepository.selectProject(project.id)
                            currentTab = ScreenTab.SCAN
                        },
                        onNavigateTo3D = { project ->
                            projectRepository.selectProject(project.id)
                            currentTab = ScreenTab.THREE_D
                        },
                        onLaunchDemo = {
                            coroutineScope.launch {
                                val demoProj = projectRepository.seedDemoProject()
                                scanEngine.startScanning(isDemo = true)
                                locationEngine.setDemoLocation()
                                currentTab = ScreenTab.SCAN
                            }
                        }
                    )
                }

                ScreenTab.NEW_PROJECT -> {
                    NewProjectScreen(
                        onNavigateBack = { currentTab = ScreenTab.HOME },
                        onCreateProject = { name, building, address, desc, floors, mode ->
                            coroutineScope.launch {
                                val newProject = projectRepository.createProject(
                                    name = name,
                                    buildingName = building,
                                    address = address,
                                    description = desc,
                                    floorCount = floors,
                                    mode = mode,
                                    isDemo = false
                                )
                                currentTab = ScreenTab.SCAN
                            }
                        }
                    )
                }

                ScreenTab.SCAN -> {
                    ScanScreen(
                        currentProject = currentProject,
                        scanEngine = scanEngine,
                        aiAdvisor = aiAdvisor,
                        onSaveSegment = { segmentData ->
                            currentProject?.let { proj ->
                                coroutineScope.launch {
                                    val segment = ScanSegment(
                                        id = UUID.randomUUID().toString(),
                                        projectId = proj.id,
                                        floor = proj.currentFloor,
                                        pointCount = segmentData.points.size,
                                        vertexCount = segmentData.mesh.vertices.size / 3,
                                        triangleCount = segmentData.mesh.indices.size / 3,
                                        coverage = (segmentData.points.size / 1000f).coerceIn(0.1f, 1.0f),
                                        quality = segmentData.quality,
                                        minX = segmentData.boundingBox.minX,
                                        maxX = segmentData.boundingBox.maxX,
                                        minY = segmentData.boundingBox.minY,
                                        maxY = segmentData.boundingBox.maxY,
                                        minZ = segmentData.boundingBox.minZ,
                                        maxZ = segmentData.boundingBox.maxZ
                                    )
                                    projectRepository.saveScanSegment(segment)
                                }
                            }
                        },
                        onSaveKeyframe = { keyframe ->
                            coroutineScope.launch {
                                projectRepository.saveImage(keyframe)
                            }
                        },
                        onNavigateTo3D = { currentTab = ScreenTab.THREE_D }
                    )
                }

                ScreenTab.MAP -> {
                    GoogleMapScreen(
                        currentProject = currentProject,
                        locationPose = locationPose,
                        points = livePoints,
                        mesh = liveMesh,
                        geoAnchors = geoAnchors,
                        aiRecommendation = aiRecommendations.firstOrNull(),
                        onNavigateToScan = { currentTab = ScreenTab.SCAN },
                        onNavigateTo3D = { currentTab = ScreenTab.THREE_D },
                        onNavigateToPlan = { currentTab = ScreenTab.PLAN }
                    )
                }

                ScreenTab.THREE_D -> {
                    ThreeDScreen(
                        currentProject = currentProject,
                        points = livePoints,
                        mesh = liveMesh,
                        boundingBox = scanState.boundingBox,
                        scanImages = scanImages,
                        onNavigateToScan = { currentTab = ScreenTab.SCAN }
                    )
                }

                ScreenTab.AI -> {
                    AIScreen(
                        currentProject = currentProject,
                        recommendations = aiRecommendations,
                        aiAdvisor = aiAdvisor,
                        onNavigateToScan = { currentTab = ScreenTab.SCAN }
                    )
                }

                ScreenTab.PLAN -> {
                    PlanScreen(
                        currentProject = currentProject,
                        floorPlan = activeFloorPlan,
                        aiAdvisor = aiAdvisor,
                        onSaveFloorPlan = { plan ->
                            coroutineScope.launch {
                                projectRepository.saveFloorPlan(plan)
                            }
                        },
                        onNavigateToScan = { currentTab = ScreenTab.SCAN },
                        onNavigateTo3D = { currentTab = ScreenTab.THREE_D }
                    )
                }

                ScreenTab.PHOTOS -> {
                    ImageGalleryScreen(
                        images = scanImages,
                        onNavigateTo3D = { currentTab = ScreenTab.THREE_D },
                        onNavigateToMap = { currentTab = ScreenTab.MAP }
                    )
                }

                ScreenTab.SETTINGS -> {
                    SettingsScreen(
                        aiAdvisor = aiAdvisor,
                        onNavigateToDeviceCheck = { currentTab = ScreenTab.DEVICE_CHECK }
                    )
                }

                ScreenTab.DEVICE_CHECK -> {
                    DeviceCheckScreen(deviceCap = deviceCap)
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
