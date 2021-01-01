package com.kylecorry.trail_sense.tools.level.ui

import android.content.Context
import com.kylecorry.trailsensecore.domain.Accuracy
import com.kylecorry.trailsensecore.domain.math.Vector3
import com.kylecorry.trailsensecore.domain.math.toDegrees
import com.kylecorry.trailsensecore.infrastructure.sensors.AbstractSensor
import com.kylecorry.trailsensecore.infrastructure.sensors.SensorChecker
import com.kylecorry.trailsensecore.infrastructure.sensors.accelerometer.GravitySensor
import com.kylecorry.trailsensecore.infrastructure.sensors.accelerometer.LowPassAccelerometer
import com.kylecorry.trailsensecore.infrastructure.sensors.accelerometer.IAccelerometer
import com.kylecorry.trailsensecore.infrastructure.sensors.orientation.IOrientationSensor
import kotlin.math.*

class OrientationSensor2(context: Context) : AbstractSensor(), IOrientationSensor {

    override val orientation: Vector3
        get() = _angle

    override val hasValidReading: Boolean
        get() = gotReading

    private var gotReading = false

    override val accuracy: Accuracy
        get() = _accuracy
    private var _accuracy: Accuracy = Accuracy.Unknown

    private val sensorChecker = SensorChecker(context)
    private val accelerometer: IAccelerometer =
        if (sensorChecker.hasGravity()) GravitySensor(context) else LowPassAccelerometer(context)

    private var _angle = Vector3.zero

    private fun updateSensor(): Boolean {

        // Gravity
        val gravity = accelerometer.acceleration
        val normalGravity = gravity.normalize()

        _angle = Vector3(
            acos(normalGravity.x).toDegrees(),
            normalGravity.y * 90f,
            atan2(-normalGravity.y, normalGravity.x).toDegrees()
        )

        gotReading = true
        notifyListeners()
        return true
    }

    private fun calculate(y: Float, x: Float): Float {
        var angle = Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat()

        if (angle > 90) {
            angle = 180 - angle
        }

        if (angle < -90) {
            angle = -180 - angle
        }

        return angle
    }

    override fun startImpl() {
        accelerometer.start(this::updateSensor)
    }

    override fun stopImpl() {
        accelerometer.stop(this::updateSensor)
    }

}