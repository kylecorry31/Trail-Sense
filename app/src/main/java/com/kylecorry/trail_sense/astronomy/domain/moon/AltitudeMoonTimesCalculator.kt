package com.kylecorry.trail_sense.astronomy.domain.moon

import com.kylecorry.trail_sense.shared.Coordinate
import com.kylecorry.trail_sense.shared.toZonedDateTime
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import kotlin.math.*

// Adapted from https://github.com/mourner/suncalc/blob/master/suncalc.js
class AltitudeMoonTimesCalculator : IMoonTimesCalculator {
    override fun calculate(location: Coordinate, date: LocalDate): MoonTimes {
        val hc = Math.toRadians(0.133)

        val startTime = date.atStartOfDay()

        var h0 = getMoonAltitude(location, startTime) - hc

        var rise = 0.0
        var set = 0.0

        var x1 = 0.0
        var x2 = 0.0

        for (i in 1..24 step 2){
            val h1 = getMoonAltitude(location, startTime.plusHours(i.toLong())) - hc
            val h2 = getMoonAltitude(location, startTime.plusHours(i.toLong() + 1)) - hc
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

    private fun getMoonAltitude(location: Coordinate, time: LocalDateTime): Double {
        val lw = Math.toRadians(-location.longitude)
        val phi = Math.toRadians(location.latitude)

        val days = time.toZonedDateTime().toEpochSecond() / dayS - 0.5 + J1970 - J2000

        val c = getMoonCoordinates(days)

        val H = getSiderealTime(days, lw) - c.ra

        return calculateAltitude(H, phi, c.declination)
    }

    private fun getMoonCoordinates(days: Double): MoonCoordinates {

        val L = Math.toRadians(218.316 + 13.176396 * days)
        val M = Math.toRadians(134.963 + 13.064993 * days)
        val F = Math.toRadians(93.272 + 13.229350 * days)

        val l = L + Math.toRadians(6.289 * sin(M))
        val b = Math.toRadians(5.128 * sin(F))

        return MoonCoordinates(calculateRightAscension(l, b), calculateDeclination(l, b))
    }

    private fun getSiderealTime(days: Double, lw: Double): Double {
        return Math.toRadians(280.16 + 360.9856235 * days) - lw
    }

    private fun calculateAltitude(H: Double, phi: Double, declination: Double): Double {
        val altitude = asin(sin(phi) * sin(declination) + cos(phi) * cos(declination) * cos(H))
        val h = max(0.0, altitude)
        val refraction = 0.0002967 / tan(h + 0.00312536 / (h + 0.08901179))
        return altitude + refraction
    }

    private fun calculateRightAscension(l: Double, b: Double): Double {
        return atan2(sin(l) * cos(e) - tan(b) * sin(e), cos(l))
    }

    private fun calculateDeclination(l: Double, b: Double): Double {
        return asin(sin(b) * cos(e) + cos(b) * sin(e) * sin(l))
    }

    private data class MoonCoordinates(val ra: Double, val declination: Double)


    companion object {
        private val e = Math.toRadians(23.4397)
        private const val J2000 = 2451545.0
        private const val J1970 = 2440588.0
        private const val dayS = 60 * 60 * 24.0
    }

}