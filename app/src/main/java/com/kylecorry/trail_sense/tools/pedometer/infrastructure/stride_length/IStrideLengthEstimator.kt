package com.kylecorry.trail_sense.tools.pedometer.infrastructure.stride_length

import com.kylecorry.andromeda.core.sensors.ISensor
import com.kylecorry.sol.units.Distance

interface IStrideLengthEstimator: ISensor {
    val strideLength: Distance?
    fun reset()
}