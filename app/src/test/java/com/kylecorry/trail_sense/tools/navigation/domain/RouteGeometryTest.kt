package com.kylecorry.trail_sense.tools.navigation.domain

import com.kylecorry.sol.units.Coordinate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RouteGeometryTest {
    private fun meters(east: Double, north: Double) = Coordinate(north / METERS_PER_DEGREE, east / METERS_PER_DEGREE)

    // Parallel legs 10, 9.5 and 8.8 m north of the origin, joined by long connecting segments
    private val geometry = RouteGeometry(
        listOf(
            meters(-50.0, 10.0),
            meters(50.0, 10.0),
            meters(50.0, 9.5),
            meters(-50.0, 9.5),
            meters(-50.0, 8.8),
            meters(50.0, 8.8)
        )
    )

    @Test
    fun `the earliest match within tolerance of the closest is used when not preferring later`() {
        val match = geometry.findClosestInRange(meters(0.0, 0.0), 0f, geometry.length, preferLater = false)

        assertEquals(2, match.segment)
    }

    @Test
    fun `the latest match within tolerance of the closest is used when preferring later`() {
        val match = geometry.findClosestInRange(meters(0.0, 0.0), 0f, geometry.length)

        assertEquals(4, match.segment)
    }

    private companion object {
        const val METERS_PER_DEGREE = 111_195.0
    }
}
