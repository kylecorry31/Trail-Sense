package com.kylecorry.trail_sense.astronomy.domain.sun

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class SunTimes(val up: LocalDateTime, val down: LocalDateTime) {

    val noon: LocalDateTime = up.plus(Duration.between(up, down).dividedBy(2))

    companion object {
        fun unknown(date: LocalDate): SunTimes {
            return SunTimes(
                date.atTime(
                    LocalTime.MIN
                ), date.atTime(LocalTime.MAX)
            )
        }

    }

}