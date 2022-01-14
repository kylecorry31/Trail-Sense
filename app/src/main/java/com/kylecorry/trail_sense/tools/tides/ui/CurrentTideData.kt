package com.kylecorry.trail_sense.tools.tides.ui

import com.kylecorry.sol.science.oceanography.TideType

data class CurrentTideData(val waterLevel: Float?, val type: TideType?, val rising: Boolean)
