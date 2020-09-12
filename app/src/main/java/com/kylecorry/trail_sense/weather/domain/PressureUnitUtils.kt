package com.kylecorry.trail_sense.weather.domain

import com.kylecorry.trailsensecore.domain.units.PressureUnits
import java.text.DecimalFormat

/**
 * A collection of weather utilities
 */
object PressureUnitUtils {

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

    fun getSymbol(units: PressureUnits): String {
        return when (units) {
            PressureUnits.Hpa -> "hPa"
            PressureUnits.Inhg -> "in"
            PressureUnits.Mbar -> "mbar"
            PressureUnits.Psi -> "PSI"
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