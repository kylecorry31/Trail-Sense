package com.kylecorry.trail_sense.shared.sensors

import com.kylecorry.andromeda.sense.orientation.IGyroscope
import com.kylecorry.sol.math.Euler
import com.kylecorry.sol.math.Quaternion

class NullGyroscope : NullSensor(), IGyroscope {

    private val empty = FloatArray(3)

    override val orientation: Quaternion
        get() = Quaternion.zero
    override val rawAngularRate: FloatArray
        get() = empty
    override val rawOrientation: FloatArray
        get() = Quaternion.zero.toFloatArray()
    override val angularRate: Euler
        get() = Euler(0f, 0f, 0f)
}