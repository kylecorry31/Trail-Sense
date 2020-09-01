package com.kylecorry.trail_sense.weather.domain

import java.text.DecimalFormat

/**
 * A collection of weather utilities
 */
object PressureUnitUtils {

    fun getUnits(units: String): PressureUnits {
        return when (units) {
            "hpa" -> PressureUnits.Hpa
            "in" -> PressureUnits.Inhg
            "mbar" -> PressureUnits.Mbar
            "psi" -> PressureUnits.Psi
            else -> PressureUnits.Hpa
        }
    }

    /**
     * Converts a pressure in hPa to another unit
     * @param pressure The pressure in hPa
     * @param units The new units for the pressure
     * @return The pressure in the new units
     */
    fun convert(pressure: Float, units: PressureUnits): Float {
        return when (units) {
            PressureUnits.Hpa -> pressure
            PressureUnits.Inhg -> 0.02953f * pressure
            PressureUnits.Mbar -> pressure
            PressureUnits.Psi -> 0.01450f * pressure
        }
    }

    /**
     * Gets the symbol for the pressure units
     * @param units The pressure units
     * @return The symbol for the units
     */
    fun getSymbol(units: String): String {
        return when (units) {
            "hpa" -> "hPa"
            "in" -> "in"
            "mbar" -> "mbar"
            "psi" -> "PSI"
            else -> "hPa"
        }
    }

    fun getSymbol(units: PressureUnits): String {
        return when (units) {
            PressureUnits.Hpa -> "hPa"
            PressureUnits.Inhg -> "in"
            PressureUnits.Mbar -> "mbar"
            PressureUnits.Psi -> "PSI"
        }
    }

    fun getDecimalFormat(units: String): DecimalFormat {
        return when(units){
            "hpa", "mbar" -> DecimalFormat("0")
            else -> DecimalFormat("0.##")
        }
    }

    fun getDecimalFormat(units: PressureUnits): DecimalFormat {
        return when(units){
            PressureUnits.Hpa, PressureUnits.Mbar -> DecimalFormat("0")
            else -> DecimalFormat("0.##")
        }
    }

    fun getTendencyDecimalFormat(units: PressureUnits): DecimalFormat {
        return when(units){
            PressureUnits.Hpa, PressureUnits.Mbar -> DecimalFormat("0.#")
            else -> DecimalFormat("0.###")
        }
    }
}