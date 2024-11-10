package com.kylecorry.trail_sense.tools.astronomy.domain

import com.kylecorry.sol.science.astronomy.moon.MoonTruePhase
import com.kylecorry.sol.units.Bearing
import java.time.LocalDateTime

data class MoonDetails(
    val rise: LocalDateTime?,
    val set: LocalDateTime?,
    val transit: LocalDateTime?,
    val isUp: Boolean,
    val phase: MoonTruePhase,
    val illumination: Float,
    val altitude: Float,
    val azimuth: Bearing,
    val tilt: Float
)