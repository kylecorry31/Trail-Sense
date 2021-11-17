package com.kylecorry.trail_sense.navigation.paths.domain

import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.intersects

/**
 * @param bounds The bounds to load the path in
 */
class ShouldLoadPathSpecification(private val bounds: CoordinateBounds) : Specification<Path>() {
    override fun isSatisfiedBy(value: Path): Boolean {
        return value.metadata.bounds.intersects(bounds)
    }
}