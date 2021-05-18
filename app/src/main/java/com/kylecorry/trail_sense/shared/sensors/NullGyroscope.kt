package com.kylecorry.trail_sense.shared.sensors

import com.kylecorry.trailsensecore.domain.math.Euler
import com.kylecorry.trailsensecore.domain.math.Quaternion
import com.kylecorry.trailsensecore.infrastructure.sensors.AbstractSensor
import com.kylecorry.trailsensecore.infrastructure.sensors.orientation.IGyroscope

class NullGyroscope : AbstractSensor(), IGyroscope {

    private val empty = FloatArray(3)

    override val quaternion: Quaternion
        get() = Quaternion.zero
    override val rawEuler: FloatArray
        get() = quaternion.toEuler().toFloatArray()
    override val rawQuaternion: FloatArray
        get() = Quaternion.zero.toFloatArray()
    override val euler: Euler
        get() = quaternion.toEuler()

    override val hasValidReading: Boolean
        get() = true

    override fun calibrate() {
    }

    override fun startImpl() {
        notifyListeners()
    }

    override fun stopImpl() {
    }
}