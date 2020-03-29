package com.kylecorry.trail_sense.astronomy.sun

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class SunTimes(val up: LocalDateTime, val down: LocalDateTime) {

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