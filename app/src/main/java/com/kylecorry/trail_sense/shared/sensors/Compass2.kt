package com.kylecorry.trail_sense.shared.sensors

import android.content.Context
import android.hardware.SensorManager
import com.kylecorry.trail_sense.navigation.domain.compass.Bearing
import com.kylecorry.trail_sense.weather.domain.MovingAverageFilter
import kotlin.math.abs

class Compass2(context: Context): AbstractSensor(), ICompass {

    private val accelerometer = Accelerometer(context)
    private val magnetometer = Magnetometer(context)

    private val filter = MovingAverageFilter(50)

    override var declination = 0f

    override val bearing: Bearing
        get() = Bearing(_bearing).withDeclination(declination)

    private val rotation = FloatArray(9)
    private val orientation = FloatArray(3)

    private var _bearing = 0f

    private fun updateRotation(): Boolean {
        SensorManager.getRotationMatrix(rotation, null, accelerometer.acceleration, magnetometer.magneticField)

        var largestAccelAxis = 0
        for (i in accelerometer.acceleration.indices){
            if (abs(accelerometer.acceleration[i]) > abs(accelerometer.acceleration[largestAccelAxis])){
                largestAccelAxis = i
            }
        }

        // If the device is vertical, change the compass orientation to a different axis
        if (largestAccelAxis == 1) {
            SensorManager.remapCoordinateSystem(
                rotation,
                SensorManager.AXIS_X, SensorManager.AXIS_Z,
                rotation
            )
        }


        SensorManager.getOrientation(rotation, orientation)

        _bearing = filter.filter(Math.toDegrees(orientation[0].toDouble())).toFloat()

        notifyListeners()

        return true
    }

    override fun startImpl() {
        accelerometer.start(this::updateRotation)
        magnetometer.start(this::updateRotation)
    }

    override fun stopImpl() {
        accelerometer.stop(this::updateRotation)
        magnetometer.stop(this::updateRotation)
    }

}