package com.example.data.model

data class Point3D(
    val x: Float,
    val y: Float,
    val z: Float,
    val confidence: Float = 1.0f,
    val color: Int = 0xFF38BDF8.toInt()
)

data class BoundingBox(
    val minX: Float = 0f,
    val maxX: Float = 0f,
    val minY: Float = 0f,
    val maxY: Float = 0f,
    val minZ: Float = 0f,
    val maxZ: Float = 0f
) {
    val width: Float get() = (maxX - minX).coerceAtLeast(0f)
    val height: Float get() = (maxY - minY).coerceAtLeast(0f)
    val depth: Float get() = (maxZ - minZ).coerceAtLeast(0f)
}

data class MeshData(
    val vertices: List<Float> = emptyList(), // x, y, z triplets
    val indices: List<Int> = emptyList(),    // triangle indices
    val normals: List<Float> = emptyList()
)

data class PoseRecord(
    val x: Float,
    val y: Float,
    val z: Float,
    val pitch: Float,
    val yaw: Float,
    val roll: Float,
    val timestamp: Long = System.currentTimeMillis()
)
