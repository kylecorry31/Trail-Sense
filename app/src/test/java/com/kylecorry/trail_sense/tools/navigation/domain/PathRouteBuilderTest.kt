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
        val forward = PathRouteBuilder.buildRoute(points, location, PathNavigationMode.TO_END)
        val reverse = PathRouteBuilder.buildRoute(points, location, PathNavigationMode.REVERSED_TO_END)
        assertTrue(forward.first().coordinate.distanceTo(location) < 0.1f)
        assertEquals(listOf(2L, 3L), forward.drop(1).map { it.id })
        assertEquals(listOf(1L), reverse.drop(1).map { it.id })
    }

    @Test
    fun `full outAndBack takes the long way from the current location to the end`() {
        val forward = PathRouteBuilder.buildRoute(outAndBack, location, PathNavigationMode.FULL_LOOP)
        assertEquals(outAndBack.last().id, forward.last().id)
        assertEquals(listOf(2L, 3L, 4L, 5L), forward.drop(1).map { it.id })
        val reverse = PathRouteBuilder.buildRoute(outAndBack, location, PathNavigationMode.REVERSED_FULL_LOOP)
        assertEquals(outAndBack.first().id, reverse.last().id)
        assertEquals(listOf(4L, 3L, 2L, 1L), reverse.drop(1).map { it.id })
    }

    @Test
    fun `following to the end or start takes the shortest route when the location is on a shared segment`() {
        val toEnd = PathRouteBuilder.buildRoute(outAndBack, location, PathNavigationMode.TO_END)
        assertEquals(outAndBack.last().id, toEnd.last().id)
        assertTrue(length(toEnd) < 60f)
        val toStart = PathRouteBuilder.buildRoute(outAndBack, location, PathNavigationMode.REVERSED_TO_END)
        assertEquals(outAndBack.first().id, toStart.last().id)
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
        assertEquals(listOf(5L), PathRouteBuilder.buildRoute(loop, nearEnd, PathNavigationMode.TO_END).drop(1).map { it.id })
        assertEquals(listOf(5L, 1L), PathRouteBuilder.buildRoute(loop, nearEnd, PathNavigationMode.REVERSED_TO_END).drop(1).map { it.id })
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
        val toLast = PathRouteBuilder.buildRoute(points, location, PathNavigationMode.TO_END, 3)
        assertEquals(3L, toLast.last().id)
        val toFirst = PathRouteBuilder.buildRoute(points, location, PathNavigationMode.TO_END, 1)
        assertEquals(listOf(1L), toFirst.drop(1).map { it.id })
    }

    @Test
    fun `selected point takes the shorter route across a loop seam`() {
        val loop = points + PathPoint(4, 1, Coordinate(0.001, 0.002)) +
            PathPoint(5, 1, Coordinate(0.001, 0.0)) + points[0].copy(id = 6)
        val route = PathRouteBuilder.buildRoute(loop, location, PathNavigationMode.TO_END, 5)
        assertEquals(listOf(1L, 6L, 5L), route.drop(1).map { it.id })
    }

    @Test
    fun `selected point on overlapping legs chooses shortest itinerary`() {
        val route = PathRouteBuilder.buildRoute(outAndBack, location, PathNavigationMode.TO_END, 5)
        assertEquals(5L, route.last().id)
        assertTrue(route.drop(1).all { it.coordinate == outAndBack.last().coordinate })
    }

    @Test
    fun `route starts at a snap with interpolated elevation`() {
        val corrected = HikingService().correctElevations(points)
        val route = PathRouteBuilder.buildRoute(points, location, PathNavigationMode.TO_END)
        assertEquals((corrected[0].elevation!! + corrected[1].elevation!!) / 2, route.first().elevation!!, 0.5f)

        val unknown = points.toMutableList().apply { this[1] = this[1].copy(elevation = null) }
        assertNull(PathRouteBuilder.buildRoute(unknown, location, PathNavigationMode.TO_END).first().elevation)
    }

    @Test
    fun `route sorts and smooths elevations before constructing either itinerary`() {
        for (mode in PathNavigationMode.entries) {
            assertEquals(
                PathRouteBuilder.buildRoute(points, location, mode),
                PathRouteBuilder.buildRoute(points.reversed(), location, mode)
            )
        }
        val corrected = HikingService().correctElevations(points)
        val toMiddle = PathRouteBuilder.buildRoute(points.reversed(), location, PathNavigationMode.TO_END, 2)
        assertEquals(corrected[1], toMiddle.last())
    }

    @Test
    fun `route uses every recorded point`() {
        val start = points.first().coordinate
        val bend = points[1].copy(coordinate = Coordinate(0.00001, 0.001))
        val route = PathRouteBuilder.buildRoute(listOf(points.first(), bend, points.last()), start, PathNavigationMode.TO_END)
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
        val route = PathRouteBuilder.buildRoute(recording, Coordinate(0.00001, 0.00004), PathNavigationMode.FULL_LOOP)
        assertEquals(listOf(2L, 3L), route.drop(1).map { it.id })
        assertTrue(route.first().coordinate.distanceTo(trailhead) < 10f)
    }

    @Test
    fun `route preserves an out and back turnaround and its route distance`() {
        val recording = listOf(
            PathPoint(1, 1, Coordinate(42.0, -72.0)),
            PathPoint(2, 1, Coordinate(42.01, -72.0)),
            PathPoint(3, 1, Coordinate(42.001, -72.0))
        )

        val prepared = PathRouteBuilder.buildRoute(
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
        for (mode in PathNavigationMode.entries) {
            assertEquals(listOf(1L), PathRouteBuilder.buildRoute(points.take(1), location, mode).map { it.id })
        }
        assertThrows(IllegalArgumentException::class.java) {
            PathRouteBuilder.buildRoute(emptyList(), location, PathNavigationMode.TO_END)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PathRouteBuilder.buildRoute(points, location, PathNavigationMode.TO_END, 99)
        }
    }

    @Test
    fun `path with no length routes to its first point`() {
        val stacked = List(3) { PathPoint(it + 1L, 1, Coordinate(1.0, 1.0)) }
        for (mode in PathNavigationMode.entries) {
            assertEquals(listOf(1L), PathRouteBuilder.buildRoute(stacked, location, mode).map { it.id })
            assertEquals(listOf(1L), PathRouteBuilder.buildRoute(stacked, location, mode, 3).map { it.id })
        }
    }

    @Test
    fun `path with a zero length segment still routes along it`() {
        val withDuplicate = listOf(points[0], points[1], points[1].copy(id = 4), points[2].copy(id = 5))
        val route = PathRouteBuilder.buildRoute(withDuplicate, Coordinate(0.0, 0.001), PathNavigationMode.FULL_LOOP)
        assertEquals(listOf(2L, 4L, 5L), route.map { it.id }.takeLast(3))
        assertEquals(1L, route.first().id)
    }

    private fun length(route: List<PathPoint>) =
        route.zipWithNext().sumOf { (a, b) -> a.coordinate.distanceTo(b.coordinate).toDouble() }.toFloat()
}
