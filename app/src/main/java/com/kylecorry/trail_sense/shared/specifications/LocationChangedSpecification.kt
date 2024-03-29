package com.kylecorry.trail_sense.shared.specifications

import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.ApproximateCoordinate

class LocationChangedSpecification(private val previousLocation: ApproximateCoordinate, private val minDistance: Distance): Specification<ApproximateCoordinate>() {
    override fun isSatisfiedBy(value: ApproximateCoordinate): Boolean {
        val isFarEnoughAway = InGeofenceSpecification(previousLocation.coordinate, minDistance).not()
        val areDifferentLocations = ApproximatelySameLocationSpecification(previousLocation).not()
        return isFarEnoughAway.isSatisfiedBy(value.coordinate) && areDifferentLocations.isSatisfiedBy(value)
    }
}