package com.kylecorry.trail_sense.shared

import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.shared.specifications.IsLargeUnitSpecification

fun Distance.toRelativeDistance(threshold: Float = 1000f): Distance {
    val metric = units.isMetric()
    val baseDistance =
        if (metric) convertTo(DistanceUnits.Meters) else convertTo(DistanceUnits.Feet)
    val newUnits = if (baseDistance.distance >= threshold) {
        if (metric) DistanceUnits.Kilometers else DistanceUnits.Miles
    } else {
        if (metric) DistanceUnits.Meters else DistanceUnits.Feet
    }
    return convertTo(newUnits)
}

fun DistanceUnits.isMetric(): Boolean {
    return listOf(
        DistanceUnits.Kilometers,
        DistanceUnits.Meters,
        DistanceUnits.Centimeters
    ).contains(this)
}

fun DistanceUnits.isLarge(): Boolean {
    return IsLargeUnitSpecification().isSatisfiedBy(this)
}