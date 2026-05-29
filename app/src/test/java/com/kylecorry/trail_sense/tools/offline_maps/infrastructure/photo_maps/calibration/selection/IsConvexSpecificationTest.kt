package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration.selection

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.geometry.Polygon
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IsConvexSpecificationTest {

    @Test
    fun isSatisfiedByRejectsConcaveQuadrilateral() {
        val specification = IsConvexSpecification()

        assertTrue(specification.isSatisfiedBy(criteria(rectangle(4f, 4f))))
        assertFalse(specification.isSatisfiedBy(criteria(concaveQuadrilateral())))
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

    private fun concaveQuadrilateral(): Polygon {
        return Polygon(
            listOf(
                Vector2(0f, 0f),
                Vector2(4f, 0f),
                Vector2(1f, 1f),
                Vector2(0f, 4f)
            )
        )
    }
}
