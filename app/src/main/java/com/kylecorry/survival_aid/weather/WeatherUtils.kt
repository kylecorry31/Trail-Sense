package com.kylecorry.survival_aid.weather

import android.util.Log
import java.time.Duration
import java.time.Instant
import kotlin.math.abs

/**
 * A collection of weather utilities
 */
object WeatherUtils {

    private const val CHANGE_THRESHOLD = 2
    private const val STORM_THRESHOLD = 6
    private val STORM_PREDICTION_DURATION = Duration.ofHours(3).plusMinutes(5)

    /**
     * Convert hPa to inches
     * @param pressure The pressure in hPa
     * @return The pressure in inches
     */
    fun hPaToInches(pressure: Float): Float {
        return 0.02953f * pressure
    }

    /**
     * Convert inHg to hPa
     * @param pressure The pressure in inHg
     * @return The pressure in hPa
     */
    fun inchesToHPa(pressure: Float): Float {
        return pressure / 0.02953f
    }

    enum class BarometricChange(val readableName: String) {
        FALLING("Weather worsening soon"),
        RISING("Weather improving soon"),
        NO_CHANGE("Weather not changing soon")
    }

    /**
     * Get the barometric pressure change direction
     * @param readings The barometric pressure readings in hPa
     * @return The direction of change
     */
    fun getBarometricChangeDirection(readings: List<PressureReading>): BarometricChange {
        val change = getBarometricChange(readings.filter { Duration.between(it.time, Instant.now()) <= STORM_PREDICTION_DURATION })

        return when {
            abs(change) < CHANGE_THRESHOLD -> BarometricChange.NO_CHANGE
            change < 0 -> BarometricChange.FALLING
            else -> BarometricChange.RISING
        }
    }

    /**
     * Get the barometric pressure change
     * @param readings The barometric pressure readings in hPa
     * @return The pressure change in hPa
     */
    fun getBarometricChange(readings: List<PressureReading>): Float {
        if (readings.size < 2) return 0f
        val firstReading = readings.first()
        val lastReading = readings.last()
        return lastReading.reading - firstReading.reading
    }

    /**
     * Determines if a storm is incoming
     * @param readings The barometric pressure readings in hPa
     * @return True if a storm is incoming, false otherwise
     */
    fun isStormIncoming(readings: List<PressureReading>): Boolean {
        val pressureDirection = getBarometricChangeDirection(readings)
        val pressureChange = getBarometricChange(readings.filter { Duration.between(it.time, Instant.now()) <= STORM_PREDICTION_DURATION })

        return pressureDirection == BarometricChange.FALLING && abs(pressureChange) >= STORM_THRESHOLD
    }

}