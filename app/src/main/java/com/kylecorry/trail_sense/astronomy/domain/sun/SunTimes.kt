package com.kylecorry.trail_sense.astronomy.domain.sun

import org.threeten.bp.Duration
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime

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