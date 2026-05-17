package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.mapsforge

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.datastore.Way

internal class WayCentroidCalculatorTest {

    private fun way(vararg rings: Array<LatLong>): Way {
        return Way(0, emptyList(), arrayOf(*rings), null)
    }

    @Test
    fun calculateSquare() {
        val centroid = WayCentroidCalculator.calculate(
            listOf(
                way(
                    arrayOf(
                        LatLong(0.0, 0.0),
                        LatLong(0.0, 4.0),
                        LatLong(4.0, 4.0),
                        LatLong(4.0, 0.0)
                    )
                )
            )
        )

        assertEquals(2.0, centroid?.latitude ?: 0.0, 0.0001)
        assertEquals(2.0, centroid?.longitude ?: 0.0, 0.0001)
    }

    @Test
    fun calculateClosedTriangle() {
        val centroid = WayCentroidCalculator.calculate(
            listOf(
                way(
                    arrayOf(
                        LatLong(0.0, 0.0),
                        LatLong(0.0, 6.0),
                        LatLong(3.0, 0.0),
                        LatLong(0.0, 0.0)
                    )
                )
            )
        )

        assertEquals(1.0, centroid?.latitude ?: 0.0, 0.0001)
        assertEquals(2.0, centroid?.longitude ?: 0.0, 0.0001)
    }

    @Test
    fun calculateMultipleRingsWithHole() {
        val centroid = WayCentroidCalculator.calculate(
            listOf(
                way(
                    arrayOf(
                        LatLong(0.0, 0.0),
                        LatLong(0.0, 4.0),
                        LatLong(4.0, 4.0),
                        LatLong(4.0, 0.0)
                    ),
                    arrayOf(
                        LatLong(0.0, 0.0),
                        LatLong(2.0, 0.0),
                        LatLong(2.0, 2.0),
                        LatLong(0.0, 2.0)
                    )
                )
            )
        )

        assertEquals(7.0 / 3.0, centroid?.latitude ?: 0.0, 0.0001)
        assertEquals(7.0 / 3.0, centroid?.longitude ?: 0.0, 0.0001)
    }

    @Test
    fun calculateIgnoresInvalidRings() {
        val centroid = WayCentroidCalculator.calculate(
            listOf(
                way(
                    arrayOf(LatLong(10.0, 10.0)),
                    arrayOf(LatLong(10.0, 10.0), LatLong(11.0, 11.0)),
                    arrayOf(
                        LatLong(0.0, 0.0),
                        LatLong(0.0, 4.0),
                        LatLong(4.0, 4.0),
                        LatLong(4.0, 0.0)
                    )
                )
            )
        )

        assertEquals(2.0, centroid?.latitude ?: 0.0, 0.0001)
        assertEquals(2.0, centroid?.longitude ?: 0.0, 0.0001)
    }

    @Test
    fun calculateReturnsNullForZeroArea() {
        val centroid = WayCentroidCalculator.calculate(
            listOf(
                way(
                    arrayOf(
                        LatLong(0.0, 0.0),
                        LatLong(1.0, 1.0),
                        LatLong(2.0, 2.0)
                    )
                )
            )
        )

        assertNull(centroid)
    }

    @Test
    fun calculateAcrossMultipleWays() {
        val centroid = WayCentroidCalculator.calculate(
            listOf(
                way(
                    arrayOf(
                        LatLong(0.0, 0.0),
                        LatLong(0.0, 4.0),
                        LatLong(4.0, 4.0),
                        LatLong(4.0, 0.0)
                    )
                ),
                way(
                    arrayOf(
                        LatLong(0.0, 0.0),
                        LatLong(2.0, 0.0),
                        LatLong(2.0, 2.0),
                        LatLong(0.0, 2.0)
                    )
                )
            )
        )

        assertEquals(7.0 / 3.0, centroid?.latitude ?: 0.0, 0.0001)
        assertEquals(7.0 / 3.0, centroid?.longitude ?: 0.0, 0.0001)
    }
}
