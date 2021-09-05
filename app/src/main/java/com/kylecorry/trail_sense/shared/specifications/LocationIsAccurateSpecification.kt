package com.kylecorry.trail_sense.shared.specifications

import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.trail_sense.shared.ApproximateCoordinate

class LocationIsAccurateSpecification: Specification<ApproximateCoordinate>() {
    override fun isSatisfiedBy(value: ApproximateCoordinate): Boolean {
        return value.accuracy.meters().distance <= 16f
    }
}