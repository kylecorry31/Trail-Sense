package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.calibration.corners.selection

import com.kylecorry.luna.specifications.Specification
import com.kylecorry.sol.math.Range

internal class HasValidAreaSpecification(private val validPercentAreas: Range<Float>) :
    Specification<QuadrilateralSelectionCriteria>() {
    override fun isSatisfiedBy(value: QuadrilateralSelectionCriteria): Boolean {
        val area = value.quadrilateral.area()
        val imageArea = value.imageWidth * value.imageHeight.toFloat()
        return area in (imageArea * validPercentAreas.start)..(imageArea * validPercentAreas.end)
    }
}
