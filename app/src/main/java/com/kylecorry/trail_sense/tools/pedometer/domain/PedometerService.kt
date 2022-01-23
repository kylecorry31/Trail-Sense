package com.kylecorry.trail_sense.tools.pedometer.domain

import com.kylecorry.sol.units.Distance

class PedometerService : IPedometerService {
    override fun getDistance(steps: Long, strideLength: Distance): Distance {
        return Distance(steps * strideLength.distance, strideLength.units)
    }
}