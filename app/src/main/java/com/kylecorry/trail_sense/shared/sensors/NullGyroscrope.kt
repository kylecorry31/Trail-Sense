package com.kylecorry.trail_sense.shared.sensors

import com.kylecorry.trail_sense.tools.metaldetector.ui.IGyroscope
import com.kylecorry.trailsensecore.domain.math.Vector3
import com.kylecorry.trailsensecore.infrastructure.sensors.AbstractSensor

class NullGyroscrope: AbstractSensor(), IGyroscope {

    private val empty = FloatArray(3)

    override val rawRotation: FloatArray
        get() = empty
    override val rotation: Vector3
        get() = Vector3.zero

    override fun calibrate() {
    }

    override val hasValidReading: Boolean
        get() = true

    override fun startImpl() {
        notifyListeners()
    }

    override fun stopImpl() {
    }
}