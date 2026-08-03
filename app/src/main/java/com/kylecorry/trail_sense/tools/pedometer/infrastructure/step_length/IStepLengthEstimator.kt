package com.kylecorry.trail_sense.tools.pedometer.infrastructure.step_length

import com.kylecorry.andromeda.core.sensors.ISensor
import com.kylecorry.sol.units.Distance

interface IStepLengthEstimator : ISensor {
    val stepLength: Distance?
    fun reset()
}
