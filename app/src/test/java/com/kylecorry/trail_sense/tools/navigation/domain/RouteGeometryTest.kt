package com.kylecorry.trail_sense.tools.navigation.domain

import com.kylecorry.sol.units.Coordinate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RouteGeometryTest {
    private fun meters(east: Double, north: Double) = Coordinate(north / METERS_PER_DEGREE, east / METERS_PER_DEGREE)

    // Parallel legs 10 and 9.5 m north of the origin, joined by a long connecting segment
    private val geometry = RouteGeometry(
        listOf(
            meters(-50.0, 10.0),
            meters(50.0, 10.0),
            meters(50.0, 9.5),
            meters(-50.0, 9.5)
        )
    )

    @Test
    fun `a similarly close match nearest the estimated distance is used`() {
        val origin = meters(0.0, 0.0)

        val nearStart = geometry.getOptimalLocationMatch(origin, 0f, geometry.length, 0f)
        val nearEnd = geometry.getOptimalLocationMatch(origin, 0f, geometry.length, geometry.length)

        assertEquals(0, nearStart.segment)
        assertEquals(2, nearEnd.segment)
    }

    @Test
    fun `a closer match is used even when it is far from the estimated distance`() {
        val closerFirst = RouteGeometry(listOf(meters(-50.0, 10.0), meters(50.0, 10.0), meters(50.0, 40.0)))

        val match = closerFirst.getOptimalLocationMatch(meters(0.0, 0.0), 0f, closerFirst.length, closerFirst.length)

        assertEquals(0, match.segment)
    }

    @Test
    fun `the match nearest the estimated distance is used when equally close`() {
        val outAndBack = RouteGeometry(listOf(meters(-50.0, 10.0), meters(50.0, 10.0), meters(-50.0, 10.0)))
        val origin = meters(0.0, 0.0)

        val nearStart = outAndBack.getOptimalLocationMatch(origin, 0f, outAndBack.length, 0f)
        val nearEnd = outAndBack.getOptimalLocationMatch(origin, 0f, outAndBack.length, outAndBack.length)

        assertEquals(0, nearStart.segment)
        assertEquals(1, nearEnd.segment)
    }

    @Test
    fun `the earliest match is used when equally close and equally near the estimated distance`() {
        val outAndBack = RouteGeometry(listOf(meters(-50.0, 10.0), meters(50.0, 10.0), meters(-50.0, 10.0)))

        val match = outAndBack.getOptimalLocationMatch(meters(0.0, 0.0), 0f, outAndBack.length, outAndBack.length / 2)

        assertEquals(0, match.segment)
    }

    @Test
    fun `a much closer match is used however far it is from the estimated distance`() {
        val farButClose = RouteGeometry(
            listOf(
                meters(-50.0, 40.0),
                meters(50.0, 40.0),
                meters(5000.0, 40.0),
                meters(5000.0, -5.0),
                meters(-50.0, -5.0)
            )
        )

        val match = farButClose.getOptimalLocationMatch(meters(0.0, 0.0), 0f, farButClose.length, 0f)

        assertEquals(3, match.segment)
    }

    private companion object {
        const val METERS_PER_DEGREE = 111_195.0
    }
}
