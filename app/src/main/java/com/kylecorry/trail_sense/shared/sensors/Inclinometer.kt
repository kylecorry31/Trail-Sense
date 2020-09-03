package com.kylecorry.trail_sense.shared.sensors

import android.content.Context
import com.kylecorry.trail_sense.shared.domain.Accuracy
import kotlin.math.atan2
import kotlin.math.sqrt

class Inclinometer(context: Context) : AbstractSensor(), IInclinometer {

    override val angle: Float
        get() = _angle

    override val hasValidReading: Boolean
        get() = gotReading

    private var gotReading = false

    override val accuracy: Accuracy
        get() = _accuracy
    private var _accuracy: Accuracy = Accuracy.Unknown

    // TODO: Check if gravity sensor is available, else use accelerometer
    private val sensorChecker = SensorChecker(context)
    private val accelerometer: IAccelerometer =
        if (sensorChecker.hasGravity()) GravitySensor(context) else LowPassAccelerometer(context)

    private var _angle = 0f

    private fun updateSensor(): Boolean {

        // Gravity
        val normGravity = accelerometer.acceleration.normalize()

        _angle = MathUtils.wrap(Math.toDegrees(atan2(normGravity.y.toDouble(), normGravity.x.toDouble())).toFloat(), -90f, 90f)
            //Math.toDegrees(atan2(-normGravity.x.toDouble(), magnitude(normGravity.y.toDouble(), normGravity.z.toDouble()))).toFloat()
            //Math.toDegrees(atan2(normGravity.y.toDouble(), normGravity.z.toDouble())).toFloat()

        gotReading = true
        notifyListeners()
        return true
    }

    private fun magnitude(a: Double, b: Double): Double {
        return sqrt(a * a + b * b)
    }

    override fun startImpl() {
        accelerometer.start(this::updateSensor)
    }

    override fun stopImpl() {
        accelerometer.stop(this::updateSensor)
    }

}