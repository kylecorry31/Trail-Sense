package com.kylecorry.trail_sense.tools.tides.infrastructure.model

import com.kylecorry.sol.science.oceanography.TidalHarmonic
import com.kylecorry.sol.units.Coordinate

data class TideModelResult(val location: Coordinate, val harmonics: List<TidalHarmonic>)