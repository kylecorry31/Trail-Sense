package com.kylecorry.trail_sense.navigation.ui.data

import com.kylecorry.sol.science.astronomy.moon.MoonTruePhase
import com.kylecorry.sol.units.Bearing

data class NavAstronomyData(
    val sunBearing: Bearing,
    val moonBearing: Bearing,
    val isSunUp: Boolean,
    val isMoonUp: Boolean,
    val moonPhase: MoonTruePhase
)