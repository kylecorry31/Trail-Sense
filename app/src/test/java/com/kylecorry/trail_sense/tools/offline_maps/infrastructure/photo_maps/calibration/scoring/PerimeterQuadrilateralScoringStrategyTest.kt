package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration.scoring

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.algebra.Matrix
import com.kylecorry.sol.math.geometry.Gradients
import com.kylecorry.sol.math.geometry.Polygon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PerimeterQuadrilateralScoringStrategyTest {

    @Test
    fun scoreReturnsQuadrilateralPerimeter() {
        val quadrilateral = Polygon(
            listOf(
                Vector2(0f, 0f),
                Vector2(3f, 0f),
                Vector2(3f, 4f),
                Vector2(0f, 4f)
            )
        )
        val matrix = Matrix.create(arrayOf(floatArrayOf(0f)))
        val gradients = Gradients(matrix, matrix, matrix)

        val score = PerimeterQuadrilateralScoringStrategy().score(quadrilateral, gradients, 1f)

        assertEquals(14f, score, 0.0001f)
    }
}
