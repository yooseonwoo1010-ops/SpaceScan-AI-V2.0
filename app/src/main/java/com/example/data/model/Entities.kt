package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_images")
data class ScanImage(
    @PrimaryKey val id: String,
    val projectId: String,
    val scanSegmentId: String,
    val imageUri: String,
    val timestamp: Long = System.currentTimeMillis(),
    val worldX: Float,
    val worldY: Float,
    val worldZ: Float,
    val yaw: Float,
    val pitch: Float,
    val mapX: Float,
    val mapY: Float,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val floor: String = "1F",
    val roomId: String = "",
    val quality: String = "HIGH" // HIGH, MEDIUM, LOW
)

@Entity(tableName = "geo_anchors")
data class GeoAnchor(
    @PrimaryKey val id: String,
    val projectId: String,
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val heading: Float,
    val localX: Float,
    val localY: Float,
    val localZ: Float,
    val scanSegmentId: String = "",
    val imageId: String = "",
    val accuracyMeters: Float = 1.0f,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "floor_plans")
data class FloorPlan(
    @PrimaryKey val id: String,
    val projectId: String,
    val floor: String,
    val name: String,
    val sourceType: String, // CAMERA, IMAGE, PDF, WEB_SEARCH
    val sourceUrl: String = "",
    val imageUri: String = "",
    val widthMeters: Float = 30f,
    val heightMeters: Float = 20f,
    val scaleFactor: Float = 1.0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotationDegrees: Float = 0f,
    val opacity: Float = 0.5f,
    val georeferenced: Boolean = false,
    val recognizedRoomsCount: Int = 0,
    val recognizedCorridorsCount: Int = 0,
    val confidence: Float = 0.85f,
    val featuresJson: String = "" // List of Room/Feature bounding boxes
)

@Entity(tableName = "ai_recommendations")
data class AIRecommendation(
    @PrimaryKey val id: String,
    val projectId: String,
    val floor: String,
    val title: String,
    val reason: String,
    val targetRoom: String,
    val distanceMeters: Float,
    val priority: String, // HIGH, MEDIUM, LOW
    val targetWorldX: Float,
    val targetWorldZ: Float,
    val isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
