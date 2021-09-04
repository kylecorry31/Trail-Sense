package com.kylecorry.trail_sense.shared.specifications

import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.andromeda.core.units.DistanceUnits

class IsLargeUnitSpecification: Specification<DistanceUnits>() {
    override fun isSatisfiedBy(value: DistanceUnits): Boolean {
        val largeUnits = listOf(DistanceUnits.Miles, DistanceUnits.Kilometers, DistanceUnits.NauticalMiles)
        return largeUnits.contains(value)
    }
}