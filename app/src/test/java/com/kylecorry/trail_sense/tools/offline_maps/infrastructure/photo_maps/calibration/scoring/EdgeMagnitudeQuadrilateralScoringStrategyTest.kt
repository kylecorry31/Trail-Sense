package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration.scoring

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.algebra.Matrix
import com.kylecorry.sol.math.geometry.Gradients
import com.kylecorry.sol.math.geometry.Polygon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EdgeMagnitudeQuadrilateralScoringStrategyTest {

    private val strategy = EdgeMagnitudeQuadrilateralScoringStrategy()

    @Test
    fun scoreReturnsSumOfPerEdgeAverageStrengths() {
        val quadrilateral = rectangle(2f, 2f)
        val gradients = gradients(
            arrayOf(
                floatArrayOf(2f, 3f, 4f),
                floatArrayOf(5f, 6f, 7f),
                floatArrayOf(8f, 9f, 10f)
            )
        )

        val score = strategy.score(quadrilateral, gradients, 1f)

        assertEquals(20f, score, 0.0001f)
    }

    @Test
    fun scoreClampsSamplesThatFallOutsideTheImage() {
        val quadrilateral = Polygon(
            listOf(
                Vector2(-1f, -1f),
                Vector2(2f, -1f),
                Vector2(2f, 2f),
                Vector2(-1f, 2f)
            )
        )
        val gradients = gradients(
            arrayOf(
                floatArrayOf(10f, 20f),
                floatArrayOf(30f, 40f)
            )
        )

        val score = strategy.score(quadrilateral, gradients, 1f)

        assertEquals(96f, score, 0.0001f)
    }

    @Test
    fun scoreUsesThresholdClampedToOneForNormalization() {
        val quadrilateral = rectangle(1f, 1f)
        val gradients = gradients(
            arrayOf(
                floatArrayOf(5f, 5f),
                floatArrayOf(5f, 5f)
            )
        )

        val score = strategy.score(quadrilateral, gradients, 2f)

        assertEquals(12f, score, 0.0001f)
    }

    private fun gradients(values: Array<FloatArray>): Gradients {
        val matrix = Matrix.create(values)
        return Gradients(matrix, matrix, matrix)
    }

    private fun rectangle(width: Float, height: Float): Polygon {
        return Polygon(
            listOf(
                Vector2(0f, 0f),
                Vector2(width, 0f),
                Vector2(width, height),
                Vector2(0f, height)
            )
        )
    }
}
