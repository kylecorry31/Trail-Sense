package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration.selection

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.geometry.Polygon
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HasValidCornerAnglesSpecificationTest {

    @Test
    fun isSatisfiedByRejectsSharpCorners() {
        val specification = HasValidCornerAnglesSpecification(30f)

        assertTrue(specification.isSatisfiedBy(criteria(rectangle(10f, 5f))))
        assertFalse(specification.isSatisfiedBy(criteria(skewedThinQuadrilateral())))
    }

    private fun criteria(polygon: Polygon): QuadrilateralSelectionCriteria {
        return QuadrilateralSelectionCriteria(polygon, 10, 10)
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

    private fun skewedThinQuadrilateral(): Polygon {
        return Polygon(
            listOf(
                Vector2(0f, 0f),
                Vector2(10f, 0f),
                Vector2(20f, 1f),
                Vector2(10f, 1f)
            )
        )
    }
}
