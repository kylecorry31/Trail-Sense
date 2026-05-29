package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration.selection

import com.kylecorry.luna.specifications.Specification
import com.kylecorry.sol.math.geometry.Geometry

class HasValidCornerAnglesSpecification(private val minCornerAngleDegrees: Float) : Specification<QuadrilateralSelectionCriteria>() {
    override fun isSatisfiedBy(value: QuadrilateralSelectionCriteria): Boolean {
        val points = value.quadrilateral.vertices
        return points.indices.all { i ->
            val previous = points[(i - 1 + points.size) % points.size]
            val current = points[i]
            val next = points[(i + 1) % points.size]
            val angle = Geometry.getInteriorAngle(previous, current, next)
            angle in minCornerAngleDegrees..(180f - minCornerAngleDegrees)
        }
    }
}
