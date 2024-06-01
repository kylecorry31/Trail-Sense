package com.kylecorry.trail_sense.shared

import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits

fun Distance.toRelativeDistance(threshold: Float = 1000f): Distance {
    val metric = units.isMetric
    val baseDistance =
        if (metric) convertTo(DistanceUnits.Meters) else convertTo(DistanceUnits.Feet)
    val newUnits = if (baseDistance.distance >= threshold) {
        if (metric) DistanceUnits.Kilometers else DistanceUnits.Miles
    } else {
        if (metric) DistanceUnits.Meters else DistanceUnits.Feet
    }
    return convertTo(newUnits)
}

fun DistanceUnits.isLarge(): Boolean {
    // If it is greater than 100 meters per unit, then it is large
    return meters > 100f
}