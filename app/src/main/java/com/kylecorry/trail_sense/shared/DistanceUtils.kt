package com.kylecorry.trail_sense.shared

import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits

object DistanceUtils {
    fun Distance.toRelativeDistance(): Distance {
        val metric = listOf(
            DistanceUnits.Kilometers,
            DistanceUnits.Meters,
            DistanceUnits.Centimeters,
            DistanceUnits.Millimeters
        ).contains(this.units)
        val baseDistance =
            if (metric) this.convertTo(DistanceUnits.Meters) else this.convertTo(DistanceUnits.Feet)
        val newUnits = if (baseDistance.value > 1000) {
            if (metric) DistanceUnits.Kilometers else DistanceUnits.Miles
        } else {
            if (metric) DistanceUnits.Meters else DistanceUnits.Feet
        }
        return this.convertTo(newUnits)
    }

    val hikingDistanceUnits = listOf(
        DistanceUnits.Feet,
        DistanceUnits.Yards,
        DistanceUnits.Miles,
        DistanceUnits.NauticalMiles,
        DistanceUnits.Meters,
        DistanceUnits.Kilometers
    )

    val elevationDistanceUnits = listOf(
        DistanceUnits.Feet,
        DistanceUnits.Meters
    )


    val humanDistanceUnits = listOf(
        DistanceUnits.Feet,
        DistanceUnits.Inches,
        DistanceUnits.Meters,
        DistanceUnits.Centimeters
    )

    val rulerDistanceUnits = listOf(
        DistanceUnits.Inches,
        DistanceUnits.Centimeters,
        DistanceUnits.Millimeters
    )

}
