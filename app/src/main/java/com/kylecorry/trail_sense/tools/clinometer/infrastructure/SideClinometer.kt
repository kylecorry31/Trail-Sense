package com.kylecorry.trail_sense.tools.clinometer.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.andromeda.sense.accelerometer.GravitySensor
import com.kylecorry.andromeda.sense.accelerometer.IAccelerometer
import com.kylecorry.andromeda.sense.accelerometer.LowPassAccelerometer
import com.kylecorry.andromeda.sense.inclinometer.IInclinometer
import com.kylecorry.sol.math.Vector3
import kotlin.math.atan2

class SideClinometer(context: Context) : AbstractSensor(), IInclinometer {

    override val angle: Float
        get() = _angle

    val incline: Float
        get() = _incline

    override val hasValidReading: Boolean
        get() = gotReading

    private var gotReading = false

    override val quality: Quality
        get() = _quality
    private var _quality = Quality.Unknown

    private val accelerometer: IAccelerometer =
        if (Sensors.hasGravity(context)) GravitySensor(context) else LowPassAccelerometer(context)

    private var _angle = 0f
    private var _incline = 0f

    private fun updateSensor(): Boolean {

        // Gravity
        val gravity = accelerometer.acceleration
        _quality = accelerometer.quality
        _angle = calculate(gravity)

        gotReading = true
        notifyListeners()
        return true
    }

    override fun startImpl() {
        accelerometer.start(this::updateSensor)
    }

    override fun stopImpl() {
        accelerometer.stop(this::updateSensor)
    }

    fun calculate(gravity: Vector3): Float {
        val angle = Math.toDegrees(atan2(gravity.y.toDouble(), gravity.x.toDouble())).toFloat()

        _incline = when {
            angle > 90 -> {
                180 - angle
            }
            angle < -90 -> {
                -180 - angle
            }
            else -> {
                angle
            }
        }

        return angle
    }

}