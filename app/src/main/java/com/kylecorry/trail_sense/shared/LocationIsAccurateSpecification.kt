package com.kylecorry.trail_sense.shared

import com.kylecorry.trailsensecore.domain.specifications.Specification

class LocationIsAccurateSpecification: Specification<ApproximateCoordinate>() {
    override fun isSatisfiedBy(value: ApproximateCoordinate): Boolean {
        return value.accuracy.meters().distance <= 16f
    }
}