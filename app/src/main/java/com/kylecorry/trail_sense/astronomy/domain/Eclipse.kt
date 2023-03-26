package com.kylecorry.trail_sense.astronomy.domain

import com.kylecorry.sol.math.SolMath.roundPlaces
import com.kylecorry.sol.units.Bearing
import java.time.Duration
import java.time.ZonedDateTime

data class Eclipse(
    val start: ZonedDateTime,
    val end: ZonedDateTime,
    val peak: ZonedDateTime,
    val magnitude: Float,
    val obscuration: Float,
    val peakAltitude: Float,
    val peakDirection: Bearing
){
    val isTotal: Boolean = magnitude.roundPlaces(2) >= 1f
    val duration: Duration = Duration.between(start, end)
}