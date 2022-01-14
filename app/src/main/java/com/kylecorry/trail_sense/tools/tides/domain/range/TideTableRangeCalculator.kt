package com.kylecorry.trail_sense.tools.tides.domain.range

import com.kylecorry.sol.math.Range
import com.kylecorry.trail_sense.tools.tides.domain.TideTable

class TideTableRangeCalculator: ITideRangeCalculator {
    override fun getRange(table: TideTable): Range<Float> {
        val lows = table.tides.filter { !it.isHigh }.mapNotNull { it.height }
        val highs = table.tides.filter { it.isHigh }.mapNotNull { it.height }
        var min = lows.minByOrNull { it }
        var max = highs.maxByOrNull { it }

        if (min == null){
            min = (max ?: 1f) - 1f
        }

        if (max == null){
            max = min + 1f
        }

        return Range(min, max)
    }
}