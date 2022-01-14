package com.kylecorry.trail_sense.tools.tides.domain.range

import com.kylecorry.sol.math.Range
import com.kylecorry.trail_sense.tools.tides.domain.TideTable

interface ITideRangeCalculator {

    fun getRange(table: TideTable): Range<Float>

}