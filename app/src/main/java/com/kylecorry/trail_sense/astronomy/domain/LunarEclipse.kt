package com.kylecorry.trail_sense.astronomy.domain

import java.time.Duration
import java.time.LocalDateTime

data class LunarEclipse(
    val start: LocalDateTime,
    val end: LocalDateTime,
    val isTotal: Boolean
) {
    val peak: LocalDateTime
        get() {
            return start.plus(Duration.between(start, end).dividedBy(2))
        }
}
