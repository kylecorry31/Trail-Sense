package com.kylecorry.trail_sense.astronomy.domain

import java.time.LocalDateTime

data class LunarEclipse(
    val start: LocalDateTime,
    val end: LocalDateTime,
    val peak: LocalDateTime,
    val magnitude: Float
){
    val isTotal: Boolean = magnitude >= 1f
}