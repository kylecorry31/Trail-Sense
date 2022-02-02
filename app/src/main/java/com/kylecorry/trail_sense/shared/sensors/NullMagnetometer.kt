package com.kylecorry.trail_sense.shared.sensors

import com.kylecorry.andromeda.sense.magnetometer.IMagnetometer
import com.kylecorry.sol.math.Vector3

class NullMagnetometer : NullSensor(), IMagnetometer {
    private val empty = Vector3.zero.toFloatArray()
    override val magneticField: Vector3
        get() = Vector3.zero
    override val rawMagneticField: FloatArray
        get() = empty
}