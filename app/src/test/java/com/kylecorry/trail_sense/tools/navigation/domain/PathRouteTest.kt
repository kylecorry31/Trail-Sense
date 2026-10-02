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

    private fun route(coordinates: List<Coordinate>) = PathRoute(
        coordinates.mapIndexed { index, coordinate -> PathPoint(index.toLong(), 1, coordinate, 0f) }
    )

    @Test
    fun `remaining route interpolates current and off route target elevations`() {
        val route = PathRoute(points)
        val location = Coordinate(0.001, 0.0005)
        route.restoreProgress(0.25f, location)
        val guidance = route.navigate(location)

        assertEquals(50f, guidance.remainingRoute[0].elevation!!, 0.1f)
        assertEquals(50f, guidance.remainingRoute[1].elevation!!, 0.1f)
        assertEquals(listOf(100f, 50f), guidance.remainingRoute.map { it.elevation }.drop(2))
        assertEquals(points.drop(1), guidance.remainingRoute.drop(2))
    }

    @Test
    fun `remaining route preserves unknown elevations`() {
        val route = PathRoute(points.map { it.copy(elevation = null) })
        assertEquals(listOf(null, null, null), route.navigate(points.first().coordinate).remainingRoute.map { it.elevation })
    }

    @Test
    fun `reversed route preserves elevations at repeated coordinates`() {
        val path = (points + points.first().copy(id = 4, elevation = 75f)).reversed()
        val guidance = PathRoute(path).navigate(path.first().coordinate)
        assertEquals(listOf(75f, 50f, 100f, 0f), guidance.remainingRoute.map { it.elevation })
    }

    @Test
    fun `single point route preserves elevation`() {
        assertEquals(listOf(100f, 100f), PathRoute(listOf(points[1]))
            .navigate(points[1].coordinate).remainingRoute.map { it.elevation })
    }

    @Test
    fun `missed corner advances on sparse and densely sampled paths`() {
        for (samplesPerSegment in listOf(1, 20)) {
            val coordinates = (0 until samplesPerSegment).map {
                Coordinate(0.0, 0.004 * it / samplesPerSegment)
            } + (0..samplesPerSegment).map {
                Coordinate(0.004 * it / samplesPerSegment, 0.004)
            }
            val path = coordinates.mapIndexed { index, coordinate ->
                PathPoint(index.toLong(), 1, coordinate, 0f)
            }
            val route = PathRoute(path)
            route.navigate(coordinates.first())
            val location = Coordinate(0.003, 0.004)
            val guidance = route.navigate(location)
            assertEquals(0f, guidance.offRoute, 5f)
            assertTrue(guidance.target.latitude > location.latitude)
            assertEquals(location.distanceTo(coordinates.last()), guidance.remainingDistance, 5f)
        }
    }

    @Test
    fun `shortcut past a corner advances from restored progress`() {
        val path = listOf(Coordinate(0.0, 0.0), Coordinate(0.0, 0.004), Coordinate(0.004, 0.004))
            .mapIndexed { index, coordinate -> PathPoint(index.toLong(), 1, coordinate, 0f) }
        val route = PathRoute(path)
        route.restoreProgress(0.125f, Coordinate(0.0, 0.001))
        val location = Coordinate(0.002, 0.0039)
        val guidance = route.navigate(location)
        assertEquals(0.004, guidance.target.longitude, 0.00001)
        assertEquals(0.002 + 25 / 111_000.0, guidance.target.latitude, 0.00005)
        assertTrue(guidance.offRoute < 15f)
        assertEquals(
            guidance.offRoute + Coordinate(0.002, 0.004).distanceTo(path.last().coordinate),
            guidance.remainingDistance,
            1f
        )
    }

    @Test
    fun `nearby return leg does not skip a long outbound segment`() {
        val path = listOf(
            Coordinate(0.0, 0.0), Coordinate(0.0, 0.01),
            Coordinate(0.0001, 0.01), Coordinate(0.0001, 0.0)
        ).mapIndexed { index, coordinate -> PathPoint(index.toLong(), 1, coordinate, 0f) }
        val route = PathRoute(path)
        route.navigate(path.first().coordinate)
        val guidance = route.navigate(Coordinate(0.0001, 0.001))
        assertEquals(0.0, guidance.target.latitude, 0.00002)
        assertTrue(guidance.target.longitude > 0.001)
        assertFalse(guidance.arrived)
    }

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
    fun `progress saved as a distance locates the position on a longer route`() {
        val route = PathRoute(points)
        var savedProgress = 0f
        route.onProgressChanged = { progress, _ -> savedProgress = progress }
        val location = Coordinate(0.0, 0.0015)
        route.navigate(points.first().coordinate)
        route.navigate(location)
        val savedDistance = savedProgress * route.length

        val extended = PathRoute(points + PathPoint(4, 1, Coordinate(0.0, 0.004), 0f))
        extended.restoreProgress(savedDistance / extended.length, location)
        val guidance = extended.navigate(location)
        assertEquals(location.distanceTo(Coordinate(0.0, 0.004)), guidance.remainingDistance, 0.5f)
        assertEquals(points[2].coordinate, guidance.remainingRoute[1].coordinate)
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
        restored.restoreProgress(savedProgress, savedLocation!!)
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
        assertEquals(0.0, returning.target.latitude, 0.00001)
        assertTrue(returning.target.longitude < points[1].coordinate.longitude)
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

    @Test
    fun `first location far along the route rejoins there`() {
        val route = route(listOf(Coordinate(0.0, 0.0), Coordinate(0.0, 0.01), Coordinate(0.01, 0.01)))
        var progress = 0f
        route.onProgressChanged = { fraction, _ -> progress = fraction }
        route.navigate(Coordinate(0.005, 0.01))
        route.navigate(Coordinate(0.00501, 0.01))
        assertEquals(0.75f, progress, 0.005f)
    }

    @Test
    fun `first location with GPS error toward the end of a loop does not arrive`() {
        // The recorded end is 12 m from the start and the first fix is 10 m from the start, toward the end
        val route = route(listOf(
            Coordinate(0.0, 0.0), Coordinate(0.0, 0.003), Coordinate(0.003, 0.003),
            Coordinate(0.003, 0.0), Coordinate(0.000108, 0.0)
        ))
        val guidance = route.navigate(Coordinate(0.00009, 0.0))
        assertFalse(guidance.arrived)
        assertTrue(guidance.remainingDistance > 1000f)
    }

    @Test
    fun `first location near both the start and end matches the start`() {
        val start = Coordinate(0.0, 0.0)
        val route = route(listOf(start, Coordinate(0.0, 0.01), Coordinate(0.00001, 0.00001)))
        val guidance = route.navigate(Coordinate(0.00001, 0.00002))
        assertFalse(guidance.arrived)
        assertTrue(guidance.remainingDistance > 1000f)
    }

    @Test
    fun `rejoining the route after a shortcut follows the new position`() {
        val route = route(listOf(
            Coordinate(0.0, 0.0), Coordinate(0.0, 0.01),
            Coordinate(0.001, 0.01), Coordinate(0.001, 0.0), Coordinate(0.002, 0.0)
        ))
        var progress = 0f
        route.onProgressChanged = { fraction, _ -> progress = fraction }
        route.navigate(Coordinate(0.0, 0.0))
        route.navigate(Coordinate(0.0, 0.0002))
        // Cuts across to the return leg 55 m away
        val shortcut = Coordinate(0.0006, 0.0003)
        route.navigate(shortcut)
        assertEquals(0.02f, progress, 0.01f)
        route.navigate(shortcut)
        assertEquals(0.02f, progress, 0.01f)
        val rejoined = Coordinate(0.001, 0.0003)
        route.navigate(Coordinate(0.0009, 0.0003))
        route.navigate(rejoined)
        assertTrue(progress > 0.5f)
    }

    @Test
    fun `a single outlier does not move the progress to another part of the route`() {
        val route = route(listOf(
            Coordinate(0.0, 0.0), Coordinate(0.0, 0.01),
            Coordinate(0.0003, 0.01), Coordinate(0.0003, 0.0)
        ))
        var progress = 0f
        route.onProgressChanged = { fraction, _ -> progress = fraction }
        route.navigate(Coordinate(0.0, 0.0))
        route.navigate(Coordinate(0.0, 0.0005))
        route.navigate(Coordinate(0.0003, 0.0006))
        assertEquals(0.03f, progress, 0.005f)
        route.navigate(Coordinate(0.0, 0.0007))
        assertEquals(0.035f, progress, 0.005f)
    }

    @Test
    fun `steers to a corner instead of cutting it`() {
        val corner = Coordinate(0.0, 0.004)
        val route = route(listOf(Coordinate(0.0, 0.0), corner, Coordinate(0.004, 0.004)))
        route.navigate(Coordinate(0.0, 0.0))
        val approaching = route.navigate(Coordinate(0.0, 0.0038))
        assertEquals(corner.longitude, approaching.target.longitude, 0.00001)
        assertEquals(corner.latitude, approaching.target.latitude, 0.00001)
    }

    @Test
    fun `steers past a corner once it is reached`() {
        val corner = Coordinate(0.0, 0.004)
        val route = route(listOf(Coordinate(0.0, 0.0), corner, Coordinate(0.004, 0.004)))
        route.navigate(Coordinate(0.0, 0.0))
        route.navigate(Coordinate(0.0, 0.0038))
        val atCorner = route.navigate(Coordinate(0.0, 0.003975))
        assertTrue(atCorner.target.latitude > 0.0001)
        assertEquals(0.004, atCorner.target.longitude, 0.00001)
        // GPS jitter around the corner does not bring the corner back
        val jitter = route.navigate(Coordinate(0.0, 0.00389))
        assertTrue(jitter.target.latitude > 0.0)
    }

    @Test
    fun `the corner is targeted again after backtracking away from it`() {
        val corner = Coordinate(0.0, 0.004)
        val route = route(listOf(Coordinate(0.0, 0.0), corner, Coordinate(0.004, 0.004)))
        route.navigate(Coordinate(0.0, 0.0))
        route.navigate(Coordinate(0.0, 0.003975))
        route.navigate(Coordinate(0.0, 0.0036))
        val back = route.navigate(Coordinate(0.0, 0.0038))
        assertEquals(corner.latitude, back.target.latitude, 0.00001)
        assertEquals(corner.longitude, back.target.longitude, 0.00001)
    }

    @Test
    fun `gentle bends on a dense path are not treated as corners`() {
        val coordinates = (0..100).map { Coordinate(0.00002 * it * it / 100.0, 0.0001 * it) }
        val route = route(coordinates)
        route.navigate(coordinates.first())
        val guidance = route.navigate(coordinates[10])
        val distance = coordinates[10].distanceTo(guidance.target)
        assertEquals(25f, distance, 3f)
    }

    @Test
    fun `steers toward the route ahead rather than a distant point`() {
        val route = route(listOf(Coordinate(0.0, 0.0), Coordinate(0.0, 0.01)))
        route.navigate(Coordinate(0.0, 0.0))
        val guidance = route.navigate(Coordinate(0.0001, 0.001))
        val bearing = Coordinate(0.0001, 0.001).bearingTo(guidance.target).value
        assertTrue(bearing in 100f..150f)
    }

    @Test
    fun `dense paths are matched without simplification`() {
        val coordinates = (0..2000).map { Coordinate(0.0, 0.000005 * it) }
        val route = route(coordinates)
        var progress = 0f
        route.onProgressChanged = { fraction, _ -> progress = fraction }
        route.navigate(coordinates.first())
        for (i in 1..50) {
            route.navigate(Coordinate(0.00001, 0.0001 * i))
        }
        assertEquals(0.5f, progress, 0.01f)
    }

    @Test
    fun `walking a noisy recording around corners tracks progress and arrives`() {
        val random = java.util.Random(7)
        val corners = listOf(
            Coordinate(0.0, 0.0), Coordinate(0.0, 0.003), Coordinate(0.002, 0.003),
            Coordinate(0.002, 0.0005), Coordinate(0.0035, 0.0005)
        )
        val recorded = corners.zipWithNext().flatMap { (a, b) ->
            (0 until 50).map { Coordinate(a.latitude + (b.latitude - a.latitude) * it / 50, a.longitude + (b.longitude - a.longitude) * it / 50) }
        } + corners.last()
        val noisyRecording = recorded.map {
            Coordinate(it.latitude + random.nextGaussian() * 0.00002, it.longitude + random.nextGaussian() * 0.00002)
        }
        val route = route(noisyRecording)
        var progress = 0f
        route.onProgressChanged = { fraction, _ -> progress = fraction }
        var previousProgress = 0f
        var arrived = false
        for (recordedPoint in recorded) {
            val location = Coordinate(
                recordedPoint.latitude + random.nextGaussian() * 0.00003,
                recordedPoint.longitude + random.nextGaussian() * 0.00003
            )
            val guidance = route.navigate(location)
            assertTrue(progress * route.length >= previousProgress * route.length - 25f)
            assertTrue(guidance.offRoute < 25f)
            previousProgress = progress
            arrived = guidance.arrived
        }
        assertTrue(arrived)
    }

    @Test
    fun `rejoining near the shared start and end of a loop does not arrive`() {
        val route = route(listOf(
            Coordinate(0.0, 0.0), Coordinate(0.0, 0.003), Coordinate(0.003, 0.003),
            Coordinate(0.003, 0.0), Coordinate(0.00001, 0.0)
        ))
        // Partway up the second side, out of reach of both the start and end of the loop
        route.restoreProgress(400f / route.length, Coordinate(0.0006, 0.003))
        val nearSeam = Coordinate(0.00002, 0.00002)
        route.navigate(nearSeam)
        val guidance = route.navigate(Coordinate(0.000021, 0.00002))
        assertFalse(guidance.arrived)
        assertTrue(guidance.remainingDistance > 1000f)
    }
}
