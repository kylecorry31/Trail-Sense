package com.kylecorry.trail_sense.shared.sensors

import com.kylecorry.andromeda.core.math.Euler
import com.kylecorry.andromeda.core.math.Quaternion
import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.sense.orientation.IGyroscope

class NullGyroscope : AbstractSensor(), IGyroscope {

    private val empty = FloatArray(3)

    override val orientation: Quaternion
        get() = Quaternion.zero
    override val rawAngularRate: FloatArray
        get() = empty
    override val rawOrientation: FloatArray
        get() = Quaternion.zero.toFloatArray()
    override val angularRate: Euler
        get() = Euler(0f, 0f, 0f)

    override val hasValidReading: Boolean
        get() = true

    override fun startImpl() {
        notifyListeners()
    }

    override fun stopImpl() {
    }
}