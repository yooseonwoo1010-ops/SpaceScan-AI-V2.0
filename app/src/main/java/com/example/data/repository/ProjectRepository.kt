package com.example.data.repository

import android.content.Context
import com.example.data.db.SpaceScanDatabase
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class ProjectRepository(context: Context) {
    private val database = SpaceScanDatabase.getDatabase(context)
    private val dao = database.spaceScanDao()

    private val _currentProjectId = MutableStateFlow<String?>(null)
    val currentProjectId = _currentProjectId.asStateFlow()

    fun selectProject(projectId: String?) {
        _currentProjectId.value = projectId
    }

    fun getAllProjectsFlow(): Flow<List<Project>> = dao.getAllProjectsFlow()

    suspend fun getProject(id: String): Project? = dao.getProjectById(id)

    fun getProjectFlow(id: String): Flow<Project?> = dao.getProjectFlow(id)

    suspend fun createProject(
        name: String,
        buildingName: String,
        address: String,
        description: String,
        floorCount: Int,
        mode: String,
        isDemo: Boolean = false
    ): Project {
        val newProject = Project(
            id = UUID.randomUUID().toString(),
            name = name,
            buildingName = buildingName,
            address = address,
            description = description,
            floorCount = floorCount,
            mode = mode,
            isDemo = isDemo,
            scanProgress = 0,
            mapProgress = 0,
            threeDProgress = 0,
            currentFloor = "1F"
        )
        dao.insertProject(newProject)
        _currentProjectId.value = newProject.id
        return newProject
    }

    suspend fun updateProject(project: Project) {
        dao.updateProject(project.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteProject(project: Project) {
        if (_currentProjectId.value == project.id) {
            _currentProjectId.value = null
        }
        dao.deleteSegmentsForProject(project.id)
        dao.deleteProject(project)
    }

    // Segments
    fun getSegmentsFlow(projectId: String): Flow<List<ScanSegment>> = dao.getSegmentsFlow(projectId)

    suspend fun saveScanSegment(segment: ScanSegment) {
        dao.insertSegment(segment)
        // Recalculate progress for project
        val project = dao.getProjectById(segment.projectId) ?: return
        val currentSegments = dao.getSegmentsForFloor(project.id, segment.floor)
        val totalPoints = currentSegments.sumOf { it.pointCount }
        val new3DProgress = (totalPoints / 250).coerceIn(0, 100)
        val newScanProgress = (new3DProgress * 0.9f).toInt().coerceIn(0, 100)
        val newMapProgress = (newScanProgress * 0.85f).toInt().coerceIn(0, 100)
        dao.updateProject(
            project.copy(
                scanProgress = newScanProgress.coerceAtLeast(project.scanProgress),
                mapProgress = newMapProgress.coerceAtLeast(project.mapProgress),
                threeDProgress = new3DProgress.coerceAtLeast(project.threeDProgress),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    // Images
    fun getImagesFlow(projectId: String): Flow<List<ScanImage>> = dao.getImagesFlow(projectId)
    suspend fun saveImage(image: ScanImage) = dao.insertImage(image)

    // Anchors
    fun getAnchorsFlow(projectId: String): Flow<List<GeoAnchor>> = dao.getAnchorsFlow(projectId)
    suspend fun saveAnchor(anchor: GeoAnchor) = dao.insertAnchor(anchor)

    // Floor Plans
    fun getFloorPlansFlow(projectId: String): Flow<List<FloorPlan>> = dao.getFloorPlansFlow(projectId)
    suspend fun getFloorPlanForFloor(projectId: String, floor: String): FloorPlan? =
        dao.getFloorPlanForFloor(projectId, floor)
    suspend fun saveFloorPlan(floorPlan: FloorPlan) = dao.insertFloorPlan(floorPlan)
    suspend fun updateFloorPlan(floorPlan: FloorPlan) = dao.updateFloorPlan(floorPlan)

    // AI Recommendations
    fun getRecommendationsFlow(projectId: String): Flow<List<AIRecommendation>> =
        dao.getRecommendationsFlow(projectId)
    suspend fun saveRecommendation(rec: AIRecommendation) = dao.insertRecommendation(rec)
    suspend fun updateRecommendation(rec: AIRecommendation) = dao.updateRecommendation(rec)

    suspend fun seedDemoProject(): Project {
        val demoProject = Project(
            id = "demo_school_main",
            name = "학교 본관 (DEMO)",
            buildingName = "한국과학고 본관",
            address = "서울특별시 서초구 반포대로 12",
            description = "에뮬레이터 및 시연용 데모 프로젝트 (DEMO MODE)",
            floorCount = 3,
            mode = "INDOOR_OUTDOOR",
            currentFloor = "2F",
            scanProgress = 72,
            mapProgress = 82,
            threeDProgress = 65,
            geoOriginLat = 37.498095,
            geoOriginLng = 127.027610,
            isDemo = true
        )
        dao.insertProject(demoProject)

        // Seed demo segments
        val demoSegment = ScanSegment(
            id = "demo_seg_2f_1",
            projectId = demoProject.id,
            floor = "2F",
            pointCount = 12842,
            vertexCount = 18420,
            triangleCount = 6140,
            coverage = 0.72f,
            quality = "HIGH",
            minX = -12.4f,
            maxX = 14.8f,
            minY = 0.0f,
            maxY = 2.8f,
            minZ = -8.5f,
            maxZ = 16.2f,
            timestamp = System.currentTimeMillis()
        )
        dao.insertSegment(demoSegment)

        // Seed demo anchor
        val demoAnchor = GeoAnchor(
            id = "demo_anchor_entrance",
            projectId = demoProject.id,
            title = "본관 정문 입구",
            latitude = 37.498095,
            longitude = 127.027610,
            altitude = 38.5,
            heading = 45f,
            localX = 0f,
            localY = 0f,
            localZ = 0f,
            accuracyMeters = 0.8f
        )
        dao.insertAnchor(demoAnchor)

        // Seed demo floor plan
        val demoPlan = FloorPlan(
            id = "demo_plan_2f",
            projectId = demoProject.id,
            floor = "2F",
            name = "2층 종합 안내도",
            sourceType = "WEB_SEARCH",
            sourceUrl = "https://school.sample.edu/facilities/floor2",
            widthMeters = 45f,
            heightMeters = 28f,
            scaleFactor = 1.0f,
            georeferenced = true,
            recognizedRoomsCount = 18,
            recognizedCorridorsCount = 4,
            confidence = 0.94f
        )
        dao.insertFloorPlan(demoPlan)

        // Seed demo AI recommendation
        val demoRec1 = AIRecommendation(
            id = "demo_rec_1",
            projectId = demoProject.id,
            floor = "2F",
            title = "203호 북쪽 벽 스캔 필요",
            reason = "북쪽 벽면의 포인트 클라우드 밀도가 기준치(500pt/m²) 미만입니다.",
            targetRoom = "203호",
            distanceMeters = 12.4f,
            priority = "HIGH",
            targetWorldX = 6.2f,
            targetWorldZ = 8.5f
        )
        val demoRec2 = AIRecommendation(
            id = "demo_rec_2",
            projectId = demoProject.id,
            floor = "2F",
            title = "204호 내부 스캔 추천",
            reason = "인접한 203호 스캔이 90% 이상 완료되어 다음 이동 동선으로 적합합니다.",
            targetRoom = "204호",
            distanceMeters = 18.2f,
            priority = "MEDIUM",
            targetWorldX = 12.0f,
            targetWorldZ = 8.5f
        )
        dao.insertRecommendation(demoRec1)
        dao.insertRecommendation(demoRec2)

        _currentProjectId.value = demoProject.id
        return demoProject
    }
}
