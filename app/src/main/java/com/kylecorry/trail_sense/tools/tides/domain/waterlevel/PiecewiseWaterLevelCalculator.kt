package com.kylecorry.trail_sense.tools.tides.domain.waterlevel

import com.kylecorry.sol.math.Range
import java.time.ZonedDateTime

class PiecewiseWaterLevelCalculator(private val calculators: List<Pair<Range<ZonedDateTime>, IWaterLevelCalculator>>) :
    IWaterLevelCalculator {
    override fun calculate(time: ZonedDateTime): Float {
        return calculators.firstOrNull { it.first.contains(time) }?.second?.calculate(time) ?: 0f
    }
}