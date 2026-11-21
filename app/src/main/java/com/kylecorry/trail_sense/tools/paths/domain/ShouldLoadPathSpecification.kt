package com.kylecorry.trail_sense.tools.paths.domain

import com.kylecorry.luna.specifications.Specification
import com.kylecorry.sol.science.geology.CoordinateBounds

class ShouldLoadPathSpecification(
    private val bounds: CoordinateBounds,
    private val backtrackId: Long? = null
) : Specification<Path>() {
    override fun isSatisfiedBy(value: Path): Boolean {
        return value.id == backtrackId || value.metadata.bounds.intersects(bounds)
    }
}
