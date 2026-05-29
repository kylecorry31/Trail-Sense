package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration.selection

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.geometry.Polygon
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HasValidEdgeLengthsSpecificationTest {

    @Test
    fun isSatisfiedByUsesSmallestImageDimension() {
        val specification = HasValidEdgeLengthsSpecification(0.3f)

        assertTrue(specification.isSatisfiedBy(criteria(rectangle(4f, 5f), 20, 10)))
        assertFalse(specification.isSatisfiedBy(criteria(rectangle(2.5f, 5f), 20, 10)))
    }

    private fun criteria(polygon: Polygon, imageWidth: Int, imageHeight: Int): QuadrilateralSelectionCriteria {
        return QuadrilateralSelectionCriteria(polygon, imageWidth, imageHeight)
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
