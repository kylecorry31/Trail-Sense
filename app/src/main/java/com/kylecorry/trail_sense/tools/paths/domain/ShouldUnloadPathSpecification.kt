package com.kylecorry.trail_sense.tools.paths.domain

import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.sol.science.geology.CoordinateBounds

/**
 * @param bounds The bounds to unload the path when it is outside
 */
class ShouldUnloadPathSpecification(
    private val bounds: CoordinateBounds,
    private val backtrackId: Long? = null
) : Specification<Path>() {
    override fun isSatisfiedBy(value: Path): Boolean {
        if (value.id == backtrackId) {
            return false
        }
        return !value.metadata.bounds.intersects(bounds)
    }
}