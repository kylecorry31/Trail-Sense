package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.calibration.corners.scoring

import com.kylecorry.sol.math.geometry.Gradients
import com.kylecorry.sol.math.geometry.Polygon

internal interface QuadrilateralScoringStrategy {
    fun score(quadrilateral: Polygon, gradients: Gradients, gradientThreshold: Float): Float
}
