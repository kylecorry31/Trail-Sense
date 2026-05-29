package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration.scoring

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.algebra.Matrix
import com.kylecorry.sol.math.geometry.Gradients
import com.kylecorry.sol.math.geometry.Polygon
import com.kylecorry.sol.math.sumOfFloat
import kotlin.math.max
import kotlin.math.roundToInt

class EdgeMagnitudeQuadrilateralScoringStrategy : QuadrilateralScoringStrategy {
    override fun score(
        quadrilateral: Polygon,
        gradients: Gradients,
        gradientThreshold: Float
    ): Float {
        return quadrilateral.edges.sumOfFloat {
            scoreEdge(
                it.start,
                it.end,
                gradients.magnitude,
                gradients.magnitude.columns(),
                gradients.magnitude.rows(),
                gradientThreshold
            )
        }
    }

    private fun scoreEdge(
        start: PixelCoordinate,
        end: PixelCoordinate,
        gradientMagnitude: Matrix,
        width: Int,
        height: Int,
        threshold: Float
    ): Float {
        val length = start.distanceTo(end)
        val steps = max(1, length.roundToInt())
        var score = 0f

        for (step in 0..steps) {
            val t = step / steps.toFloat()
            val x = start.x + (end.x - start.x) * t
            val y = start.y + (end.y - start.y) * t
            score += sampleEdgeStrength(gradientMagnitude, width, height, x, y, threshold)
        }

        return score / (steps + 1)
    }

    private fun sampleEdgeStrength(
        gradientMagnitude: Matrix,
        width: Int,
        height: Int,
        x: Float,
        y: Float,
        threshold: Float
    ): Float {
        val actualX = x.roundToInt().coerceIn(0, width - 1)
        val actualY = y.roundToInt().coerceIn(0, height - 1)
        return (gradientMagnitude[actualY, actualX] - threshold) / threshold.coerceIn(0f, 1f)
    }
}
