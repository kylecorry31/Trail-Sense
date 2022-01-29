package com.kylecorry.trail_sense.tools.pedometer.infrastructure.stride_length

import com.kylecorry.sol.units.Distance

interface IStrideLengthCalculator {
    suspend fun calculate(): Distance?
    fun stop(force: Boolean = false)
}