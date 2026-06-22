package com.kylecorry.trail_sense.tools.navigation.ui.data

data class NavAstronomyData(
    val sunBearing: Float,
    val moonBearing: Float,
    val isSunUp: Boolean,
    val isMoonUp: Boolean,
    val moonPhaseAngle: Float,
    val moonTilt: Float
)
