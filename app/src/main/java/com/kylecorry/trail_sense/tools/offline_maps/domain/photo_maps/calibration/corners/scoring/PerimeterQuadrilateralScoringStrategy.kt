package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.calibration.corners.scoring

import com.kylecorry.sol.math.geometry.Gradients
import com.kylecorry.sol.math.geometry.Polygon
import com.kylecorry.sol.math.sumOfFloat

internal class PerimeterQuadrilateralScoringStrategy : QuadrilateralScoringStrategy {
    override fun score(
        quadrilateral: Polygon,
        gradients: Gradients,
        gradientThreshold: Float
    ): Float {
        return quadrilateral.edges.sumOfFloat {
            it.start.distanceTo(it.end)
        }
    }
}
