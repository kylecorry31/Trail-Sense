package com.kylecorry.trail_sense.shared.sensors.compass

import com.kylecorry.andromeda.sense.orientation.IOrientationSensor
import com.kylecorry.sol.math.Quaternion
import com.kylecorry.trail_sense.shared.sensors.NullSensor

class NullOrientationSensor : NullSensor(), IOrientationSensor {
    override val headingAccuracy: Float?
        get() = null
    override val orientation: Quaternion
        get() = Quaternion.zero
    override val rawOrientation: FloatArray
        get() = FloatArray(4)
}