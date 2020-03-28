package com.kylecorry.trail_sense.weather

import java.text.DecimalFormat

/**
 * A collection of weather utilities
 */
object WeatherUtils {

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

    // TODO: Move these two out of Weather Utils

    fun isHighPressure(pressure: Float): Boolean {
        return pressure >= 1022.6
    }

    fun isLowPressure(pressure: Float): Boolean {
        return pressure <= 1009.14
    }

}