package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.andromeda.sense.accelerometer.IAccelerometer
import android.opengl.Matrix
import android.view.Surface
import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.andromeda.sense.orientation.IOrientationSensor
import com.kylecorry.andromeda.sense.orientation.OrientationUtils
import com.kylecorry.sol.math.Vector3
import java.time.Duration
import kotlin.math.min

/**
 * An accelerometer which reports acceleration in the world frame (x is east, y is north, z is up)
 * @param accelerometer The accelerometer to use
 * @param orientation The orientation sensor to use
 * @param interval The update interval
 * @param useTrueNorth True to use true north, false to use magnetic north
 * @param surfaceRotation The rotation of the surface (e.g. the display) in degrees
 * @param useAROrientation True to use AR orientation, false to use compass orientation
 */
class WorldAccelerometer(
    private val accelerometer: IAccelerometer,
    private val orientation: IOrientationSensor,
    private val interval: Duration = Duration.ofMillis(20),
    var useTrueNorth: Boolean = false,
    var surfaceRotation: Int = Surface.ROTATION_0,
    var useAROrientation: Boolean = false
) : IAccelerometer, AbstractSensor() {

    private val rotationMatrix = FloatArray(16)
    private val tempOrientation = FloatArray(3)
    private val tempAcceleration4 = FloatArray(4)

    /**
     * The geomagnetic declination
     */
    var declination = 0f

    override val acceleration: Vector3
        get() = Vector3(rawAcceleration[0], rawAcceleration[1], rawAcceleration[2])

    override val rawAcceleration: FloatArray = FloatArray(3)

    override val hasValidReading: Boolean
        get() = accelerometer.hasValidReading && orientation.hasValidReading

    private val timer = CoroutineTimer {
        update()
    }

    override fun startImpl() {
        accelerometer.start(this::onSensorUpdate)
        orientation.start(this::onSensorUpdate)
        timer.interval(interval)
    }

    override fun stopImpl() {
        accelerometer.stop(this::onSensorUpdate)
        orientation.stop(this::onSensorUpdate)
        timer.stop()
    }

    override val quality: Quality
        get() = Quality.entries[min(accelerometer.quality.ordinal, orientation.quality.ordinal)]

    private fun onSensorUpdate(): Boolean {
        return true
    }

    private fun update() {
        if (!hasValidReading) return

        if (useAROrientation) {
            OrientationUtils.getAROrientation(
                orientation,
                rotationMatrix,
                tempOrientation,
                if (useTrueNorth) declination else 0f
            )
        } else {
            OrientationUtils.getCompassOrientation(
                orientation,
                rotationMatrix,
                tempOrientation,
                surfaceRotation,
                if (useTrueNorth) declination else 0f
            )
        }

        Matrix.invertM(rotationMatrix, 0, rotationMatrix, 0)
        accelerometer.rawAcceleration.copyInto(tempAcceleration4)
        Matrix.multiplyMV(tempAcceleration4, 0, rotationMatrix, 0, tempAcceleration4, 0)

        rawAcceleration[0] = tempAcceleration4[0]
        rawAcceleration[1] = tempAcceleration4[1]
        rawAcceleration[2] = tempAcceleration4[2]

        notifyListeners()
    }
}