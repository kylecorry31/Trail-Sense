package com.kylecorry.trail_sense.shared.sensors.compass

import android.util.Log
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.sense.orientation.IOrientationSensor
import com.kylecorry.sol.math.Quaternion
import com.kylecorry.sol.math.SolMath.real
import com.kylecorry.trail_sense.tools.metaldetector.ui.Debouncer
import java.time.Duration
import kotlin.math.sqrt

/**
 * An orientation sensor that recalibrates itself when the device is still and is significantly out of alignment.
 * @param reference the reference sensor to use for determining the absolute orientation (noisy)
 * @param primary the primary sensor to use for determining the orientation (less noisy)
 * @param resetAngleThreshold the minimum angle difference between the reference and primary sensors to trigger a recalibration
 * @param motionAngleThreshold the minimum angle difference per second that is considered motion. Recalibration will not occur if the device is moving.
 * @param resetTime the minimum time to wait before recalibrating after crossing the reset angle threshold
 * @param verbose true if the sensor should print debug information
 */
class QuickRecalibrationOrientationSensor(
    private val reference: IOrientationSensor,
    private val primary: IOrientationSensor,
    private val resetAngleThreshold: Float = 4f,
    private val motionAngleThreshold: Float = 60f,
    resetTime: Duration = Duration.ofSeconds(1),
    private val verbose: Boolean = false
) : AbstractSensor(), IOrientationSensor {

    override val hasValidReading: Boolean
        get() = primary.hasValidReading

    override val headingAccuracy: Float?
        get() = primary.headingAccuracy

    override val orientation: Quaternion
        get() = primary.orientation

    override val rawOrientation: FloatArray
        get() = primary.rawOrientation

    private var isStarted = false
    private val startLock = Any()
    private val resetTimeMillis = resetTime.toMillis()
    private var lastThresholdTime = 0L
    private var lastOrientation = Quaternion.zero
    private var isMoving = Debouncer(Duration.ofMillis(40))
    private var lastMotionTime = 0L
    private var lastLogTime = 0L

    override fun startImpl() {
        synchronized(startLock) {
            isStarted = true
            reference.start(this::onReferenceUpdate)
            primary.start(this::onPrimaryUpdate)
        }
    }

    override fun stopImpl() {
        synchronized(startLock) {
            isStarted = false
            reference.stop(this::onReferenceUpdate)
            primary.stop(this::onPrimaryUpdate)
        }
    }

    private fun onReferenceUpdate(): Boolean {

        val referenceQuat = reference.orientation
        val primaryQuat = primary.orientation

        // Calculate angle between the reference and the primary quaternions
        val euler = primaryQuat.minus(referenceQuat).toEuler()
        val angularMagnitude =
            sqrt(euler.roll * euler.roll + euler.pitch * euler.pitch + euler.yaw * euler.yaw).real(
                0f
            )

        // Don't reset if the device is moving
        val dt = (System.currentTimeMillis() - lastMotionTime) / 1000f
        if (dt > 0.1f) {
            val motionEuler = primaryQuat.minus(lastOrientation).toEuler()
            val motion = sqrt(
                motionEuler.roll * motionEuler.roll + motionEuler.pitch * motionEuler.pitch + motionEuler.yaw * motionEuler.yaw
            ).real(0f) / dt
            lastOrientation = primaryQuat
            isMoving.update(motion > motionAngleThreshold)
            lastMotionTime = System.currentTimeMillis()
        }

        val moving = isMoving.value

        if (angularMagnitude > resetAngleThreshold && !moving) {
            if (lastThresholdTime == 0L) {
                lastThresholdTime = System.currentTimeMillis()
            } else {
                if (System.currentTimeMillis() - lastThresholdTime > resetTimeMillis) {
                    if (verbose) {
                        Log.d(javaClass.simpleName, "Recalibrating")
                    }
                    recalibrate()
                    lastThresholdTime = 0L
                }
            }
        } else {
            lastThresholdTime = 0L
        }

        if (verbose && System.currentTimeMillis() - lastLogTime > 500) {
            Log.d(
                javaClass.simpleName,
                "Diff: ${
                    DecimalFormatter.format(
                        angularMagnitude,
                        2,
                        true
                    ).padStart(6, ' ')
                }, Motion: $moving, Resetting: ${lastThresholdTime != 0L}"
            )
            lastLogTime = System.currentTimeMillis()
        }

        return true
    }

    private fun onPrimaryUpdate(): Boolean {
        notifyListeners()
        return true
    }

    private fun recalibrate() {
        synchronized(startLock) {
            if (isStarted) {
                primary.stop(this::onPrimaryUpdate)
                primary.start(this::onPrimaryUpdate)
            }
        }
    }


}