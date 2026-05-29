package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration.scoring

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.algebra.Matrix
import com.kylecorry.sol.math.geometry.Gradients
import com.kylecorry.sol.math.geometry.Polygon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AggregateQuadrilateralScoringStrategyTest {

    @Test
    fun scoreReturnsWeightedSum() {
        val quadrilateral = Polygon(
            listOf(
                Vector2(0f, 0f),
                Vector2(2f, 0f),
                Vector2(2f, 1f),
                Vector2(0f, 1f)
            )
        )
        val matrix = Matrix.create(arrayOf(floatArrayOf(1f)))
        val gradients = Gradients(matrix, matrix, matrix)
        val strategy = AggregateQuadrilateralScoringStrategy(
            listOf(
                constantScoreStrategy(3f) to 2f,
                constantScoreStrategy(5f) to 0.5f,
                constantScoreStrategy(-1f) to 4f
            )
        )

        val score = strategy.score(quadrilateral, gradients, 0.5f)

        assertEquals(4.5f, score, 0.0001f)
    }

    private fun constantScoreStrategy(score: Float): QuadrilateralScoringStrategy {
        return object : QuadrilateralScoringStrategy {
            override fun score(
                quadrilateral: Polygon,
                gradients: Gradients,
                gradientThreshold: Float
            ): Float {
                return score
            }
        }
    }
}
