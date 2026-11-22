package com.kylecorry.trail_sense.tools.augmented_reality.infrastructure

import android.content.Context
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import android.view.Surface
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.sol.math.Vector3
import com.kylecorry.trail_sense.tools.augmented_reality.domain.ground.FeatureTracker
import com.kylecorry.trail_sense.tools.augmented_reality.domain.ground.GroundTrackingState
import com.kylecorry.trail_sense.tools.augmented_reality.domain.ground.GroundTrackingStatus
import com.kylecorry.trail_sense.tools.augmented_reality.domain.ground.OccupancyGrid
import com.kylecorry.trail_sense.tools.augmented_reality.domain.ground.PinholeProjector
import com.kylecorry.trail_sense.tools.augmented_reality.domain.ground.ScaleEstimator
import com.kylecorry.trail_sense.tools.augmented_reality.domain.ground.VisualGroundOdometry
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

/**
 * Maps the ground and anything on it from camera frames and the accelerometer. The camera gives
 * motion in units of its height above the ground, and the accelerometer turns that into meters once
 * the phone moves (walking is enough). Until then a typical holding height is assumed.
 */
class GroundTracker(context: Context) : SensorEventListener {

    var onUpdate: ((GroundTrackingState) -> Unit)? = null

    private var executor: ExecutorService? = null
    private val busy = AtomicBoolean(false)
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val projector = PinholeProjector()
    private val scale = ScaleEstimator()
    private val map = OccupancyGrid()
    private var odometry: VisualGroundOdometry? = null

    // The camera's height in meters when tracking started, which is the odometry's unit
    private var unitHeight = DEFAULT_CAMERA_HEIGHT
    private var heightWeight = 0f
    private var lastScaleEstimate = 0L
    private var lostFrames = 0
    private var lastPose: Pair<Vector3, Long>? = null

    @Volatile
    private var rotation = FloatArray(16)

    fun start() {
        stop()
        odometry = null
        synchronized(scale) { scale.reset() }
        map.clear()
        unitHeight = DEFAULT_CAMERA_HEIGHT
        heightWeight = 0f
        lastScaleEstimate = 0L
        lostFrames = 0
        lastPose = null
        executor = Executors.newSingleThreadExecutor()
        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)?.let {
            sensorManager.registerListener(this, it, SENSOR_PERIOD_MICROS)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        executor?.shutdownNow()
        executor = null
        busy.set(false)
    }

    /**
     * @param rotation the AR view's rotation matrix when the frame arrived
     */
    fun onFrame(frame: Bitmap, rotation: FloatArray, horizontalFov: Float, verticalFov: Float) {
        this.rotation = rotation
        val executor = executor ?: return
        if (!busy.compareAndSet(false, true)) return
        val time = SystemClock.elapsedRealtimeNanos()
        // Frames arrive on the main thread, so convert them in the background
        executor.execute {
            try {
                val image = toImage(frame)
                projector.update(image.width, image.height, horizontalFov, verticalFov)
                process(image, rotation, time)
            } finally {
                busy.set(false)
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val world = PinholeProjector.toWorld(toCameraAxes(event.values), rotation)
        synchronized(scale) { scale.addAcceleration(event.timestamp / 1e9, world) }
    }

    // The device's axes turned to match the screen, with z pointing into the scene like the camera
    private fun toCameraAxes(values: FloatArray): Vector3 {
        val (x, y, z) = values
        return when (MainActivity.deviceSurfaceRotation) {
            Surface.ROTATION_90 -> Vector3(-y, x, -z)
            Surface.ROTATION_180 -> Vector3(-x, -y, -z)
            Surface.ROTATION_270 -> Vector3(y, -x, -z)
            else -> Vector3(x, y, -z)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }

    private fun toImage(bitmap: Bitmap): FeatureTracker.Image {
        val scaled = Bitmap.createScaledBitmap(bitmap, WIDTH, WIDTH * bitmap.height / bitmap.width, true)
        val pixels = IntArray(scaled.width * scaled.height)
        scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
        if (scaled != bitmap) scaled.recycle()
        // The green channel is a good enough brightness
        return FeatureTracker.Image(
            FloatArray(pixels.size) { ((pixels[it] shr 8) and 0xff).toFloat() },
            WIDTH,
            pixels.size / WIDTH
        )
    }

    private fun process(image: FeatureTracker.Image, rotation: FloatArray, time: Long) {
        val vio = odometry ?: VisualGroundOdometry(projector, image.width, image.height).also { odometry = it }
        val output = vio.process(image, rotation)
        val position = output.position
        if (position == null) {
            lostFrames++
            lastPose = null
            publish(if (lostFrames > LOST_FRAMES) GroundTrackingStatus.Lost else GroundTrackingStatus.Searching, null, time)
            return
        }

        lostFrames = 0
        synchronized(scale) { scale.addVision(time / 1e9, position) }
        updateHeight(time)

        val metric = position * unitHeight
        map.add(metric, output.points.mapNotNull { p -> p.world?.let { p.id to it * unitHeight } }.toMap())
        publish(GroundTrackingStatus.Tracking, position, time)
    }

    // Each estimate covers the last few seconds and they overlap, so take one a second and combine
    // them by how sure each is, slowly forgetting old ones in case the phone is held differently
    private fun updateHeight(time: Long) {
        if (time - lastScaleEstimate < SCALE_INTERVAL_NANOS) return
        lastScaleEstimate = time
        val estimate = synchronized(scale) { scale.estimate() } ?: return
        if (estimate.relativeError > MAX_SCALE_ERROR) return
        val before = unitHeight
        val weight = 1f / (estimate.relativeError * estimate.relativeError)
        val old = heightWeight * HEIGHT_MEMORY
        unitHeight = if (old == 0f) estimate.height else (unitHeight * old + estimate.height * weight) / (old + weight)
        heightWeight = (old + weight).coerceAtMost(MAX_HEIGHT_WEIGHT)
        // What was mapped at the old scale is in the wrong place
        if (abs(unitHeight - before) / before > RESCALE_THRESHOLD) map.clear()
    }

    private fun publish(status: GroundTrackingStatus, position: Vector3?, time: Long) {
        val metric = position?.let { it * unitHeight }
        val previous = lastPose
        val velocity = if (metric != null && previous != null && time > previous.second) {
            (metric - previous.first) * (1e9f / (time - previous.second))
        } else {
            Vector3.zero
        }
        lastPose = metric?.let { it to time }
        onUpdate?.invoke(
            GroundTrackingState(
                status,
                map,
                position?.let { -unitHeight * (it.z + 1f) },
                metric,
                velocity,
                time
            )
        )
    }

    private companion object {
        const val WIDTH = 240
        const val SENSOR_PERIOD_MICROS = 5_000
        const val DEFAULT_CAMERA_HEIGHT = 1.4f
        const val SCALE_INTERVAL_NANOS = 1_000_000_000L
        const val MAX_SCALE_ERROR = 0.25f
        const val HEIGHT_MEMORY = 0.9f
        const val MAX_HEIGHT_WEIGHT = 2_500f
        const val RESCALE_THRESHOLD = 0.05f
        const val LOST_FRAMES = 10
    }
}
