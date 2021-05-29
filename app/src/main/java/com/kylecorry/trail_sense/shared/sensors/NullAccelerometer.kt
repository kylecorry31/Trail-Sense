package com.kylecorry.trail_sense.shared.sensors

import com.kylecorry.trailsensecore.domain.math.Vector3
import com.kylecorry.trailsensecore.infrastructure.sensors.AbstractSensor
import com.kylecorry.trailsensecore.infrastructure.sensors.accelerometer.IAccelerometer

class NullAccelerometer : AbstractSensor(), IAccelerometer {

    private val empty = Vector3.zero.toFloatArray()

    override val acceleration: Vector3
        get() = Vector3.zero
    override val hasValidReading: Boolean
        get() = true
    override val rawAcceleration: FloatArray
        get() = empty

    override fun startImpl() {
        notifyListeners()
    }

    override fun stopImpl() {
    }
}