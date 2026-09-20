package com.example.viewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import kotlin.math.*

@Composable
fun ThreeDViewerCanvas(
    modifier: Modifier = Modifier,
    points: List<Point3D>,
    mesh: MeshData,
    boundingBox: BoundingBox,
    scanImages: List<ScanImage> = emptyList(),
    currentFloor: String = "전체",
    onMeasureComplete: ((Float) -> Unit)? = null
) {
    // 3D Camera Orbit controls
    var cameraYaw by remember { mutableFloatStateOf(35f) }
    var cameraPitch by remember { mutableFloatStateOf(25f) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Measurement mode
    var isMeasuring by remember { mutableStateOf(false) }
    var measuredPoints by remember { mutableStateOf(listOf<Point3D>()) }
    var measuredDistance by remember { mutableStateOf<Float?>(null) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        zoomScale = (zoomScale * zoomChange).coerceIn(0.4f, 5.0f)
        panOffset += panChange
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .transformable(transformState)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        cameraYaw += dragAmount.x * 0.4f
                        cameraPitch = (cameraPitch - dragAmount.y * 0.4f).coerceIn(-85f, 85f)
                    }
                }
                .pointerInput(isMeasuring) {
                    detectTapGestures { tapOffset ->
                        if (isMeasuring && points.isNotEmpty()) {
                            // Find nearest 3D point projected onto screen
                            val tapped3D = findNearest3DPoint(tapOffset, points, cameraYaw, cameraPitch, zoomScale, panOffset, size.width, size.height)
                            tapped3D?.let { pt ->
                                val newPts = measuredPoints + pt
                                if (newPts.size == 2) {
                                    val dist = calculateDistance(newPts[0], newPts[1])
                                    measuredDistance = dist
                                    measuredPoints = newPts
                                    onMeasureComplete?.invoke(dist)
                                } else if (newPts.size > 2) {
                                    measuredPoints = listOf(pt)
                                    measuredDistance = null
                                } else {
                                    measuredPoints = newPts
                                }
                            }
                        }
                    }
                }
        ) {
            val center = Offset(size.width / 2f + panOffset.x, size.height / 2f + panOffset.y)
            val baseScale = 45f * zoomScale

            val yawRad = Math.toRadians(cameraYaw.toDouble())
            val pitchRad = Math.toRadians(cameraPitch.toDouble())
            val cosY = cos(yawRad).toFloat()
            val sinY = sin(yawRad).toFloat()
            val cosP = cos(pitchRad).toFloat()
            val sinP = sin(pitchRad).toFloat()

            // 1. Draw 3D Ground Coordinate Axis Grid
            draw3DGrid(center, baseScale, cosY, sinY, cosP, sinP)

            // 2. Project & Draw Mesh Triangles
            val v = mesh.vertices
            val ind = mesh.indices
            if (v.size >= 9 && ind.isNotEmpty()) {
                for (i in 0 until ind.size step 3) {
                    if (i + 2 >= ind.size) break
                    val i0 = ind[i] * 3
                    val i1 = ind[i + 1] * 3
                    val i2 = ind[i + 2] * 3

                    if (i0 + 2 < v.size && i1 + 2 < v.size && i2 + 2 < v.size) {
                        val p0 = project3D(v[i0], v[i0 + 1], v[i0 + 2], center, baseScale, cosY, sinY, cosP, sinP)
                        val p1 = project3D(v[i1], v[i1 + 1], v[i1 + 2], center, baseScale, cosY, sinY, cosP, sinP)
                        val p2 = project3D(v[i2], v[i2 + 1], v[i2 + 2], center, baseScale, cosY, sinY, cosP, sinP)

                        val trianglePath = Path().apply {
                            moveTo(p0.x, p0.y)
                            lineTo(p1.x, p1.y)
                            lineTo(p2.x, p2.y)
                            close()
                        }
                        // Semi-transparent surface fill
                        drawPath(trianglePath, color = PrimaryBlue.copy(alpha = 0.18f))
                        // Clean wireframe edge
                        drawPath(trianglePath, color = AccentCyan.copy(alpha = 0.45f), style = Stroke(width = 1f))
                    }
                }
            }

            // 3. Project & Draw Point Cloud
            for (p in points) {
                val proj = project3D(p.x, p.y, p.z, center, baseScale, cosY, sinY, cosP, sinP)
                drawCircle(
                    color = StatusGreen.copy(alpha = 0.85f),
                    radius = 2.5f * zoomScale.coerceIn(0.7f, 2.0f),
                    center = proj
                )
            }

            // 4. Project & Draw Measured Points & Line
            if (measuredPoints.isNotEmpty()) {
                val projPts = measuredPoints.map {
                    project3D(it.x, it.y, it.z, center, baseScale, cosY, sinY, cosP, sinP)
                }
                for (pt in projPts) {
                    drawCircle(color = StatusYellow, radius = 6f, center = pt)
                }
                if (projPts.size == 2) {
                    drawLine(
                        color = StatusYellow,
                        start = projPts[0],
                        end = projPts[1],
                        strokeWidth = 3f
                    )
                }
            }
        }

        // Top Info Badge: Bounding dimensions (X, Y, Z in true meters)
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(DarkSurface.copy(alpha = 0.85f), shape = MaterialTheme.shapes.medium)
                .padding(12.dp)
        ) {
            Text("3D 모델 메트릭 정보", color = TextPrimary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "가로 (X): %.2fm | 세로 (Z): %.2fm | 높이 (Y): %.2fm".format(
                    boundingBox.width, boundingBox.depth, boundingBox.height
                ),
                color = AccentCyan,
                fontSize = 11.sp
            )
            Text(
                "포인트: ${points.size} | 정점: ${mesh.vertices.size / 3} | 면: ${mesh.indices.size / 3}",
                color = TextSecondary,
                fontSize = 10.sp
            )
            measuredDistance?.let { dist ->
                Spacer(modifier = Modifier.height(4.dp))
                Text("측정 거리: %.2fm (실제 비율)".format(dist), color = StatusYellow, fontSize = 12.sp)
            }
        }

        // Bottom Controls Toolbar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .background(DarkSurface.copy(alpha = 0.9f), shape = MaterialTheme.shapes.medium)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                cameraPitch = 0f
                cameraYaw = 0f
                zoomScale = 1.0f
                panOffset = Offset.Zero
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "초기화", tint = TextPrimary)
            }
            IconButton(onClick = {
                cameraPitch = 85f // Top-down
                cameraYaw = 0f
            }) {
                Icon(Icons.Default.VerticalAlignTop, contentDescription = "Top 뷰", tint = TextPrimary)
            }
            IconButton(onClick = {
                cameraPitch = 10f
                zoomScale = 2.0f
            }) {
                Icon(Icons.Default.Visibility, contentDescription = "1인칭", tint = TextPrimary)
            }
            FilledTonalButton(
                onClick = {
                    isMeasuring = !isMeasuring
                    if (!isMeasuring) {
                        measuredPoints = emptyList()
                        measuredDistance = null
                    }
                },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (isMeasuring) StatusYellow else DarkSurfaceElevated,
                    contentColor = if (isMeasuring) DarkBackground else TextPrimary
                )
            ) {
                Icon(Icons.Default.Straighten, contentDescription = "측정")
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isMeasuring) "측정 중" else "거리 측정")
            }
        }
    }
}

private fun project3D(
    x: Float, y: Float, z: Float,
    center: Offset, scale: Float,
    cosY: Float, sinY: Float,
    cosP: Float, sinP: Float
): Offset {
    // 1. Rotate around Y (yaw)
    val x1 = x * cosY + z * sinY
    val z1 = -x * sinY + z * cosY

    // 2. Rotate around X (pitch)
    val y2 = y * cosP - z1 * sinP
    val z2 = y * sinP + z1 * cosP

    // 3. Orthographic / weak perspective projection
    val px = center.x + x1 * scale
    val py = center.y - y2 * scale
    return Offset(px, py)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.draw3DGrid(
    center: Offset, scale: Float,
    cosY: Float, sinY: Float,
    cosP: Float, sinP: Float
) {
    val gridColor = Color(0xFF1E293B)
    for (i in -4..4) {
        val f = i.toFloat()
        val startX = project3D(f, 0f, -4f, center, scale, cosY, sinY, cosP, sinP)
        val endX = project3D(f, 0f, 4f, center, scale, cosY, sinY, cosP, sinP)
        drawLine(gridColor, startX, endX, strokeWidth = 1f)

        val startZ = project3D(-4f, 0f, f, center, scale, cosY, sinY, cosP, sinP)
        val endZ = project3D(4f, 0f, f, center, scale, cosY, sinY, cosP, sinP)
        drawLine(gridColor, startZ, endZ, strokeWidth = 1f)
    }
}

private fun findNearest3DPoint(
    tap: Offset,
    points: List<Point3D>,
    yaw: Float, pitch: Float,
    zoomScale: Float, panOffset: Offset,
    w: Int, h: Int
): Point3D? {
    val center = Offset(w / 2f + panOffset.x, h / 2f + panOffset.y)
    val baseScale = 45f * zoomScale
    val yawRad = Math.toRadians(yaw.toDouble())
    val pitchRad = Math.toRadians(pitch.toDouble())
    val cosY = cos(yawRad).toFloat()
    val sinY = sin(yawRad).toFloat()
    val cosP = cos(pitchRad).toFloat()
    val sinP = sin(pitchRad).toFloat()

    var closest: Point3D? = null
    var minD = 45f // 45px tap radius threshold
    for (p in points) {
        val proj = project3D(p.x, p.y, p.z, center, baseScale, cosY, sinY, cosP, sinP)
        val d = (tap - proj).getDistance()
        if (d < minD) {
            minD = d
            closest = p
        }
    }
    return closest
}

private fun calculateDistance(p1: Point3D, p2: Point3D): Float {
    val dx = p1.x - p2.x
    val dy = p1.y - p2.y
    val dz = p1.z - p2.z
    return sqrt(dx * dx + dy * dy + dz * dz)
}
