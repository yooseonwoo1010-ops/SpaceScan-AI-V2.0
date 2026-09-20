package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey val id: String,
    val name: String,
    val buildingName: String,
    val address: String,
    val description: String = "",
    val floorCount: Int = 1,
    val mode: String = "INDOOR_OUTDOOR", // INDOOR, OUTDOOR, INDOOR_OUTDOOR
    val status: String = "ACTIVE",
    val geoOriginLat: Double = 0.0,
    val geoOriginLng: Double = 0.0,
    val currentFloor: String = "1F",
    val scanProgress: Int = 0,     // 0..100%
    val mapProgress: Int = 0,      // 0..100%
    val threeDProgress: Int = 0,   // 0..100%
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDemo: Boolean = false
)
