package com.kylecorry.trail_sense.shared

import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.PressureUnits

object Units {

    fun getDecimalPlaces(units: PressureUnits): Int {
        return when (units) {
            PressureUnits.Inhg, PressureUnits.Psi -> 2
            else -> 1
        }
    }

    fun getDecimalPlaces(units: DistanceUnits): Int {
        return if (units.isLarge()){
            2
        } else {
            0
        }
    }

}