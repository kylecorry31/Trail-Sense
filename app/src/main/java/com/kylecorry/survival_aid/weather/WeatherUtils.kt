package com.kylecorry.survival_aid.weather

import java.text.DecimalFormat
import java.time.Duration
import java.time.Instant
import kotlin.math.pow

/**
 * A collection of weather utilities
 */
object WeatherUtils {

    private const val FAST_CHANGE_THRESHOLD = 2f
    private const val SLOW_CHANGE_THRESHOLD = 0.5f
    private const val STORM_THRESHOLD = 6f
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


    /**
     * Converts a pressure in hPa to another unit
     * @param pressure The pressure in hPa
     * @param units The new units for the pressure
     * @return The pressure in the new units
     */
    fun convertPressureToUnits(pressure: Float, units: String): Float {
        return when (units) {
            "hpa" -> pressure
            "in" -> 0.02953f * pressure
            "mbar" -> pressure
            "psi" -> 0.01450f * pressure
            else -> pressure
        }
    }

    /**
     * Gets the symbol for the pressure units
     * @param units The pressure units
     * @return The symbol for the units
     */
    fun getPressureSymbol(units: String): String {
        return when (units) {
            "hpa" -> "hPa"
            "in" -> "in"
            "mbar" -> "mbar"
            "psi" -> "PSI"
            else -> "hPa"
        }
    }

    fun getDecimalFormat(units: String): DecimalFormat {
        return when(units){
            "hpa", "mbar" -> DecimalFormat("0")
            else -> DecimalFormat("0.##")
        }
    }

    enum class PressureTendency(val readableName: String) {
        FALLING_SLOW("Weather may worsen"),
        RISING_SLOW("Weather may improve"),
        FALLING_FAST("Weather will worsen soon"),
        RISING_FAST("Weather will improve soon "),
        NO_CHANGE("Weather not changing")
    }

    /**
     * Determines if a tendency is falling
     * @param change The tendency
     * @return true if it is falling, false otherwise
     */
    fun isFalling(change: PressureTendency): Boolean {
        return change == PressureTendency.FALLING_FAST || change == PressureTendency.FALLING_SLOW
    }

    /**
     * Determines if a tendency is rising
     * @param change The tendency
     * @return true if it is rising, false otherwise
     */
    fun isRising(change: PressureTendency): Boolean {
        return change == PressureTendency.RISING_FAST || change == PressureTendency.RISING_SLOW
    }

    /**
     * Get the barometric pressure change direction
     * @param readings The barometric pressure readings in hPa
     * @return The direction of change
     */
    fun getPressureTendency(readings: List<PressureReading>, useSeaLevelPressure: Boolean = false): PressureTendency {
        val change = get3HourChange(readings, useSeaLevelPressure)

        return when {
            change <= -FAST_CHANGE_THRESHOLD -> PressureTendency.FALLING_FAST
            change <= -SLOW_CHANGE_THRESHOLD -> PressureTendency.FALLING_SLOW
            change >= FAST_CHANGE_THRESHOLD -> PressureTendency.RISING_FAST
            change >= SLOW_CHANGE_THRESHOLD -> PressureTendency.RISING_SLOW
            else -> PressureTendency.NO_CHANGE
        }
    }

    private fun get3HourChange(readings: List<PressureReading>, useSeaLevelPressure: Boolean = false): Float {
        val filtered = readings.filter { Duration.between(it.time, Instant.now()) <= STORM_PREDICTION_DURATION }
        if (filtered.size < 2) return 0f
        val firstReading = filtered.first()
        val lastReading = filtered.last()
        return getCalibratedReading(lastReading, useSeaLevelPressure) - getCalibratedReading(firstReading, useSeaLevelPressure)
    }

    private fun getCalibratedReading(reading: PressureReading, useSeaLevelPressure: Boolean = false): Float {
        if (useSeaLevelPressure){
            return convertToSeaLevelPressure(reading.reading, reading.altitude.toFloat())
        }
        return reading.reading
    }

    /**
     * Determines if a storm is incoming
     * @param readings The barometric pressure readings in hPa
     * @return True if a storm is incoming, false otherwise
     */
    fun isStormIncoming(readings: List<PressureReading>, useSeaLevelPressure: Boolean = false): Boolean {
        val threeHourChange = get3HourChange(readings, useSeaLevelPressure)
        return threeHourChange <= -STORM_THRESHOLD
    }

    /**
     * Converts a pressure to sea level
     * @param pressure The barometric pressure in hPa
     * @param altitude The altitude of the device in meters
     * @return The pressure at sea level in hPa
     */
    fun convertToSeaLevelPressure(pressure: Float, altitude: Float): Float {
        return pressure * (1 - altitude / 44330.0).pow(-5.255).toFloat()
    }

}