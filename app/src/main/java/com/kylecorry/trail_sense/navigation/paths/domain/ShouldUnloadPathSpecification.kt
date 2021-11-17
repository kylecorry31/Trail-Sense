package com.kylecorry.trail_sense.navigation.paths.domain

import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.intersects

/**
 * @param bounds The bounds to unload the path when it is outside
 */
class ShouldUnloadPathSpecification(private val bounds: CoordinateBounds) : Specification<Path>() {
    override fun isSatisfiedBy(value: Path): Boolean {
        return !value.metadata.bounds.intersects(bounds)
    }
}