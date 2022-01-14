package com.kylecorry.trail_sense.tools.tides.ui

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.units.Reading

data class DailyTideData(val waterLevels: List<Reading<Float>>, val tides: List<Tide>, val waterLevelRange: Range<Float>)
