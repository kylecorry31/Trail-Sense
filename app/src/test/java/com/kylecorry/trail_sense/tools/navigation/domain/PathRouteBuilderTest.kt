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
    fun `loop modes visit the whole recorded hike in the chosen direction`() {
        val loop = points + points[1].copy(id = 4) + points[0].copy(id = 5)
        for (mode in listOf(PathNavigationMode.FULL_LOOP, PathNavigationMode.REVERSED_FULL_LOOP)) {
            val route = PathRouteBuilder.build(loop, location, mode)
            assertEquals(route.first(), route.last())
            val ordered = if (mode.isReversed) loop.reversed() else loop
            assertEquals(ordered.drop(1) + ordered.first(), route.drop(1).dropLast(1))
        }
    }

    @Test
    fun `loop detection accepts nearby endpoints but rejects open or tiny paths`() {
        assertFalse(PathRouteBuilder.isLoop(points.map { it.coordinate }))
        assertFalse(PathRouteBuilder.isLoop(emptyList()))
        assertFalse(PathRouteBuilder.isLoop(List(3) { location }))
        assertTrue(PathRouteBuilder.isLoop((points + points[0].copy(coordinate = Coordinate(0.0, 0.00005))).map { it.coordinate }))
    }

    @Test
    fun `saved loop mode can rebuild a path whose endpoints are no longer close`() {
        for (mode in listOf(PathNavigationMode.FULL_LOOP, PathNavigationMode.REVERSED_FULL_LOOP)) {
            val route = PathRouteBuilder.prepare(points, location, mode)
            assertEquals(route.first(), route.last())
            assertTrue(route.map { it.id }.containsAll(listOf(points.first().id, points.last().id)))
        }
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
        val loop = points + points[1].copy(id = 4) + points[0].copy(id = 5)
        val route = PathRouteBuilder.toPoint(loop, location, 4)
        assertEquals(loop.last(), route.last())
        assertTrue(route.drop(1).all { it.coordinate == loop.last().coordinate })
    }

    @Test
    fun `interpolation preserves unknown elevation and handles duplicate points`() {
        assertEquals(50f, PathRouteBuilder.interpolate(points[0], points[1], location).elevation!!, 0.1f)
        assertNull(PathRouteBuilder.interpolate(points[0], points[1].copy(elevation = null), location).elevation)
        assertEquals(0f, PathRouteBuilder.interpolate(points[0], points[0], points[0].coordinate).elevation)
    }

    @Test
    fun `prepare sorts and simplifies before smoothing and constructing either itinerary`() {
        val smoothed = HikingService().correctElevations(listOf(points.first(), points.last()))
        for (mode in listOf(PathNavigationMode.TO_END, PathNavigationMode.REVERSED_TO_END)) {
            assertEquals(PathRouteBuilder.build(smoothed, location, mode),
                PathRouteBuilder.prepare(points.reversed(), location, mode))
        }
        assertEquals(PathRouteBuilder.toPoint(smoothed, location, 1),
            PathRouteBuilder.prepare(points.reversed(), location, PathNavigationMode.TO_END, 3))
    }

    @Test
    fun `prepare retains a selected destination that simplification would remove`() {
        val route = PathRouteBuilder.prepare(points, location, PathNavigationMode.TO_END, 2)
        assertEquals(points[1].id, route.last().id)
        assertEquals(points[1].coordinate, route.last().coordinate)
    }

    @Test
    fun `prepare uses high quality two meter tolerance`() {
        val smallBend = points[1].copy(coordinate = Coordinate(0.00001, 0.001))
        val largerBend = points[1].copy(coordinate = Coordinate(0.000027, 0.001))
        val start = points.first().coordinate
        val simplified = PathRouteBuilder.prepare(
            listOf(points.first(), smallBend, points.last()), start, PathNavigationMode.TO_END
        )
        assertFalse(simplified.any { it.id == smallBend.id })
        val retained = PathRouteBuilder.prepare(
            listOf(points.first(), largerBend, points.last()), start, PathNavigationMode.TO_END
        )
        assertTrue(retained.any { it.id == largerBend.id })
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
}
