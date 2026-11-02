package com.kylecorry.trail_sense.tools.navigation.domain.astronomy

import com.kylecorry.trail_sense.tools.astronomy.domain.MoonTilt

data class NavAstronomyData(
    val sunBearing: Float,
    val moonBearing: Float,
    val isSunUp: Boolean,
    val isMoonUp: Boolean,
    val moonPhaseAngle: Float,
    val moonTilt: MoonTilt
)
