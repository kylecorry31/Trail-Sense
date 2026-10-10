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
    fun `the match within tolerance of the closest that is nearest the preferred distance is used`() {
        val origin = meters(0.0, 0.0)

        val nearStart = geometry.findClosestInRange(origin, 0f, geometry.length, preferNear = 0f)
        val nearEnd = geometry.findClosestInRange(origin, 0f, geometry.length, preferNear = geometry.length)

        assertEquals(2, nearStart.segment)
        assertEquals(4, nearEnd.segment)
    }

    @Test
    fun `a closer match is used even when it is far from the preferred distance`() {
        val closerFirst = RouteGeometry(listOf(meters(-50.0, 10.0), meters(50.0, 10.0), meters(50.0, 40.0)))

        val match = closerFirst.findClosestInRange(meters(0.0, 0.0), 0f, closerFirst.length, preferNear = closerFirst.length)

        assertEquals(0, match.segment)
    }

    @Test
    fun `the latest match within tolerance of the closest is used when there is no preferred distance`() {
        val match = geometry.findClosestInRange(meters(0.0, 0.0), 0f, geometry.length)

        assertEquals(4, match.segment)
    }

    private companion object {
        const val METERS_PER_DEGREE = 111_195.0
    }
}
