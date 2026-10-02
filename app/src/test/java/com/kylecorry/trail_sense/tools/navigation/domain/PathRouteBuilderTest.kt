package com.kylecorry.trail_sense.tools.navigation.domain

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PathRouteBuilderTest {
    private val points = listOf(
        PathPoint(1, 1, Coordinate(0.0, 0.0), 0f),
        PathPoint(2, 1, Coordinate(0.0, 0.001), 100f),
        PathPoint(3, 1, Coordinate(0.0, 0.002), 50f)
    )
    private val outAndBack = points + points[1].copy(id = 4) + points[0].copy(id = 5)
    private val location = Coordinate(0.0, 0.0005)

    @Test
    fun `forward and reverse trim the path at the current location`() {
        val forward = PathRouteBuilder.build(points, location, PathNavigationMode.TO_END)
        val reverse = PathRouteBuilder.build(points, location, PathNavigationMode.REVERSED_TO_END)
        assertTrue(forward.first().coordinate.distanceTo(location) < 0.1f)
        assertEquals(points.drop(1), forward.drop(1))
        assertEquals(listOf(points.first()), reverse.drop(1))
    }

    @Test
    fun `full outAndBack takes the long way from the current location to the end`() {
        val forward = PathRouteBuilder.build(outAndBack, location, PathNavigationMode.FULL_LOOP)
        assertEquals(outAndBack.last(), forward.last())
        assertEquals(listOf(2L, 3L, 4L, 5L), forward.drop(1).map { it.id })
        val reverse = PathRouteBuilder.build(outAndBack, location, PathNavigationMode.REVERSED_FULL_LOOP)
        assertEquals(outAndBack.first(), reverse.last())
        assertEquals(listOf(4L, 3L, 2L, 1L), reverse.drop(1).map { it.id })
    }

    @Test
    fun `following to the end or start takes the shortest route when the location is on a shared segment`() {
        val toEnd = PathRouteBuilder.build(outAndBack, location, PathNavigationMode.TO_END)
        assertEquals(outAndBack.last(), toEnd.last())
        assertTrue(length(toEnd) < 60f)
        val toStart = PathRouteBuilder.build(outAndBack, location, PathNavigationMode.REVERSED_TO_END)
        assertEquals(outAndBack.first(), toStart.last())
        assertTrue(length(toStart) < 60f)
    }

    @Test
    fun `following to the end or start on a loop can cross the seam if it is shorter`() {
        val loop = listOf(
            PathPoint(1, 1, Coordinate(0.0, 0.0)),
            PathPoint(2, 1, Coordinate(0.0, 0.01)),
            PathPoint(3, 1, Coordinate(0.01, 0.01)),
            PathPoint(4, 1, Coordinate(0.01, 0.0)),
            PathPoint(5, 1, Coordinate(0.0001, 0.0))
        )
        val nearEnd = Coordinate(0.009, 0.0)
        assertEquals(listOf(5L), PathRouteBuilder.build(loop, nearEnd, PathNavigationMode.TO_END).drop(1).map { it.id })
        assertEquals(listOf(5L, 1L), PathRouteBuilder.build(loop, nearEnd, PathNavigationMode.REVERSED_TO_END).drop(1).map { it.id })
    }

    @Test
    fun `loop detection accepts nearby endpoints but rejects open or tiny paths`() {
        assertFalse(PathRouteBuilder.isLoop(points.map { it.coordinate }))
        assertFalse(PathRouteBuilder.isLoop(emptyList()))
        assertFalse(PathRouteBuilder.isLoop(List(3) { location }))
        assertTrue(PathRouteBuilder.isLoop((points + points[0].copy(coordinate = Coordinate(0.0, 0.00005))).map { it.coordinate }))
    }

    @Test
    fun `selected point chooses either direction on an open path`() {
        assertEquals(points.last(), PathRouteBuilder.toPoint(points, location, 2).last())
        assertEquals(listOf(points.first()), PathRouteBuilder.toPoint(points, location, 0).drop(1))
    }

    @Test
    fun `selected point takes the shorter route across a loop seam`() {
        val loop = points + PathPoint(4, 1, Coordinate(0.001, 0.002)) +
            PathPoint(5, 1, Coordinate(0.001, 0.0)) + points[0].copy(id = 6)
        val route = PathRouteBuilder.toPoint(loop, location, 4)
        assertEquals(listOf(1L, 6L, 5L), route.drop(1).map { it.id })
    }

    @Test
    fun `selected point on overlapping legs chooses shortest itinerary`() {
        val route = PathRouteBuilder.toPoint(outAndBack, location, 4)
        assertEquals(outAndBack.last(), route.last())
        assertTrue(route.drop(1).all { it.coordinate == outAndBack.last().coordinate })
    }

    @Test
    fun `interpolation preserves unknown elevation and handles duplicate points`() {
        assertEquals(50f, PathRouteBuilder.interpolate(points[0], points[1], location).elevation!!, 0.1f)
        assertNull(PathRouteBuilder.interpolate(points[0], points[1].copy(elevation = null), location).elevation)
        assertEquals(0f, PathRouteBuilder.interpolate(points[0], points[0], points[0].coordinate).elevation)
    }

    @Test
    fun `prepare sorts and smooths elevations before constructing either itinerary`() {
        val smoothed = HikingService().correctElevations(points)
        for (mode in listOf(PathNavigationMode.TO_END, PathNavigationMode.REVERSED_TO_END)) {
            assertEquals(PathRouteBuilder.build(smoothed, location, mode),
                PathRouteBuilder.prepare(points.reversed(), location, mode))
        }
        assertEquals(PathRouteBuilder.toPoint(smoothed, location, 1),
            PathRouteBuilder.prepare(points.reversed(), location, PathNavigationMode.TO_END, 2))
    }

    @Test
    fun `prepare uses every recorded point`() {
        val start = points.first().coordinate
        val bend = points[1].copy(coordinate = Coordinate(0.00001, 0.001))
        val route = PathRouteBuilder.prepare(listOf(points.first(), bend, points.last()), start, PathNavigationMode.TO_END)
        assertEquals(listOf(1L, 2L, 3L), route.map { it.id })
    }

    @Test
    fun `starting near both ends of an out and back begins at the start`() {
        val trailhead = Coordinate(0.0, 0.0)
        val recording = listOf(
            PathPoint(1, 1, trailhead),
            PathPoint(2, 1, Coordinate(0.0, 0.01)),
            PathPoint(3, 1, Coordinate(0.00001, 0.00003))
        )
        // Slightly closer to the end of the recording than the start
        val route = PathRouteBuilder.prepare(recording, Coordinate(0.00001, 0.00004), PathNavigationMode.FULL_LOOP)
        assertEquals(listOf(2L, 3L), route.drop(1).map { it.id })
        assertTrue(route.first().coordinate.distanceTo(trailhead) < 10f)
    }

    @Test
    fun `prepare preserves an out and back turnaround and its route distance`() {
        val recording = listOf(
            PathPoint(1, 1, Coordinate(42.0, -72.0)),
            PathPoint(2, 1, Coordinate(42.01, -72.0)),
            PathPoint(3, 1, Coordinate(42.001, -72.0))
        )

        val prepared = PathRouteBuilder.prepare(
            recording,
            recording.first().coordinate,
            PathNavigationMode.TO_END
        )
        val guidance = PathRoute(prepared).navigate(recording.first().coordinate)

        assertTrue(prepared.any { it.id == recording[1].id })
        assertTrue(guidance.remainingDistance > 2_000f)
    }

    @Test
    fun `single point and invalid inputs`() {
        assertEquals(points.take(1), PathRouteBuilder.build(points.take(1), location, PathNavigationMode.TO_END))
        assertThrows(IllegalArgumentException::class.java) {
            PathRouteBuilder.build(emptyList(), location, PathNavigationMode.TO_END)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PathRouteBuilder.prepare(points, location, PathNavigationMode.TO_END, 99)
        }
    }

    private fun length(route: List<PathPoint>) =
        route.zipWithNext().sumOf { (a, b) -> a.coordinate.distanceTo(b.coordinate).toDouble() }.toFloat()
}
