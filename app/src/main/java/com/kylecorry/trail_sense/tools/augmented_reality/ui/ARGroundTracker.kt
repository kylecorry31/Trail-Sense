package com.kylecorry.trail_sense.tools.augmented_reality.ui

import android.graphics.Bitmap
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.sense.pedometer.IPedometer
import com.kylecorry.sol.math.Vector3
import com.kylecorry.trail_sense.tools.augmented_reality.domain.ground.GroundEstimator
import com.kylecorry.trail_sense.tools.augmented_reality.domain.ground.TerrainMap
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.AugmentedRealityCoordinate
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.cos
import kotlin.math.sin

/**
 * Feeds camera frames to the ground estimator while the user is walking and publishes the
 * smoothed result on the AR view.
 */
class ARGroundTracker(
    private val view: AugmentedRealityView,
    private val pedometer: IPedometer,
    private val stepLengthMeters: Float
) {

    private val executor = Executors.newSingleThreadExecutor()
    private val busy = AtomicBoolean(false)

    private var reference: GroundEstimator.Frame? = null
    private var referenceTime = 0L
    private var referenceSteps = 0
    private var smoothed: Float? = null
    private val terrain = TerrainMap()
    private var posX = 0f
    private var posY = 0f

    // Frames are scaled to the view, so frame pixels map linearly to view pixels
    private var frameWidth = 1
    private var frameHeight = 1

    private val projector = object : GroundEstimator.Projector {
        override fun ray(x: Float, y: Float, rotation: FloatArray): Vector3 {
            val px = PixelCoordinate(x * view.width / frameWidth, y * view.height / frameHeight)
            return view.toCoordinate(px, rotation).position
        }

        override fun pixel(ray: Vector3, rotation: FloatArray): Pair<Float, Float>? {
            val p = view.toPixel(
                AugmentedRealityCoordinate(ray, view.isTrueNorth),
                nanIfBehindCamera = true,
                rotationMatrixOverride = rotation
            )
            if (p.x.isNaN()) return null
            return (p.x * frameWidth / view.width) to (p.y * frameHeight / view.height)
        }
    }

    private val estimator = GroundEstimator(projector)

    fun start() {
        pedometer.start { true }
    }

    fun stop() {
        pedometer.stop { true }
    }

    fun reset() {
        reference = null
        smoothed = null
        posX = 0f
        posY = 0f
        terrain.clear()
        view.terrain = terrain
        view.terrainPosition = 0f to 0f
        view.groundOffset = null
    }

    fun onFrame(bitmap: Bitmap) {
        if (view.width == 0 || !busy.compareAndSet(false, true)) return
        val rotation = view.rotationMatrix.copyOf()
        val azimuth = Math.toRadians(view.azimuth.toDouble())
        val speed = view.gps.speed.value
        val steps = pedometer.steps
        val now = System.nanoTime()
        val scaled = Bitmap.createScaledBitmap(bitmap, WIDTH, WIDTH * bitmap.height / bitmap.width, true)
        executor.execute {
            try {
                process(scaled, rotation, azimuth, speed, steps, now)
            } finally {
                busy.set(false)
            }
        }
    }

    private fun process(bitmap: Bitmap, rotation: FloatArray, azimuth: Double, speed: Float, steps: Int, now: Long) {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val gray = IntArray(w * h) { (pixels[it] shr 8) and 0xff }
        val frame = GroundEstimator.Frame(gray, w, h, rotation)
        frameWidth = w
        frameHeight = h

        val ref = reference
        val dt = (now - referenceTime) / 1e9f
        val stepDistance = (steps - referenceSteps) * stepLengthMeters
        // Prefer the pedometer (real distance walked), fall back to GPS speed
        val distance = if (stepDistance > 0f) stepDistance else if (speed >= MIN_SPEED) speed * dt else 0f
        val move = Vector3(sin(azimuth).toFloat() * distance, cos(azimuth).toFloat() * distance, 0f)
        if (ref == null || dt > MAX_GAP || (distance == 0f && dt > MIN_GAP)) {
            advance(move)
            reference = frame
            referenceTime = now
            referenceSteps = steps
            return
        }
        if (dt < MIN_GAP || distance == 0f) return

        val result = estimator.estimate(ref, frame, move)
        advance(move)
        terrain.add(result.points, posX, posY)
        reference = frame
        referenceTime = now
        referenceSteps = steps

        val down = result.groundDown ?: return
        val previous = smoothed
        val next = if (previous == null) down else previous + (down - previous) * ALPHA
        smoothed = next
        view.groundOffset = -next
    }

    private fun advance(move: Vector3) {
        posX += move.x
        posY += move.y
        view.terrainPosition = posX to posY
    }

    private companion object {
        const val WIDTH = 160
        const val MIN_SPEED = 0.6f
        const val MIN_GAP = 0.4f
        const val MAX_GAP = 2.5f
        const val ALPHA = 0.25f
    }
}
