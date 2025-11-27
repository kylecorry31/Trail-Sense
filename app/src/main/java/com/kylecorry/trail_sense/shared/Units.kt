package com.kylecorry.trail_sense.shared

import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.PressureUnits
import com.kylecorry.sol.units.WeightUnits

object Units {

    fun getDecimalPlaces(units: PressureUnits): Int {
        return when (units) {
            PressureUnits.Inhg, PressureUnits.Psi -> 2
            else -> 1
        }
    }

    fun getDecimalPlaces(units: DistanceUnits): Int {
        return if (units.isLarge()) {
            2
        } else {
            0
        }
    }

    fun getDecimalPlaces(units: WeightUnits): Int {
        return when (units) {
            WeightUnits.Ounces -> 1
            WeightUnits.Pounds, WeightUnits.Kilograms -> 2
            WeightUnits.Grams, WeightUnits.Milligrams, WeightUnits.Grains -> 0
        }
    }

}