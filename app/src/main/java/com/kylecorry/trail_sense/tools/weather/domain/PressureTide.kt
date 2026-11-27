package com.kylecorry.trail_sense.tools.weather.domain

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import java.time.Instant
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

object PressureTide {

    // WHY: Global fits of the surface pressure tides from Haurwitz (1956, 1965), as given in Lindzen's Dynamics in Atmospheric Physics, eq. 9.1 and 9.2
    private const val SEMIDIURNAL_AMPLITUDE_HPA = 1.16
    private const val SEMIDIURNAL_PHASE_DEGREES = 158.0
    private const val STANDING_SEMIDIURNAL_AMPLITUDE_HPA = 0.085
    private const val STANDING_SEMIDIURNAL_PHASE_DEGREES = 118.0
    private const val DIURNAL_AMPLITUDE_HPA = 0.593
    private const val DIURNAL_PHASE_DEGREES = 12.0
    private const val SECONDS_PER_HOUR = 3600.0
    private const val DEGREES_PER_HOUR = 15.0

    fun getOffsetHpa(time: Instant, location: Coordinate): Float {
        return (getSemidiurnalOffsetHpa(time, location) +
            getStandingSemidiurnalOffsetHpa(time, location) +
            getDiurnalOffsetHpa(time, location)).toFloat()
    }

    fun remove(reading: Reading<Pressure>, location: Coordinate): Reading<Pressure> {
        return shift(reading, -getOffsetHpa(reading.time, location))
    }

    fun add(reading: Reading<Pressure>, location: Coordinate): Reading<Pressure> {
        return shift(reading, getOffsetHpa(reading.time, location))
    }

    internal fun getSemidiurnalOffsetHpa(time: Instant, location: Coordinate): Double {
        return SEMIDIURNAL_AMPLITUDE_HPA * cosLatitude(location).pow(3) *
            sinDegrees(2 * getLocalAngle(time, location) + SEMIDIURNAL_PHASE_DEGREES)
    }

    internal fun getStandingSemidiurnalOffsetHpa(time: Instant, location: Coordinate): Double {
        val sinLatitude = sin(Math.toRadians(location.latitude))
        val legendre = (3 * sinLatitude * sinLatitude - 1) / 2
        return STANDING_SEMIDIURNAL_AMPLITUDE_HPA * legendre *
            sinDegrees(2 * getUniversalAngle(time) + STANDING_SEMIDIURNAL_PHASE_DEGREES)
    }

    internal fun getDiurnalOffsetHpa(time: Instant, location: Coordinate): Double {
        return DIURNAL_AMPLITUDE_HPA * cosLatitude(location).pow(3) *
            sinDegrees(getLocalAngle(time, location) + DIURNAL_PHASE_DEGREES)
    }

    private fun getUniversalAngle(time: Instant): Double {
        return time.epochSecond / SECONDS_PER_HOUR * DEGREES_PER_HOUR
    }

    private fun getLocalAngle(time: Instant, location: Coordinate): Double {
        return getUniversalAngle(time) + location.longitude
    }

    private fun cosLatitude(location: Coordinate): Double {
        return cos(Math.toRadians(location.latitude))
    }

    private fun sinDegrees(degrees: Double): Double {
        return sin(Math.toRadians(degrees))
    }

    private fun shift(reading: Reading<Pressure>, hpa: Float): Reading<Pressure> {
        val shifted = Pressure.hpa(reading.value.hpa().value + hpa).convertTo(reading.value.units)
        return reading.copy(value = shifted)
    }
}
