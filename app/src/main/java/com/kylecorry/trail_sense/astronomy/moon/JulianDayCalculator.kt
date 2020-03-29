package com.kylecorry.trail_sense.astronomy.moon

import java.time.LocalDateTime
import kotlin.math.floor

object JulianDayCalculator {
    fun calculate(date: LocalDateTime): Double {
        var y = date.year
        var m = date.monthValue
        val d = date.dayOfMonth + date.hour / 24.0 + date.minute / (60.0 * 24) + date.second / (60.0 * 60 * 24)

        if (m <= 2){
            y--
            m += 12
        }

        val a = floor(y / 100.0)

        val b = 2 - a + floor(a / 4.0)

        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + d + b - 1524.5
    }
}