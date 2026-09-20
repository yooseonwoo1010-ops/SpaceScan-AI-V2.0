package com.example.scanner

import com.example.data.model.BoundingBox
import com.example.data.model.Point3D
import kotlin.math.*

object CoordinateTransform {

    /**
     * Converts raw depth pixel into Camera 3D coordinates using camera intrinsics.
     * Metric unit: meters.
     */
    fun depthToCamera3D(
        depthMeters: Float,
        uPx: Float,
        vPx: Float,
        fx: Float,
        fy: Float,
        cx: Float,
        cy: Float
    ): Point3D {
        if (depthMeters <= 0.1f || depthMeters > 15f) return Point3D(0f, 0f, 0f, confidence = 0f)
        val x = (uPx - cx) * depthMeters / fx
        val y = (vPx - cy) * depthMeters / fy
        val z = depthMeters
        return Point3D(x, y, z, confidence = 1.0f)
    }

    /**
     * Transforms point in Camera coordinates into World 3D coordinates
     * using the 4x4 camera pose transformation matrix or camera position + rotation.
     */
    fun cameraToWorld3D(
        camPoint: Point3D,
        camPosX: Float,
        camPosY: Float,
        camPosZ: Float,
        yawDeg: Float,
        pitchDeg: Float
    ): Point3D {
        val yawRad = Math.toRadians(yawDeg.toDouble())
        val pitchRad = Math.toRadians(pitchDeg.toDouble())

        // Rotation around Y (yaw) and X (pitch)
        val cosY = cos(yawRad).toFloat()
        val sinY = sin(yawRad).toFloat()
        val cosP = cos(pitchRad).toFloat()
        val sinP = sin(pitchRad).toFloat()

        // Apply pitch (around X)
        val y1 = camPoint.y * cosP - camPoint.z * sinP
        val z1 = camPoint.y * sinP + camPoint.z * cosP

        // Apply yaw (around Y)
        val xWorld = camPoint.x * cosY + z1 * sinY + camPosX
        val yWorld = y1 + camPosY
        val zWorld = -camPoint.x * sinY + z1 * cosY + camPosZ

        return Point3D(xWorld, yWorld, zWorld, confidence = camPoint.confidence, color = camPoint.color)
    }

    /**
     * Projects 3D World point to 2D Top-down Map coordinate (meters from origin).
     */
    fun worldToMap2D(worldPoint: Point3D): Pair<Float, Float> {
        // Top-down map: X represents East/Right, Z represents North/Forward (Y is height)
        return Pair(worldPoint.x, worldPoint.z)
    }

    /**
     * Projects 3D World point to Floor Plan 2D coordinates.
     */
    fun worldToFloorPlan2D(
        worldPoint: Point3D,
        planOffsetX: Float,
        planOffsetY: Float,
        planScale: Float,
        planRotationDeg: Float
    ): Pair<Float, Float> {
        val rad = Math.toRadians(planRotationDeg.toDouble())
        val cosR = cos(rad).toFloat()
        val sinR = sin(rad).toFloat()

        val rx = (worldPoint.x - planOffsetX) * cosR - (worldPoint.z - planOffsetY) * sinR
        val ry = (worldPoint.x - planOffsetX) * sinR + (worldPoint.z - planOffsetY) * cosR

        return Pair(rx * planScale, ry * planScale)
    }

    /**
     * Converts Local World 3D position (in meters from origin) to GPS/Geo coordinates (WGS84).
     */
    fun worldToGeo(
        worldX: Float,
        worldZ: Float,
        originLat: Double,
        originLng: Double,
        headingDeg: Float = 0f
    ): Pair<Double, Double> {
        val rad = Math.toRadians(headingDeg.toDouble())
        val eastMeters = worldX * cos(rad) - worldZ * sin(rad)
        val northMeters = worldX * sin(rad) + worldZ * cos(rad)

        val deltaLat = northMeters / 111320.0
        val deltaLng = eastMeters / (111320.0 * cos(Math.toRadians(originLat)).coerceAtLeast(0.01))

        return Pair(originLat + deltaLat, originLng + deltaLng)
    }

    /**
     * Converts GPS/Geo coordinates to Local World 3D (meters relative to origin).
     */
    fun geoToWorld(
        targetLat: Double,
        targetLng: Double,
        originLat: Double,
        originLng: Double,
        headingDeg: Float = 0f
    ): Pair<Float, Float> {
        val deltaLat = targetLat - originLat
        val deltaLng = targetLng - originLng

        val northMeters = deltaLat * 111320.0
        val eastMeters = deltaLng * 111320.0 * cos(Math.toRadians(originLat))

        val rad = Math.toRadians(-headingDeg.toDouble())
        val worldX = (eastMeters * cos(rad) - northMeters * sin(rad)).toFloat()
        val worldZ = (eastMeters * sin(rad) + northMeters * cos(rad)).toFloat()

        return Pair(worldX, worldZ)
    }

    /**
     * Calculates tight bounding box over points preserving actual real-world meters.
     */
    fun computeBoundingBox(points: List<Point3D>): BoundingBox {
        if (points.isEmpty()) return BoundingBox()
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE

        for (p in points) {
            if (p.x < minX) minX = p.x
            if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y
            if (p.y > maxY) maxY = p.y
            if (p.z < minZ) minZ = p.z
            if (p.z > maxZ) maxZ = p.z
        }
        return BoundingBox(minX, maxX, minY, maxY, minZ, maxZ)
    }
}
