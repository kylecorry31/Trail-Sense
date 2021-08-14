package com.kylecorry.trail_sense.shared.sensors

import com.kylecorry.andromeda.core.math.Vector3
import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.sense.magnetometer.IMagnetometer

class NullMagnetometer : AbstractSensor(), IMagnetometer {

    private val empty = Vector3.zero.toFloatArray()

    override val magneticField: Vector3
        get() = Vector3.zero
    override val hasValidReading: Boolean
        get() = true
    override val rawMagneticField: FloatArray
        get() = empty

    override fun startImpl() {
        notifyListeners()
    }

    override fun stopImpl() {
    }
}