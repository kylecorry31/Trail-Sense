package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration.selection

import com.kylecorry.luna.specifications.Specification
import kotlin.math.min

class HasValidEdgeLengthsSpecification(private val minEdgeLengthRatio: Float) :
    Specification<QuadrilateralSelectionCriteria>() {
    override fun isSatisfiedBy(value: QuadrilateralSelectionCriteria): Boolean {
        val minDimension = min(value.imageWidth, value.imageHeight).toFloat()
        val minEdgeLength = minDimension * minEdgeLengthRatio
        val points = value.quadrilateral.vertices
        val topWidth = points[0].distanceTo(points[1])
        val bottomWidth = points[3].distanceTo(points[2])
        val leftHeight = points[0].distanceTo(points[3])
        val rightHeight = points[1].distanceTo(points[2])

        val hasValidHorizontalEdges = topWidth >= minEdgeLength && bottomWidth >= minEdgeLength
        val hasValidVerticalEdges = leftHeight >= minEdgeLength && rightHeight >= minEdgeLength
        return hasValidHorizontalEdges && hasValidVerticalEdges
    }
}
