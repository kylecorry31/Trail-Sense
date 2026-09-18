package com.kylecorry.trail_sense.tools.navigation.domain

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PathRouteTest {
    private val points = listOf(
        PathPoint(1, 1, Coordinate(0.0, 0.0), 0f),
        PathPoint(2, 1, Coordinate(0.0, 0.001), 100f),
        PathPoint(3, 1, Coordinate(0.0, 0.002), 50f)
    )

    @Test
    fun `progress is a fraction of cumulative distance with unequal segments`() {
        val path = points.take(2) + points.last().copy(coordinate = Coordinate(0.003, 0.001))
        val route = PathRoute(path)
        var progress = -1f
        route.onProgressChanged = { fraction, _ -> progress = fraction }
        route.navigate(path.first().coordinate)
        assertEquals(0f, progress)
        route.navigate(path[1].coordinate)
        assertEquals(0.25f, progress, 0.002f)
        route.navigate(Coordinate(0.0015, 0.001))
        assertEquals(0.625f, progress, 0.002f)
        route.navigate(path.last().coordinate)
        assertEquals(1f, progress, 0.0001f)
    }

    @Test
    fun `restored fraction locates progress along the route`() {
        val route = PathRoute(points)
        val location = Coordinate(0.0, 0.0015)
        route.restoreProgress(0.75f, location)
        val guidance = route.navigate(location)
        assertEquals(location.distanceTo(points.last().coordinate), guidance.remainingDistance, 0.1f)
    }

    @Test
    fun `remaining estimates use hiking service and include partial segment elevation`() {
        val route = PathRoute(points)
        route.navigate(points.first().coordinate)
        val location = Coordinate(0.0, 0.0005)
        val guidance = route.navigate(location)
        val remaining = listOf(PathRouteBuilder.interpolate(points[0], points[1], location)) + points.drop(1)
        val hiking = HikingService()
        assertEquals(50f, guidance.remainingElevationGain.meters().value, 0.1f)
        assertEquals(
            hiking.getElevationLossGain(remaining).first.meters().value,
            guidance.remainingElevationLoss.meters().value, 0.1f
        )
        assertEquals(location.distanceTo(points.last().coordinate), guidance.remainingDistance, 0.1f)
    }

    @Test
    fun `restored progress produces the same guidance even at the last saved location`() {
        val route = PathRoute(points)
        var savedProgress = 0f
        var savedLocation: Coordinate? = null
        route.onProgressChanged = { progress, location -> savedProgress = progress; savedLocation = location }
        route.navigate(points.first().coordinate)
        val expected = route.navigate(Coordinate(0.0, 0.0005))
        val restored = PathRoute(points)
        restored.restoreProgress(savedProgress, savedLocation)
        assertEquals(expected, restored.navigate(savedLocation!!))
    }

    @Test
    fun `repeated location reuses guidance without saving again`() {
        val route = PathRoute(points)
        var updates = 0
        route.onProgressChanged = { _, _ -> updates++ }
        val first = route.navigate(points.first().coordinate)
        assertSame(first, route.navigate(points.first().coordinate))
        assertEquals(1, updates)
    }

    @Test
    fun `overlapping return leg does not cause early arrival`() {
        val loop = points + points[1] + points[0]
        val route = PathRoute(loop)
        assertFalse(route.navigate(points[0].coordinate).arrived)
        assertFalse(route.navigate(points[1].coordinate).arrived)
    }

    @Test
    fun `arrival clears remaining estimates`() {
        val route = PathRoute(points)
        points.dropLast(1).forEach { route.navigate(it.coordinate) }
        val arrived = route.navigate(points.last().coordinate)
        assertTrue(arrived.arrived)
        assertEquals(0f, arrived.remainingDistance)
        assertEquals(0f, arrived.remainingElevationGain.meters().value)
        assertEquals(0f, arrived.remainingElevationLoss.meters().value)
    }

    @Test
    fun `exactly retraced turnaround prefers forward progress on the return leg`() {
        val route = PathRoute(points + points[1] + points[0])
        var progress = 0f
        route.onProgressChanged = { fraction, _ -> progress = fraction }
        points.forEach { route.navigate(it.coordinate) }
        assertEquals(0.5f, progress, 0.001f)
        val returning = route.navigate(points[1].coordinate)
        assertEquals(0.75f, progress, 0.001f)
        assertEquals(points[0].coordinate, returning.target)
        assertEquals(points[1].coordinate.distanceTo(points[0].coordinate), returning.remainingDistance, 1f)
        assertTrue(route.navigate(points[0].coordinate).arrived)
        assertEquals(1f, progress)
    }

    @Test
    fun `backtracking still decreases progress when there is no matching forward leg`() {
        val route = PathRoute(points)
        var progress = 0f
        route.onProgressChanged = { fraction, _ -> progress = fraction }
        route.navigate(points[0].coordinate)
        route.navigate(points[1].coordinate)
        assertEquals(0.5f, progress, 0.001f)
        route.navigate(Coordinate(0.0, 0.0005))
        assertEquals(0.25f, progress, 0.001f)
    }

    @Test
    fun `off route guidance leads back to the projected point`() {
        val guidance = PathRoute(points).navigate(Coordinate(0.001, 0.0))
        assertTrue(guidance.target.distanceTo(points[0].coordinate) < 0.1f)
        assertEquals(
            guidance.offRoute + points[0].coordinate.distanceTo(points[2].coordinate),
            guidance.remainingDistance,
            0.1f
        )
    }

    @Test
    fun `missing elevations are treated as 0`() {
        val guidance = PathRoute(points.map { it.copy(elevation = null) }).navigate(points[0].coordinate)
        assertEquals(Distance.meters(0f), guidance.remainingElevationGain)
        assertEquals(Distance.meters(0f), guidance.remainingElevationLoss)
    }

    @Test
    fun `single and duplicate points can arrive without a segment`() {
        for (path in listOf(points.take(1), List(3) { points[0] })) {
            val route = PathRoute(path)
            var progress = -1f
            route.onProgressChanged = { fraction, _ -> progress = fraction }
            assertTrue(route.navigate(points[0].coordinate).arrived)
            assertEquals(1f, progress)
        }
        assertThrows(IllegalArgumentException::class.java) { PathRoute(emptyList()) }
    }
}
