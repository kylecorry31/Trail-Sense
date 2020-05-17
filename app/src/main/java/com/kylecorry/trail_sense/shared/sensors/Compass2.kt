package com.kylecorry.trail_sense.shared.sensors

import android.content.Context
import android.hardware.SensorManager
import com.kylecorry.trail_sense.navigation.domain.compass.Bearing
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.weather.domain.MovingAverageFilter
import kotlin.math.abs
import kotlin.math.floor

class Compass2(context: Context) : AbstractSensor(), ICompass {

    private val accelerometer = Accelerometer(context)
    private val magnetometer = Magnetometer(context)

    private val prefs = UserPreferences(context)
    private val filterSize = prefs.navigation.compassSmoothing * 2 * 2
    private val filter = MovingAverageFilter(filterSize)

    override var declination = 0f

    override val bearing: Bearing
        get() = Bearing(_filteredBearing).withDeclination(declination)

    private var _bearing = 0f
    private var _filteredBearing = 0f

    private val R = FloatArray(9)
    private val I = FloatArray(9)
    private val orientation = FloatArray(3)

    private fun updateBearing(newBearing: Float) {
        _bearing += deltaAngle(_bearing, newBearing)
        _filteredBearing = filter.filter(_bearing.toDouble()).toFloat()
    }

    private fun deltaAngle(angle1: Float, angle2: Float): Float {
        var delta = angle2 - angle1
        delta += 180
        delta -= floor(delta / 360) * 360
        delta -= 180
        if (abs(abs(delta) - 180) <= Float.MIN_VALUE) {
            delta = 180f
        }
        return delta
    }

    private fun updateSensor(): Boolean {
        val success = SensorManager.getRotationMatrix(
            R,
            I,
            accelerometer.acceleration,
            magnetometer.magneticField
        )

        if (success) {

            var largestAccelAxis = 0
            for (i in accelerometer.acceleration.indices) {
                if (abs(accelerometer.acceleration[i]) > abs(accelerometer.acceleration[largestAccelAxis])) {
                    largestAccelAxis = i
                }
            }

            // If the device is vertical, change the compass orientation to a different axis
            if (largestAccelAxis == 1) {
                SensorManager.remapCoordinateSystem(
                    R,
                    SensorManager.AXIS_X, SensorManager.AXIS_Z,
                    R
                )
            }

            SensorManager.getOrientation(R, orientation)

            if (largestAccelAxis == 1 && accelerometer.acceleration[largestAccelAxis] < 0){
                orientation[0] = orientation[0] - Math.PI.toFloat()
            }

            updateBearing(Math.toDegrees(orientation[0].toDouble()).toFloat())
            notifyListeners()
        }
        return true
    }

    override fun startImpl() {
        accelerometer.start(this::updateSensor)
        magnetometer.start(this::updateSensor)
    }

    override fun stopImpl() {
        accelerometer.stop(this::updateSensor)
        magnetometer.stop(this::updateSensor)
    }

}