package com.kylecorry.trail_sense.astronomy.domain

import com.kylecorry.trail_sense.navigation.domain.compass.Bearing
import com.kylecorry.trail_sense.shared.Coordinate
import com.kylecorry.trail_sense.shared.math.toRadians
import com.kylecorry.trail_sense.shared.toZonedDateTime
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import kotlin.math.*

// Adapted from https://github.com/mourner/suncalc/blob/master/suncalc.js
class AstronomicalAltitudeCalculator {

    fun getMoonAltitudes(location: Coordinate, date: LocalDate, granularityMinutes: Int = 15): List<AstroAltitude> {
        val totalTime = 24 * 60
        val altitudes = mutableListOf<AstroAltitude>()
        for (i in 0..totalTime step granularityMinutes){
            altitudes.add(getMoonAltitude(location, date.atStartOfDay().plusMinutes(i.toLong())))
        }
        return altitudes
    }

    fun getMoonAzimuth(location: Coordinate, time: LocalDateTime): Bearing {
        val lw = Math.toRadians(-location.longitude)
        val phi = Math.toRadians(location.latitude)

        val days = time.toZonedDateTime().toEpochSecond() / dayS - 0.5 + J1970 - J2000

        val c = getMoonCoordinates(days)

        val H = getSiderealTime(days, lw) - c.ra

        return Bearing(Math.toDegrees(calculateAzimuth(H, phi, c.declination)).toFloat())
    }

    fun getMoonAltitude(location: Coordinate, time: LocalDateTime): AstroAltitude {
        val lw = Math.toRadians(-location.longitude)
        val phi = Math.toRadians(location.latitude)

        val days = time.toZonedDateTime().toEpochSecond() / dayS - 0.5 + J1970 - J2000

        val c = getMoonCoordinates(days)

        val H = getSiderealTime(days, lw) - c.ra

        return AstroAltitude(time, Math.toDegrees(calculateAltitude(H, phi, c.declination)).toFloat())
    }

    fun getSunAltitudes(location: Coordinate, date: LocalDate, granularityMinutes: Long = 15): List<AstroAltitude> {
        val totalTime = 24 * 60L
        val altitudes = mutableListOf<AstroAltitude>()
        for (i in 0..totalTime step granularityMinutes){
            altitudes.add(getSunAltitude(location, date.atStartOfDay().plusMinutes(i)))
        }
        return altitudes
    }

    fun getSunAzimuth(location: Coordinate, time: LocalDateTime): Bearing {
        val lw  = -location.longitude.toRadians()
        val phi = location.latitude.toRadians()
        val d = time.toZonedDateTime().toEpochSecond() / dayS - 0.5 + J1970 - J2000

        val c  = getSunCoordinates(d)
        val H  = getSiderealTime(d, lw) - c.ra;

        return Bearing(Math.toDegrees(calculateAzimuth(H, phi, c.declination)).toFloat())
    }

    fun getSunAltitude(location: Coordinate, time: LocalDateTime): AstroAltitude {
        val lw  = -location.longitude.toRadians()
        val phi = location.latitude.toRadians()
        val d = time.toZonedDateTime().toEpochSecond() / dayS - 0.5 + J1970 - J2000

        val c  = getSunCoordinates(d)
        val H  = getSiderealTime(d, lw) - c.ra;

        return AstroAltitude(time, Math.toDegrees(calculateAltitude(H, phi, c.declination)).toFloat())
    }

    private fun getSunCoordinates(days: Double): AstroCoordinates {
        val M = (357.5291 + 0.98560028 * days).toRadians()
        val L = getEclipticLongitude(M)

        return AstroCoordinates(
            calculateRightAscension(L, 0.0),
            calculateDeclination(L, 0.0)
        )
    }

    private fun getEclipticLongitude(M: Double): Double {
        val C = (1.9148 * sin(M) + 0.02 * sin(2 * M) + 0.0003 * sin(3 * M)).toRadians()
        val P = 102.9372.toRadians()
        return M + C + P + PI
    }

    private fun getMoonCoordinates(days: Double): AstroCoordinates {

        val L = Math.toRadians(218.316 + 13.176396 * days)
        val M = Math.toRadians(134.963 + 13.064993 * days)
        val F = Math.toRadians(93.272 + 13.229350 * days)

        val l = L + Math.toRadians(6.289 * sin(M))
        val b = Math.toRadians(5.128 * sin(F))

        return AstroCoordinates(
            calculateRightAscension(l, b),
            calculateDeclination(l, b)
        )
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

    private fun calculateAzimuth(H: Double, phi: Double, declination: Double): Double {
        return atan2(sin(H), cos(H) * sin(phi) - tan(declination) * cos(phi))
    }

    private fun calculateRightAscension(l: Double, b: Double): Double {
        return atan2(sin(l) * cos(e) - tan(b) * sin(
            e
        ), cos(l))
    }

    private fun calculateDeclination(l: Double, b: Double): Double {
        return asin(sin(b) * cos(e) + cos(b) * sin(
            e
        ) * sin(l))
    }

    private data class AstroCoordinates(val ra: Double, val declination: Double)

    companion object {
        private val e = Math.toRadians(23.4397)
        private const val J2000 = 2451545.0
        private const val J1970 = 2440588.0
        private const val dayS = 60 * 60 * 24.0
    }

}