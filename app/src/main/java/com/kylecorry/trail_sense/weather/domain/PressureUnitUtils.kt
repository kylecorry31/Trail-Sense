package com.kylecorry.trail_sense.weather.domain

import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.PressureUnits

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

}