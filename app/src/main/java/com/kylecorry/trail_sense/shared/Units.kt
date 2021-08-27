package com.kylecorry.trail_sense.shared

import com.kylecorry.andromeda.core.units.DistanceUnits
import com.kylecorry.andromeda.core.units.PressureUnits
import com.kylecorry.trailsensecore.domain.units.isLarge

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