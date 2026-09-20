package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.example.data.model.GeoAnchor
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

data class FusedLocationPose(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val accuracyMeters: Float = 0.0f,
    val heading: Float = 0f,
    val speed: Float = 0f,
    val hasFix: Boolean = false,
    val source: String = "NONE", // GPS, FUSED, GEOSPATIAL, LOCAL_ANCHOR
    val confidence: Float = 0f,
    val isTrackingLost: Boolean = false,
    val recoveryStatus: String = "정상"
)

class LocationEngine(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _locationPose = MutableStateFlow(FusedLocationPose())
    val locationPose = _locationPose.asStateFlow()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location: Location = result.lastLocation ?: return
            val current = _locationPose.value
            _locationPose.value = current.copy(
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                accuracyMeters = location.accuracy,
                heading = if (location.hasBearing()) location.bearing else current.heading,
                speed = location.speed,
                hasFix = true,
                source = "GPS / FUSED GNSS",
                confidence = if (location.accuracy < 5f) 0.95f else 0.75f,
                isTrackingLost = false,
                recoveryStatus = "GPS 정상 수신"
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        try {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
                .setMinUpdateIntervalMillis(1000L)
                .setMinUpdateDistanceMeters(0.5f)
                .build()

            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            _locationPose.value = _locationPose.value.copy(
                hasFix = false,
                recoveryStatus = "위치 권한 필요"
            )
        }
    }

    fun stopLocationUpdates() {
        fusedClient.removeLocationUpdates(locationCallback)
    }

    /**
     * Attempts recovery using the nearest GeoAnchor if GPS / AR tracking is lost.
     */
    fun attemptRelocalization(anchors: List<GeoAnchor>, localX: Float, localZ: Float): GeoAnchor? {
        if (anchors.isEmpty()) {
            _locationPose.value = _locationPose.value.copy(
                isTrackingLost = true,
                recoveryStatus = "저장된 앵커 없음 - GPS/IMU 대기 중"
            )
            return null
        }

        // Find nearest anchor in local space
        var bestAnchor: GeoAnchor? = null
        var minDistSq = Float.MAX_VALUE
        for (a in anchors) {
            val dx = localX - a.localX
            val dz = localZ - a.localZ
            val distSq = dx * dx + dz * dz
            if (distSq < minDistSq) {
                minDistSq = distSq
                bestAnchor = a
            }
        }

        val nearest = bestAnchor
        if (nearest != null && minDistSq < 400f) { // Within 20m radius
            _locationPose.value = _locationPose.value.copy(
                latitude = nearest.latitude,
                longitude = nearest.longitude,
                altitude = nearest.altitude,
                heading = nearest.heading,
                hasFix = true,
                source = "GEO_ANCHOR (${nearest.title})",
                confidence = 0.9f,
                isTrackingLost = false,
                recoveryStatus = "앵커 [${nearest.title}] 기반 위치 복구 완료"
            )
            return nearest
        } else {
            _locationPose.value = _locationPose.value.copy(
                isTrackingLost = true,
                recoveryStatus = "위치 추적 불안정: 기존 스캔 위치 매칭 중..."
            )
            return null
        }
    }

    fun setDemoLocation(lat: Double = 37.498095, lng: Double = 127.027610) {
        _locationPose.value = FusedLocationPose(
            latitude = lat,
            longitude = lng,
            altitude = 38.5,
            accuracyMeters = 0.5f,
            heading = 45f,
            hasFix = true,
            source = "DEMO SIMULATOR",
            confidence = 1.0f,
            isTrackingLost = false,
            recoveryStatus = "데모 GPS 시뮬레이션 활성"
        )
    }
}
