package com.kylecorry.trail_sense.shared

import com.kylecorry.trailsensecore.domain.units.Distance
import com.kylecorry.trailsensecore.domain.units.DistanceUnits

object DistanceUtils {
    fun Distance.toRelativeDistance(): Distance {
        val metric = listOf(DistanceUnits.Kilometers, DistanceUnits.Meters, DistanceUnits.Centimeters).contains(this.units)
        val baseDistance = if (metric) this.convertTo(DistanceUnits.Meters) else this.convertTo(DistanceUnits.Feet)
        val newUnits = if (baseDistance.distance > 1000){
            if (metric) DistanceUnits.Kilometers else DistanceUnits.Miles
        } else {
            if (metric) DistanceUnits.Meters else DistanceUnits.Feet
        }
        return this.convertTo(newUnits)
    }
}