package com.example.data.db

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SpaceScanDao {

    // --- Projects ---
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjectsFlow(): Flow<List<Project>>

    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    suspend fun getAllProjects(): List<Project>

    @Query("SELECT * FROM projects WHERE id = :projectId LIMIT 1")
    suspend fun getProjectById(projectId: String): Project?

    @Query("SELECT * FROM projects WHERE id = :projectId LIMIT 1")
    fun getProjectFlow(projectId: String): Flow<Project?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project)

    @Update
    suspend fun updateProject(project: Project)

    @Delete
    suspend fun deleteProject(project: Project)

    // --- Scan Segments ---
    @Query("SELECT * FROM scan_segments WHERE projectId = :projectId ORDER BY timestamp ASC")
    fun getSegmentsFlow(projectId: String): Flow<List<ScanSegment>>

    @Query("SELECT * FROM scan_segments WHERE projectId = :projectId AND floor = :floor ORDER BY timestamp ASC")
    suspend fun getSegmentsForFloor(projectId: String, floor: String): List<ScanSegment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegment(segment: ScanSegment)

    @Query("DELETE FROM scan_segments WHERE projectId = :projectId")
    suspend fun deleteSegmentsForProject(projectId: String)

    // --- Scan Images ---
    @Query("SELECT * FROM scan_images WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getImagesFlow(projectId: String): Flow<List<ScanImage>>

    @Query("SELECT * FROM scan_images WHERE projectId = :projectId AND floor = :floor ORDER BY timestamp DESC")
    suspend fun getImagesForFloor(projectId: String, floor: String): List<ScanImage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: ScanImage)

    // --- Geo Anchors ---
    @Query("SELECT * FROM geo_anchors WHERE projectId = :projectId ORDER BY timestamp ASC")
    fun getAnchorsFlow(projectId: String): Flow<List<GeoAnchor>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnchor(anchor: GeoAnchor)

    // --- Floor Plans ---
    @Query("SELECT * FROM floor_plans WHERE projectId = :projectId")
    fun getFloorPlansFlow(projectId: String): Flow<List<FloorPlan>>

    @Query("SELECT * FROM floor_plans WHERE projectId = :projectId AND floor = :floor LIMIT 1")
    suspend fun getFloorPlanForFloor(projectId: String, floor: String): FloorPlan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFloorPlan(floorPlan: FloorPlan)

    @Update
    suspend fun updateFloorPlan(floorPlan: FloorPlan)

    // --- AI Recommendations ---
    @Query("SELECT * FROM ai_recommendations WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getRecommendationsFlow(projectId: String): Flow<List<AIRecommendation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecommendation(rec: AIRecommendation)

    @Update
    suspend fun updateRecommendation(rec: AIRecommendation)
}
