package com.kylecorry.trail_sense.tools.pedometer.domain

import com.kylecorry.sol.units.Distance

interface IPedometerService {
    fun getDistance(steps: Long, strideLength: Distance): Distance
}