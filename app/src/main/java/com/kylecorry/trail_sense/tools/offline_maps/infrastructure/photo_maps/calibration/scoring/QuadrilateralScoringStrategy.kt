package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration.scoring

import com.kylecorry.sol.math.geometry.Gradients
import com.kylecorry.sol.math.geometry.Polygon

interface QuadrilateralScoringStrategy {
    fun score(quadrilateral: Polygon, gradients: Gradients, gradientThreshold: Float): Float
}
