package com.kylecorry.trail_sense.tools.astronomy.domain

import com.kylecorry.sol.units.Bearing
import java.time.LocalDateTime

data class SunDetails(
    val rise: LocalDateTime?,
    val set: LocalDateTime?,
    val transit: LocalDateTime?,
    val isUp: Boolean,
    val nextRise: LocalDateTime?,
    val nextSet: LocalDateTime?,
    val altitude: Float,
    val azimuth: Bearing
) {
    val nextTransition: AstronomyTransition?
        get() {
            if (nextRise == null && nextSet == null) {
                return null
            }
            if (nextRise == null) {
                return AstronomyTransition.Set
            }
            if (nextSet == null) {
                return AstronomyTransition.Rise
            }
            return if (nextRise.isBefore(nextSet)) AstronomyTransition.Rise else AstronomyTransition.Set
        }
}