package com.kylecorry.trail_sense.weather

import com.kylecorry.trail_sense.models.PressureAltitudeReading
import com.kylecorry.trail_sense.models.PressureReading
import com.kylecorry.trail_sense.models.PressureTendency
import java.text.DecimalFormat

/**
 * A collection of weather utilities
 */
object WeatherUtils {

    private val pressureCalibrator: IPressureCalibrator = SeaLevelPressureCalibrator()
    private val pressureTendencyCalculator: IPressureTendencyCalculator = PressureTendencyCalculator()

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
    fun getPressureTendency(readings: List<PressureAltitudeReading>, useSeaLevelPressure: Boolean = false): PressureTendency {
        val calibratedReadings = readings.map {
            PressureReading(
                it.time,
                getCalibratedReading(it, useSeaLevelPressure)
            )
        }
        return pressureTendencyCalculator.getPressureTendency(calibratedReadings)
    }

    private fun getCalibratedReading(reading: PressureAltitudeReading, useSeaLevelPressure: Boolean = false): Float {
        if (useSeaLevelPressure){
            return convertToSeaLevelPressure(reading.pressure, reading.altitude.toFloat())
        }
        return reading.pressure
    }

    /**
     * Determines if a storm is incoming
     * @param readings The barometric pressure readings in hPa
     * @return True if a storm is incoming, false otherwise
     */
    fun isStormIncoming(readings: List<PressureAltitudeReading>, useSeaLevelPressure: Boolean = false): Boolean {
        val calibratedReadings = readings.map {
            PressureReading(
                it.time,
                getCalibratedReading(it, useSeaLevelPressure)
            )
        }
        return StormDetector().isStormIncoming(calibratedReadings)
    }

    /**
     * Converts a pressure to sea level
     * @param pressure The barometric pressure in hPa
     * @param altitude The altitude of the device in meters
     * @return The pressure at sea level in hPa
     */
    fun convertToSeaLevelPressure(pressure: Float, altitude: Float): Float {
        return pressureCalibrator.getCalibratedPressure(pressure, altitude)
    }

}