package com.kylecorry.trail_sense.shared.specifications

import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.trail_sense.shared.ApproximateCoordinate

class ApproximatelySameLocationSpecification(private val firstLocation: ApproximateCoordinate): Specification<ApproximateCoordinate>() {
    override fun isSatisfiedBy(value: ApproximateCoordinate): Boolean {
        val distance = value.coordinate.distanceTo(firstLocation.coordinate)
        val maxDistance = value.accuracy.meters().value * 2 + firstLocation.accuracy.meters().value * 2
        return distance <= maxDistance
    }
}