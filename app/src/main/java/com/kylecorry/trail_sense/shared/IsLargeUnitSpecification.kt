package com.kylecorry.trail_sense.shared

import com.kylecorry.trailsensecore.domain.specifications.Specification
import com.kylecorry.trailsensecore.domain.units.DistanceUnits

class IsLargeUnitSpecification: Specification<DistanceUnits>() {
    override fun isSatisfiedBy(value: DistanceUnits): Boolean {
        val largeUnits = listOf(DistanceUnits.Miles, DistanceUnits.Kilometers, DistanceUnits.NauticalMiles)
        return largeUnits.contains(value)
    }
}