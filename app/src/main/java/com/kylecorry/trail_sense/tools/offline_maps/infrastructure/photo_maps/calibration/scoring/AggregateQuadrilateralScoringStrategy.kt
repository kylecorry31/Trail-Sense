package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration.scoring

import com.kylecorry.sol.math.geometry.Gradients
import com.kylecorry.sol.math.geometry.Polygon
import com.kylecorry.sol.math.sumOfFloat

class AggregateQuadrilateralScoringStrategy(
    private val strategies: List<Pair<QuadrilateralScoringStrategy, Float>>
) : QuadrilateralScoringStrategy {
    override fun score(
        quadrilateral: Polygon,
        gradients: Gradients,
        gradientThreshold: Float
    ): Float {
        return strategies.sumOfFloat { it.first.score(quadrilateral, gradients, gradientThreshold) * it.second }
    }
}
