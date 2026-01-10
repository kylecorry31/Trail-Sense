package com.kylecorry.trail_sense.tools.paths.domain

import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.sol.science.geology.CoordinateBounds

/**
 * @param bounds The bounds to load the path in
 */
class ShouldLoadPathSpecification(
    private val bounds: CoordinateBounds,
    private val backtrackId: Long? = null
) : Specification<Path>() {
    override fun isSatisfiedBy(value: Path): Boolean {
        return value.id == backtrackId || value.metadata.bounds.intersects(bounds)
    }
}