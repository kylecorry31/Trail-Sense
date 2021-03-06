package com.kylecorry.trail_sense.weather.domain

import com.kylecorry.trailsensecore.domain.units.Pressure
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
        return Pressure(pressure, PressureUnits.Hpa).convertTo(units).pressure
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