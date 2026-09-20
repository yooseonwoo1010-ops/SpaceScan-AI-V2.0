package com.example.scanner

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import com.example.data.model.*
import com.google.ar.core.ArCoreApk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

enum class TrackingState {
    NOT_INITIALIZED,
    INITIALIZING,
    TRACKING,
    PAUSED,
    TRACKING_LOST
}

data class DeviceCapability(
    val hasCamera: Boolean,
    val isArCoreSupported: Boolean,
    val hasDepthSensor: Boolean,
    val hasGps: Boolean,
    val hasCompass: Boolean,
    val hasAccelerometer: Boolean,
    val isStorageReady: Boolean = true,
    val isRendererReady: Boolean = true
)

data class LiveScanState(
    val isScanning: Boolean = false,
    val trackingState: TrackingState = TrackingState.NOT_INITIALIZED,
    val isDepthActive: Boolean = false,
    val currentPosX: Float = 0f,
    val currentPosY: Float = 1.6f, // typical eye-level height in meters
    val currentPosZ: Float = 0f,
    val yawDeg: Float = 0f,
    val pitchDeg: Float = 0f,
    val rollDeg: Float = 0f,
    val pointCount: Int = 0,
    val vertexCount: Int = 0,
    val triangleCount: Int = 0,
    val planeCount: Int = 0,
    val cameraPoseCount: Int = 0,
    val quality: String = "LOW", // HIGH, MEDIUM, LOW
    val trackingMessage: String = "대기 중",
    val boundingBox: BoundingBox = BoundingBox(),
    val isDemoMode: Boolean = false
)

class ScanEngine(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val pointCloudProcessor = PointCloudProcessor()
    private val cameraPath = mutableListOf<PoseRecord>()

    private val _deviceCapability = MutableStateFlow(checkDeviceCapability())
    val deviceCapability = _deviceCapability.asStateFlow()

    private val _scanState = MutableStateFlow(LiveScanState())
    val scanState = _scanState.asStateFlow()

    private var lastKeyframeX = 0f
    private var lastKeyframeZ = 0f
    private var lastKeyframeYaw = 0f

    // Live points cache for rendering
    private val _livePoints = MutableStateFlow<List<Point3D>>(emptyList())
    val livePoints = _livePoints.asStateFlow()

    private val _liveMesh = MutableStateFlow(MeshData())
    val liveMesh = _liveMesh.asStateFlow()

    private val _pathRecords = MutableStateFlow<List<PoseRecord>>(emptyList())
    val pathRecords = _pathRecords.asStateFlow()

    fun checkDeviceCapability(): DeviceCapability {
        val pm = context.packageManager
        val hasCamera = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        val hasGps = pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)
        val hasCompass = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null
        val hasAccel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null

        val arAvailability = try {
            ArCoreApk.getInstance().checkAvailability(context)
        } catch (e: Exception) {
            ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE
        }
        val isArSupported = arAvailability.isSupported

        return DeviceCapability(
            hasCamera = hasCamera,
            isArCoreSupported = isArSupported,
            hasDepthSensor = isArSupported, // ARCore device with Depth API support
            hasGps = hasGps,
            hasCompass = hasCompass,
            hasAccelerometer = hasAccel
        )
    }

    fun startSensors() {
        val rotSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        rotSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopSensors() {
        sensorManager.unregisterListener(this)
    }

    fun startScanning(isDemo: Boolean = false) {
        startSensors()
        cameraPath.clear()
        pointCloudProcessor.clear()
        _livePoints.value = emptyList()
        _liveMesh.value = MeshData()
        _pathRecords.value = emptyList()

        _scanState.value = _scanState.value.copy(
            isScanning = true,
            isDemoMode = isDemo,
            trackingState = TrackingState.TRACKING,
            isDepthActive = _deviceCapability.value.hasDepthSensor,
            trackingMessage = if (isDemo) "DEMO 시뮬레이션 스캔 중" else "실시간 센서 트래킹 중"
        )

        if (isDemo) {
            populateDemoSpace()
        }
    }

    fun stopScanning(): ScanSegmentData {
        stopSensors()
        val currentState = _scanState.value
        val points = pointCloudProcessor.getPoints()
        val bbox = CoordinateTransform.computeBoundingBox(points)
        val mesh = MeshReconstructor.reconstructMesh(points)

        _scanState.value = currentState.copy(
            isScanning = false,
            trackingState = TrackingState.PAUSED,
            trackingMessage = "스캔 완료 및 저장됨"
        )

        return ScanSegmentData(
            points = points,
            mesh = mesh,
            boundingBox = bbox,
            cameraPath = cameraPath.toList(),
            quality = currentState.quality
        )
    }

    /**
     * Called when a real frame delivers points from Depth / ARCore / Feature detector.
     */
    fun onNewFrame(
        incomingPoints: List<Point3D>,
        posX: Float,
        posY: Float,
        posZ: Float,
        yaw: Float,
        pitch: Float,
        roll: Float
    ) {
        if (!_scanState.value.isScanning) return

        val added = pointCloudProcessor.addPoints(incomingPoints)
        val pose = PoseRecord(posX, posY, posZ, pitch, yaw, roll)
        cameraPath.add(pose)
        _pathRecords.value = cameraPath.takeLast(100)

        val totalPoints = pointCloudProcessor.pointCount()
        val pointsList = pointCloudProcessor.getPoints()
        val bbox = CoordinateTransform.computeBoundingBox(pointsList)

        // Reconstruct mesh periodically or on point milestones
        if (totalPoints % 15 == 0 || totalPoints < 100) {
            _liveMesh.value = MeshReconstructor.reconstructMesh(pointsList)
        }
        _livePoints.value = pointsList

        val quality = when {
            totalPoints > 5000 -> "HIGH"
            totalPoints > 800 -> "MEDIUM"
            else -> "LOW"
        }

        _scanState.value = _scanState.value.copy(
            currentPosX = posX,
            currentPosY = posY,
            currentPosZ = posZ,
            yawDeg = yaw,
            pitchDeg = pitch,
            rollDeg = roll,
            pointCount = totalPoints,
            vertexCount = _liveMesh.value.vertices.size / 3,
            triangleCount = _liveMesh.value.indices.size / 3,
            cameraPoseCount = cameraPath.size,
            boundingBox = bbox,
            quality = quality
        )
    }

    /**
     * Demo mode initialization for emulator/testing without corrupting real projects.
     */
    private fun populateDemoSpace() {
        val demoPoints = mutableListOf<Point3D>()
        // Generate rectangular classroom space (width: 8m, length: 12m, height: 2.8m)
        val xLen = 8f
        val zLen = 12f
        val h = 2.8f

        // Floor and walls points
        var x = -xLen / 2
        while (x <= xLen / 2) {
            var z = -zLen / 2
            while (z <= zLen / 2) {
                // Floor
                demoPoints.add(Point3D(x, 0f, z, 0.95f))
                // Ceiling
                demoPoints.add(Point3D(x, h, z, 0.85f))
                z += 0.5f
            }
            // North & South walls
            var y = 0f
            while (y <= h) {
                demoPoints.add(Point3D(x, y, -zLen / 2, 0.9f))
                demoPoints.add(Point3D(x, y, zLen / 2, 0.9f))
                y += 0.4f
            }
            x += 0.5f
        }

        // East & West walls
        var z = -zLen / 2
        while (z <= zLen / 2) {
            var y = 0f
            while (y <= h) {
                demoPoints.add(Point3D(-xLen / 2, y, z, 0.9f))
                demoPoints.add(Point3D(xLen / 2, y, z, 0.9f))
                y += 0.4f
            }
            z += 0.5f
        }

        pointCloudProcessor.addPoints(demoPoints)
        val points = pointCloudProcessor.getPoints()
        _livePoints.value = points
        _liveMesh.value = MeshReconstructor.reconstructMesh(points)
        val bbox = CoordinateTransform.computeBoundingBox(points)

        _scanState.value = _scanState.value.copy(
            pointCount = points.size,
            vertexCount = _liveMesh.value.vertices.size / 3,
            triangleCount = _liveMesh.value.indices.size / 3,
            boundingBox = bbox,
            quality = "HIGH"
        )
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)

            val azimuthDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
            val pitchDeg = Math.toDegrees(orientation[1].toDouble()).toFloat()
            val rollDeg = Math.toDegrees(orientation[2].toDouble()).toFloat()

            _scanState.value = _scanState.value.copy(
                yawDeg = azimuthDeg,
                pitchDeg = pitchDeg,
                rollDeg = rollDeg
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

data class ScanSegmentData(
    val points: List<Point3D>,
    val mesh: MeshData,
    val boundingBox: BoundingBox,
    val cameraPath: List<PoseRecord>,
    val quality: String
)
