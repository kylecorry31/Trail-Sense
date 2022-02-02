package com.kylecorry.trail_sense.shared.sensors

import com.kylecorry.andromeda.sense.accelerometer.IAccelerometer
import com.kylecorry.sol.math.Vector3

class NullAccelerometer : NullSensor(), IAccelerometer {
    private val empty = Vector3.zero.toFloatArray()

    override val acceleration: Vector3
        get() = Vector3.zero
    override val rawAcceleration: FloatArray
        get() = empty
}