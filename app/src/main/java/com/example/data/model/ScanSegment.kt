package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_segments")
data class ScanSegment(
    @PrimaryKey val id: String,
    val projectId: String,
    val floor: String,
    val pointCount: Int,
    val vertexCount: Int,
    val triangleCount: Int,
    val coverage: Float, // 0.0 - 1.0
    val quality: String, // HIGH, MEDIUM, LOW
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float,
    val minZ: Float,
    val maxZ: Float,
    val pointsJson: String = "", // serialized sample points or binary file path
    val meshJson: String = "",   // serialized mesh vertices and triangles
    val cameraPathJson: String = "", // serialized PoseRecord list
    val timestamp: Long = System.currentTimeMillis()
)
