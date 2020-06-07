package com.kylecorry.trail_sense.astronomy.domain.moon

import com.kylecorry.trail_sense.astronomy.domain.AstronomicalAltitudeCalculator
import com.kylecorry.trail_sense.shared.domain.Coordinate
import java.time.LocalDate
import kotlin.math.*

// Adapted from https://github.com/mourner/suncalc/blob/master/suncalc.js
class AltitudeMoonTimesCalculator : IMoonTimesCalculator {
    override fun calculate(location: Coordinate, date: LocalDate): MoonTimes {
        val altitudeCalculator =
            AstronomicalAltitudeCalculator()
        val hc = Math.toRadians(0.133)

        val startTime = date.atStartOfDay()

        var h0 = altitudeCalculator.getMoonAltitude(location, startTime).altitudeRadians - hc

        var rise = 0.0
        var set = 0.0

        var x1 = 0.0
        var x2 = 0.0

        for (i in 1..24 step 2){
            val h1 = altitudeCalculator.getMoonAltitude(location, startTime.plusHours(i.toLong())).altitudeRadians - hc
            val h2 = altitudeCalculator.getMoonAltitude(location, startTime.plusHours(i.toLong() + 1)).altitudeRadians - hc
            val a = (h0 + h2) / 2.0 - h1
            val b = (h2 - h0) / 2.0
            val xe = -b / (2 * a)
            val ye = (a * xe + b) * xe + h1
            val d = b.pow(2) - 4 * a * h1

            var roots = 0

            if (d >= 0){
                val dx = sqrt(d) / (abs(a) * 2)
                x1 = xe - dx
                x2 = xe + dx
                if (abs(x1) <= 1){
                    roots++
                }

                if (abs(x2) <= 1){
                    roots++
                }

                if (x1 < -1){
                    x1 = x2
                }
            }

            if (roots == 1){
                if (h0 < 0) rise = i + x1
                else set = i + x1
            } else if (roots == 2){
                rise = i + (if (ye < 0) x2 else x1)
                set = i + (if (ye < 0) x1 else x2)
            }

            if (rise != 0.0 && set != 0.0){
                break
            }

            h0 = h2
        }

        val riseTime = if (rise != 0.0){
            startTime.plusSeconds((rise * 60 * 60).roundToLong())
        } else {
            null
        }

        val setTime = if (set != 0.0){
            startTime.plusSeconds((set * 60 * 60).roundToLong())
        } else {
            null
        }

        return MoonTimes(riseTime, setTime)
    }



}