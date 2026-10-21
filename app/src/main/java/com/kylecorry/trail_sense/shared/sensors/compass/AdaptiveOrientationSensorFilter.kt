package com.kylecorry.trail_sense.shared.sensors.compass

import com.kylecorry.andromeda.sense.orientation.filter.OrientationSensorFilter
import com.kylecorry.sol.math.Quaternion
import com.kylecorry.sol.math.QuaternionMath
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * A One Euro filter for orientation. The cutoff frequency rises with the rotation speed, so a still
 * device is smoothed heavily (no jitter) and a turning device is barely smoothed (no lag).
 *
 * The speed is the magnitude of the low passed rotation vector, so zero-mean noise cancels out
 * instead of being read as movement.
 */
class AdaptiveOrientationSensorFilter(
    private val minCutoffHz: Float = 0.2f,
    private val beta: Float = 0.1f,
    private val speedCutoffHz: Float = 1f,
    private val nanoTime: () -> Long = System::nanoTime
) : OrientationSensorFilter {

    private val current = Quaternion.zero.toFloatArray()
    private val previousRaw = Quaternion.zero.toFloatArray()
    private val delta = FloatArray(4)
    private val inverse = FloatArray(4)
    private val angularVelocity = FloatArray(3)
    private var lastTime = 0L

    override fun filter(quaternion: FloatArray, out: FloatArray) {
        if (quaternion.any { it.isNaN() || it.isInfinite() }) return

        val now = nanoTime()
        val dt = ((now - lastTime) / 1e9f).coerceIn(MIN_DT, MAX_DT)
        lastTime = now

        // Rotation from the previous raw reading to this one, in the world frame
        QuaternionMath.conjugate(previousRaw, inverse)
        QuaternionMath.multiply(quaternion, inverse, delta)
        if (delta[QuaternionMath.W] < 0) {
            for (i in delta.indices) delta[i] = -delta[i]
        }
        val halfAngle = sqrt(delta[0] * delta[0] + delta[1] * delta[1] + delta[2] * delta[2])
        val angle = 2 * acos(delta[QuaternionMath.W].coerceIn(-1f, 1f))
        val scale = if (halfAngle < 1e-6f) 2f else angle / halfAngle

        val speedAlpha = alpha(speedCutoffHz, dt)
        var speedSquared = 0f
        for (i in 0..2) {
            val velocity = delta[i] * scale / dt
            angularVelocity[i] += speedAlpha * (velocity - angularVelocity[i])
            speedSquared += angularVelocity[i] * angularVelocity[i]
        }
        val speedDegrees = sqrt(speedSquared) * RAD_TO_DEG

        val cutoff = minCutoffHz + beta * speedDegrees
        QuaternionMath.slerp(current, quaternion, alpha(cutoff, dt), current, true)
        QuaternionMath.normalize(current, current)

        quaternion.copyInto(previousRaw)
        current.copyInto(out)
    }

    override fun reset(value: FloatArray) {
        value.copyInto(current)
        value.copyInto(previousRaw)
        angularVelocity.fill(0f)
        lastTime = nanoTime()
    }

    private fun alpha(cutoffHz: Float, dt: Float): Float {
        val tau = 1f / (2 * PI.toFloat() * cutoffHz)
        return 1f / (1f + tau / dt)
    }

    private companion object {
        const val MIN_DT = 0.001f
        const val MAX_DT = 0.5f
        const val RAD_TO_DEG = (180 / PI).toFloat()
    }
}
