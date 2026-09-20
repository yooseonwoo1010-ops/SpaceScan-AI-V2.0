package com.example.mapping

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import kotlin.math.*

@Composable
fun IndoorMapCanvas(
    modifier: Modifier = Modifier,
    points: List<Point3D>,
    cameraPath: List<PoseRecord>,
    currentX: Float,
    currentZ: Float,
    currentYaw: Float,
    aiRecommendation: AIRecommendation?,
    scanImages: List<ScanImage>,
    floorPlan: FloorPlan? = null,
    onImageSelected: ((ScanImage) -> Unit)? = null,
    onManualLocationSet: ((Float, Float) -> Unit)? = null
) {
    // Zoom scale from 0.25x to 8.0x
    var scale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.25f, 8.0f)
        panOffset += panChange
    }

    Box(
        modifier = modifier
            .background(DarkBackground)
            .fillMaxSize()
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .transformable(state = transformableState)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            scale = if (scale > 1.5f) 1.0f else 2.5f
                        },
                        onTap = { tapOffset ->
                            onManualLocationSet?.invoke(tapOffset.x, tapOffset.y)
                        }
                    )
                }
        ) {
            val center = Offset(size.width / 2f + panOffset.x, size.height / 2f + panOffset.y)
            val baseMeterToPx = 35f * scale

            // 1. Grid lines (1 meter intervals)
            drawSurveyGrid(center, baseMeterToPx, size)

            // 2. Floor plan boundary / rooms if present
            floorPlan?.let { plan ->
                drawFloorPlanOutline(center, plan, baseMeterToPx)
            }

            // 3. Scanned points top-down projection
            for (p in points) {
                val px = center.x + p.x * baseMeterToPx
                val py = center.y - p.z * baseMeterToPx // Z is forward/north
                drawCircle(
                    color = StatusGreen.copy(alpha = 0.65f),
                    radius = 3.5f * scale.coerceIn(0.8f, 2.5f),
                    center = Offset(px, py)
                )
            }

            // 4. Camera movement path
            if (cameraPath.size > 1) {
                val path = Path()
                for (i in cameraPath.indices) {
                    val pose = cameraPath[i]
                    val px = center.x + pose.x * baseMeterToPx
                    val py = center.y - pose.z * baseMeterToPx
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                drawPath(
                    path = path,
                    color = AccentCyan.copy(alpha = 0.8f),
                    style = Stroke(width = 2.5f * scale.coerceIn(0.8f, 2.0f))
                )
            }

            // 5. Scan image keyframe icons (📷)
            for (img in scanImages) {
                val imgPx = center.x + img.worldX * baseMeterToPx
                val imgPy = center.y - img.worldZ * baseMeterToPx
                drawCircle(
                    color = StatusBlue,
                    radius = 7f * scale.coerceIn(0.8f, 2.0f),
                    center = Offset(imgPx, imgPy)
                )
                drawCircle(
                    color = Color.White,
                    radius = 3f * scale.coerceIn(0.8f, 2.0f),
                    center = Offset(imgPx, imgPy)
                )
            }

            // 6. AI Recommendation target & route (Purple)
            aiRecommendation?.let { rec ->
                val targetPx = center.x + rec.targetWorldX * baseMeterToPx
                val targetPy = center.y - rec.targetWorldZ * baseMeterToPx
                val currentPx = center.x + currentX * baseMeterToPx
                val currentPy = center.y - currentZ * baseMeterToPx

                // Dotted/solid route line
                drawLine(
                    color = StatusPurple,
                    start = Offset(currentPx, currentPy),
                    end = Offset(targetPx, targetPy),
                    strokeWidth = 3f * scale.coerceIn(0.8f, 2.0f)
                )

                // Target star marker
                drawCircle(
                    color = StatusPurple,
                    radius = 9f * scale.coerceIn(0.8f, 2.0f),
                    center = Offset(targetPx, targetPy)
                )
                drawCircle(
                    color = Color.White,
                    radius = 4f * scale.coerceIn(0.8f, 2.0f),
                    center = Offset(targetPx, targetPy)
                )
            }

            // 7. Current Position and Camera Orientation (●━━━━► Cyan)
            val currPx = center.x + currentX * baseMeterToPx
            val currPy = center.y - currentZ * baseMeterToPx

            // Heading ray
            val headingRad = Math.toRadians((currentYaw - 90).toDouble())
            val arrowLength = 28f * scale.coerceIn(0.8f, 2.0f)
            val arrowEndX = currPx + (cos(headingRad) * arrowLength).toFloat()
            val arrowEndY = currPy + (sin(headingRad) * arrowLength).toFloat()

            drawLine(
                color = StatusCyan,
                start = Offset(currPx, currPy),
                end = Offset(arrowEndX, arrowEndY),
                strokeWidth = 4f
            )

            // Current position dot
            drawCircle(
                color = StatusCyan,
                radius = 8f * scale.coerceIn(0.8f, 2.0f),
                center = Offset(currPx, currPy)
            )
            drawCircle(
                color = Color.White,
                radius = 4f * scale.coerceIn(0.8f, 2.0f),
                center = Offset(currPx, currPy)
            )
        }

        // Overlay status badge
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .background(DarkSurface.copy(alpha = 0.85f), shape = MaterialTheme.shapes.small)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "확대 ${(scale * 100).toInt()}%",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

private fun DrawScope.drawSurveyGrid(center: Offset, meterToPx: Float, size: Size) {
    val gridColor = Color(0xFF1E293B)
    var x = center.x % meterToPx
    while (x < size.width) {
        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        x += meterToPx
    }
    var y = center.y % meterToPx
    while (y < size.height) {
        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += meterToPx
    }
}

private fun DrawScope.drawFloorPlanOutline(center: Offset, plan: FloorPlan, meterToPx: Float) {
    val halfW = (plan.widthMeters / 2f) * meterToPx
    val halfH = (plan.heightMeters / 2f) * meterToPx

    drawRect(
        color = Color(0xFF334155).copy(alpha = plan.opacity),
        topLeft = Offset(center.x - halfW, center.y - halfH),
        size = Size(halfW * 2f, halfH * 2f),
        style = Stroke(width = 2.5f)
    )

    // Draw corridor divider
    drawLine(
        color = Color(0xFF475569).copy(alpha = plan.opacity),
        start = Offset(center.x - halfW, center.y),
        end = Offset(center.x + halfW, center.y),
        strokeWidth = 2f
    )
}
