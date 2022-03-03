package com.kylecorry.trail_sense.astronomy.domain

import java.time.Duration
import java.time.ZonedDateTime

data class LunarEclipse(
    val start: ZonedDateTime,
    val end: ZonedDateTime,
    val peak: ZonedDateTime,
    val magnitude: Float
){
    val isTotal: Boolean = magnitude >= 1f
    val duration = Duration.between(start, end)
}