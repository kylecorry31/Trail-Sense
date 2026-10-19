package com.kylecorry.trail_sense.tools.navigation.domain.paths

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.paths.domain.PathDirection
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
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
    private val loop = listOf(
        PathPoint(1, 1, Coordinate(0.0, 0.0)),
        PathPoint(2, 1, Coordinate(0.0, 0.01)),
        PathPoint(3, 1, Coordinate(0.01, 0.01)),
        PathPoint(4, 1, Coordinate(0.01, 0.0)),
        PathPoint(5, 1, Coordinate(0.0001, 0.0))
    )

    @Test
    fun `forward and reverse trim the path at the current location`() {
        val forward = buildRoute(points, location, toEnd)
        val reverse = buildRoute(points, location, toStart)
        assertTrue(forward.first().coordinate.distanceTo(location) < 0.1f)
        assertEquals(listOf(2L, 3L), forward.drop(1).map { it.id })
        assertEquals(listOf(1L), reverse.drop(1).map { it.id })
    }

    @Test
    fun `follow and retrace on an out and back keep the recorded order`() {
        val forward = buildRoute(outAndBack, location, toEnd)
        assertEquals(outAndBack.last().id, forward.last().id)
        assertEquals(listOf(2L, 3L, 4L, 5L), forward.drop(1).map { it.id })
        val reverse = buildRoute(outAndBack, location, toStart)
        assertEquals(outAndBack.first().id, reverse.last().id)
        assertEquals(listOf(4L, 3L, 2L, 1L), reverse.drop(1).map { it.id })
    }

    @Test
    fun `follow and retrace on a loop do not cross the seam`() {
        val nearEnd = Coordinate(0.009, 0.0)
        assertEquals(listOf(5L), buildRoute(loop, nearEnd, toEnd).drop(1).map { it.id })
        assertEquals(listOf(4L, 3L, 2L, 1L), buildRoute(loop, nearEnd, toStart).drop(1).map { it.id })
    }

    @Test
    fun `following a loop from near its seam goes all the way around`() {
        val nearEnd = Coordinate(0.00009, 0.0)
        val route = buildRoute(loop, nearEnd, toEnd)
        assertEquals(listOf(2L, 3L, 4L, 5L), route.drop(1).map { it.id })
    }

    @Test
    fun `destination of the first point takes the shortest route to the start of an out and back`() {
        val route = buildRoute(outAndBack, location, PathFollow.ToPoint(1))
        assertEquals(outAndBack.first().coordinate, route.last().coordinate)
        assertTrue(length(route) < 60f)
    }

    @Test
    fun `selected point chooses either direction on an open path`() {
        val toLast = buildRoute(points, location, PathFollow.ToPoint(3))
        assertEquals(3L, toLast.last().id)
        val toFirst = buildRoute(points, location, PathFollow.ToPoint(1))
        assertEquals(listOf(1L), toFirst.drop(1).map { it.id })
    }

    @Test
    fun `selected point on overlapping legs chooses shortest route`() {
        val route = buildRoute(outAndBack, location, PathFollow.ToPoint(5))
        assertEquals(5L, route.last().id)
        assertTrue(route.drop(1).all { it.coordinate == outAndBack.last().coordinate })
    }

    @Test
    fun `route starts at a snap with interpolated elevation`() {
        val route = buildRoute(points, location, toEnd)
        assertEquals((points[0].elevation!! + points[1].elevation!!) / 2, route.first().elevation!!, 0.5f)

        val unknown = points.toMutableList().apply { this[1] = this[1].copy(elevation = null) }
        assertNull(buildRoute(unknown, location, toEnd).first().elevation)
    }

    @Test
    fun `route to a point ends at that point`() {
        val toMiddle = buildRoute(points, location, PathFollow.ToPoint(2))
        assertEquals(points[1], toMiddle.last())
    }

    @Test
    fun `route uses every recorded point`() {
        val start = points.first().coordinate
        val bend = points[1].copy(coordinate = Coordinate(0.00001, 0.001))
        val route = buildRoute(listOf(points.first(), bend, points.last()), start, toEnd)
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
        val route = buildRoute(recording, Coordinate(0.00001, 0.00004), toEnd)
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

        val prepared = buildRoute(
            recording,
            recording.first().coordinate,
            toEnd
        )
        val guidance = PathRoute(prepared).navigate(recording.first().coordinate, null)

        assertTrue(prepared.any { it.id == recording[1].id })
        assertTrue(guidance.remainingDistance.meters().value > 2_000f)
    }

    @Test
    fun `path with no length has no route`() {
        val stacked = List(3) { PathPoint(it + 1L, 1, Coordinate(1.0, 1.0)) }
        for (path in listOf(points.take(1), stacked)) {
            for (follow in listOf(toEnd, toStart, PathFollow.ToPoint(path.last().id))) {
                assertNull(PathRouteBuilder.buildRoute(path, location, follow))
            }
        }
    }

    @Test
    fun `no points has no route`() {
        assertNull(PathRouteBuilder.buildRoute(emptyList(), location, toEnd))
        assertNull(PathRouteBuilder.buildRoute(emptyList(), location, PathFollow.ToPoint(1)))
    }

    @Test
    fun `unknown destination point has no route`() {
        assertNull(PathRouteBuilder.buildRoute(points, location, PathFollow.ToPoint(99)))
    }

    @Test
    fun `path with a zero length segment still routes along it`() {
        val withDuplicate = listOf(points[0], points[1], points[1].copy(id = 4), points[2].copy(id = 5))
        val route = buildRoute(withDuplicate, Coordinate(0.0, 0.001), toEnd)
        assertEquals(listOf(2L, 4L, 5L), route.map { it.id }.takeLast(3))
        assertEquals(1L, route.first().id)
    }

    private fun buildRoute(
        points: List<PathPoint>,
        location: Coordinate,
        follow: PathFollow
    ) = checkNotNull(PathRouteBuilder.buildRoute(points, location, follow))

    private val toEnd = PathFollow.Direction(PathDirection.ToEnd)
    private val toStart = PathFollow.Direction(PathDirection.ToStart)

    private fun length(route: List<PathPoint>) =
        route.zipWithNext().sumOf { (a, b) -> a.coordinate.distanceTo(b.coordinate).toDouble() }.toFloat()
}
