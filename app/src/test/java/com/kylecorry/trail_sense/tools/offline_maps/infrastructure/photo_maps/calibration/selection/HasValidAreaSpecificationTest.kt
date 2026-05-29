package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration.selection

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.geometry.Polygon
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HasValidAreaSpecificationTest {

    @Test
    fun isSatisfiedByUsesPercentOfImageArea() {
        val specification = HasValidAreaSpecification(Range(0.2f, 0.3f))

        assertTrue(specification.isSatisfiedBy(criteria(rectangle(5f, 5f), 10, 10)))
        assertFalse(specification.isSatisfiedBy(criteria(rectangle(4f, 4f), 10, 10)))
        assertFalse(specification.isSatisfiedBy(criteria(rectangle(6f, 6f), 10, 10)))
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
