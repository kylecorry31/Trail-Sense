package com.kylecorry.trail_sense.tools.tides.domain.waterlevel

import com.kylecorry.sol.math.Range
import java.time.ZonedDateTime

class PiecewiseWaterLevelCalculator(private val calculators: List<Pair<Range<ZonedDateTime>, IWaterLevelCalculator>>) :
    IWaterLevelCalculator {
    override fun calculate(time: ZonedDateTime): Float {
        for (calculator in calculators) {
            if (calculator.first.contains(time)) {
                return calculator.second.calculate(time)
            }
        }
        return 0f
    }
}