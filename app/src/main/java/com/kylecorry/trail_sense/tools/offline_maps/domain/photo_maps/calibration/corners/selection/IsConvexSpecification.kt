package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.calibration.corners.selection

import com.kylecorry.luna.specifications.Specification

internal class IsConvexSpecification : Specification<QuadrilateralSelectionCriteria>() {
    override fun isSatisfiedBy(value: QuadrilateralSelectionCriteria): Boolean {
        return value.quadrilateral.isConvex()
    }
}
