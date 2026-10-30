package com.kylecorry.trail_sense.tools.navigation.domain.paths

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.IdentityHashMap

class PathRouteTest {
    private val points = listOf(
        PathPoint(1, 1, Coordinate(0.0, 0.0), 0f),
        PathPoint(2, 1, Coordinate(0.0, 0.001), 100f),
        PathPoint(3, 1, Coordinate(0.0, 0.002), 50f)
    )

    // The route does not expose its length, so it is tracked here to express positions as a fraction of it
    private val lengths = IdentityHashMap<PathRoute, Float>()

    private val PathRoute.length: Float
        get() = lengths.getValue(this)

    private fun createRoute(points: List<PathPoint>, clock: Clock = Clock.systemUTC()): PathRoute {
        return PathRoute(points, clock).also {
            lengths[it] = RouteGeometry(points.map { point -> point.coordinate }).length
        }
    }

    private fun route(coordinates: List<Coordinate>) = createRoute(
        coordinates.mapIndexed { index, coordinate -> PathPoint(index.toLong(), 1, coordinate, 0f) }
    )

    private fun PathRoute.navigate(location: Coordinate) = navigate(location, null)

    private fun PathRoute.navigateFraction(location: Coordinate) = navigate(location).distanceAlong.meters().value / length

    @Test
    fun `location accuracy widens the part of the route that is searched`() {
        val location = Coordinate(0.0, 0.00036)

        val withoutAccuracy = createRoute(points).navigate(location, null)
        val withAccuracy = createRoute(points).navigate(location, Distance.meters(50f))

        assertEquals(15f, withoutAccuracy.distanceAlong.meters().value, 0.5f)
        assertEquals(location.distanceTo(points.first().coordinate), withAccuracy.distanceAlong.meters().value, 0.5f)
        assertEquals(0f, withAccuracy.offRouteDistance.meters().value, 1f)
    }

    @Test
    fun `accuracy smaller than the minimum tolerance does not narrow the search`() {
        val location = Coordinate(0.0, 0.00036)

        val withSmallAccuracy = createRoute(points).navigate(location, Distance.meters(3f))

        assertEquals(15f, withSmallAccuracy.distanceAlong.meters().value, 0.5f)
    }

    @Test
    fun `accuracy larger than the maximum tolerance does not widen the search any further`() {
        val location = Coordinate(0.0, 0.0009)

        val withMaxAccuracy = createRoute(points).navigate(location, Distance.meters(75f))
        val withPoorAccuracy = createRoute(points).navigate(location, Distance.meters(500f))

        assertEquals(75f, withPoorAccuracy.distanceAlong.meters().value, 0.5f)
        assertEquals(withMaxAccuracy.distanceAlong.meters().value, withPoorAccuracy.distanceAlong.meters().value, 0.05f)
    }

    @Test
    fun `remaining route interpolates current and off route target elevations`() {
        val route = createRoute(points)
        val location = Coordinate(0.001, 0.0005)
        route.restorePosition(0.25f * route.length, location)
        val guidance = route.navigate(location)

        assertEquals(50f, guidance.remainingRoute[0].elevation!!, 0.1f)
        assertEquals(50f, guidance.remainingRoute[1].elevation!!, 0.1f)
        assertEquals(listOf(100f, 50f), guidance.remainingRoute.map { it.elevation }.drop(2))
        assertEquals(points.drop(1), guidance.remainingRoute.drop(2))
    }

    @Test
    fun `remaining route preserves unknown elevations`() {
        val route = createRoute(points.map { it.copy(elevation = null) })
        assertEquals(listOf(null, null, null), route.navigate(points.first().coordinate).remainingRoute.map { it.elevation })
    }

    @Test
    fun `reversed route preserves elevations at repeated coordinates`() {
        val path = (points + points.first().copy(id = 4, elevation = 75f)).reversed()
        val guidance = createRoute(path).navigate(path.first().coordinate)
        assertEquals(listOf(75f, 50f, 100f, 0f), guidance.remainingRoute.map { it.elevation })
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
            val route = createRoute(path)
            route.navigate(coordinates.first())
            val location = Coordinate(0.003, 0.004)
            route.navigate(location)
            val guidance = route.navigate(Coordinate(location.latitude + 0.000001, location.longitude))
            assertEquals(0f, guidance.offRouteDistance.meters().value, 5f)
            assertTrue(guidance.target.latitude > location.latitude)
            assertEquals(location.distanceTo(coordinates.last()), guidance.remainingDistance.meters().value, 5f)
        }
    }

    @Test
    fun `shortcut past a corner advances from restored progress`() {
        val path = listOf(Coordinate(0.0, 0.0), Coordinate(0.0, 0.004), Coordinate(0.004, 0.004))
            .mapIndexed { index, coordinate -> PathPoint(index.toLong(), 1, coordinate, 0f) }
        val route = createRoute(path)
        route.restorePosition(0.125f * route.length, Coordinate(0.0, 0.001))
        val location = Coordinate(0.002, 0.0039)
        route.navigate(location)
        val guidance = route.navigate(Coordinate(location.latitude + 0.000001, location.longitude))
        assertEquals(0.004, guidance.target.longitude, 0.00001)
        assertEquals(0.002 + 25 / 111_000.0, guidance.target.latitude, 0.00005)
        assertTrue(guidance.offRouteDistance.meters().value < 15f)
        assertEquals(
            guidance.offRouteDistance.meters().value + Coordinate(0.002, 0.004).distanceTo(path.last().coordinate),
            guidance.remainingDistance.meters().value,
            1f
        )
    }

    @Test
    fun `nearby return leg does not skip a long outbound segment`() {
        val path = listOf(
            Coordinate(0.0, 0.0), Coordinate(0.0, 0.01),
            Coordinate(0.0001, 0.01), Coordinate(0.0001, 0.0)
        ).mapIndexed { index, coordinate -> PathPoint(index.toLong(), 1, coordinate, 0f) }
        val route = createRoute(path)
        route.navigate(path.first().coordinate)
        val guidance = route.navigate(Coordinate(0.0001, 0.001))
        assertEquals(0.0, guidance.target.latitude, 0.00002)
        assertTrue(guidance.target.longitude > 0.001)
        assertFalse(guidance.arrived)
    }

    @Test
    fun `distance along is the cumulative distance with unequal segments`() {
        val path = points.take(2) + points.last().copy(coordinate = Coordinate(0.003, 0.001))
        val route = createRoute(path)
        assertEquals(0f, route.navigateFraction(path.first().coordinate))
        assertEquals(0.25f, route.navigateFraction(path[1].coordinate), 0.002f)
        assertEquals(0.625f, route.navigateFraction(Coordinate(0.0015, 0.001)), 0.002f)
        assertEquals(1f, route.navigateFraction(path.last().coordinate), 0.0001f)
    }

    @Test
    fun `progress weights elevation gain`() {
        val route = createRoute(points)
        val segment = points[0].coordinate.distanceTo(points[1].coordinate)
        val total = 2 * segment + HikingService.SCARF_ELEVATION_GAIN_FACTOR * 100f

        assertEquals(0f, route.navigate(points[0].coordinate).progress, 0.0001f)
        assertEquals(
            (segment + HikingService.SCARF_ELEVATION_GAIN_FACTOR * 100f) / total,
            route.navigate(points[1].coordinate).progress,
            0.002f
        )
        assertEquals(1f, route.navigate(points[2].coordinate).progress, 0.0001f)
    }

    @Test
    fun `restored distance locates the position along the route`() {
        val route = createRoute(points)
        val location = Coordinate(0.0, 0.0015)
        route.restorePosition(0.75f * route.length, location)
        val guidance = route.navigate(location)
        assertEquals(location.distanceTo(points.last().coordinate), guidance.remainingDistance.meters().value, 0.1f)
    }

    @Test
    fun `position saved as a distance locates the position on a longer route`() {
        val route = createRoute(points)
        val location = Coordinate(0.0, 0.0015)
        route.navigate(points.first().coordinate)
        val savedDistance = route.navigate(location).distanceAlong.meters().value

        val extended = createRoute(points + PathPoint(4, 1, Coordinate(0.0, 0.004), 0f))
        extended.restorePosition(savedDistance, location)
        val guidance = extended.navigate(location)
        assertEquals(location.distanceTo(Coordinate(0.0, 0.004)), guidance.remainingDistance.meters().value, 0.5f)
        assertEquals(points[2].coordinate, guidance.remainingRoute[1].coordinate)
    }

    @Test
    fun `remaining estimates use hiking service and include partial segment elevation`() {
        val route = createRoute(points)
        route.navigate(points.first().coordinate)
        val location = Coordinate(0.0, 0.0005)
        val guidance = route.navigate(location)
        val remaining = listOf(points[0].copy(coordinate = location, elevation = 50f)) + points.drop(1)
        assertEquals(50f, guidance.remainingElevationGain.meters().value, 0.1f)
        assertEquals(
            HikingService.getElevationLossGain(remaining).first.meters().value,
            guidance.remainingElevationLoss.meters().value, 0.1f
        )
        assertEquals(location.distanceTo(points.last().coordinate), guidance.remainingDistance.meters().value, 0.1f)
    }

    @Test
    fun `restored position produces the same guidance even at the last saved location`() {
        val route = createRoute(points)
        val location = Coordinate(0.0, 0.0005)
        route.navigate(points.first().coordinate)
        val expected = route.navigate(location)
        val restored = createRoute(points)
        restored.restorePosition(expected.distanceAlong.meters().value, location)
        assertEquals(expected, restored.navigate(location))
    }

    @Test
    fun `repeated location reuses guidance`() {
        val route = createRoute(points)
        val first = route.navigate(points.first().coordinate)
        assertSame(first, route.navigate(points.first().coordinate))
    }

    @Test
    fun `overlapping return leg does not cause early arrival`() {
        val loop = points + points[1] + points[0]
        val route = createRoute(loop)
        assertFalse(route.navigate(points[0].coordinate).arrived)
        assertFalse(route.navigate(points[1].coordinate).arrived)
    }

    @Test
    fun `arrival clears remaining estimates`() {
        val route = createRoute(points)
        points.dropLast(1).forEach { route.navigate(it.coordinate) }
        val arrived = route.navigate(points.last().coordinate)
        assertTrue(arrived.arrived)
        assertEquals(0f, arrived.remainingDistance.meters().value)
        assertEquals(0f, arrived.remainingElevationGain.meters().value)
        assertEquals(0f, arrived.remainingElevationLoss.meters().value)
    }

    @Test
    fun `exactly retraced turnaround prefers forward progress on the return leg`() {
        val route = createRoute(points + points[1] + points[0])
        val outbound = points.map { route.navigate(it.coordinate) }
        assertEquals(0.5f, outbound.last().distanceAlong.meters().value / route.length, 0.001f)
        val returning = route.navigate(points[1].coordinate)
        assertEquals(0.75f, returning.distanceAlong.meters().value / route.length, 0.001f)
        assertEquals(0.0, returning.target.latitude, 0.00001)
        assertTrue(returning.target.longitude < points[1].coordinate.longitude)
        assertEquals(points[1].coordinate.distanceTo(points[0].coordinate), returning.remainingDistance.meters().value, 1f)
        val arrived = route.navigate(points[0].coordinate)
        assertTrue(arrived.arrived)
        assertEquals(route.length, arrived.distanceAlong.meters().value)
    }

    @Test
    fun `backtracking still decreases progress when there is no matching forward leg`() {
        val route = createRoute(points)
        route.navigate(points[0].coordinate)
        assertEquals(0.5f, route.navigateFraction(points[1].coordinate), 0.001f)
        assertEquals(0.25f, route.navigateFraction(Coordinate(0.0, 0.0005)), 0.001f)
    }

    @Test
    fun `off route guidance leads back to the projected point`() {
        val guidance = createRoute(points).navigate(Coordinate(0.001, 0.0))
        assertTrue(guidance.target.distanceTo(points[0].coordinate) < 0.1f)
        assertEquals(
            guidance.offRouteDistance.meters().value + points[0].coordinate.distanceTo(points[2].coordinate),
            guidance.remainingDistance.meters().value,
            0.1f
        )
    }

    @Test
    fun `missing elevations are treated as 0`() {
        val guidance = createRoute(points.map { it.copy(elevation = null) }).navigate(points[0].coordinate)
        assertEquals(Distance.meters(0f), guidance.remainingElevationGain)
        assertEquals(Distance.meters(0f), guidance.remainingElevationLoss)
    }

    @Test
    fun `duplicate points can arrive without a segment`() {
        for (path in listOf(List(2) { points[0] }, List(3) { points[0] })) {
            val route = createRoute(path)
            val guidance = route.navigate(points[0].coordinate)
            assertTrue(guidance.arrived)
            assertEquals(route.length, guidance.distanceAlong.meters().value)
        }
    }

    @Test
    fun `first location far along the route rejoins there`() {
        val route = route(listOf(Coordinate(0.0, 0.0), Coordinate(0.0, 0.01), Coordinate(0.01, 0.01)))
        route.navigate(Coordinate(0.005, 0.01))
        val guidance = route.navigate(Coordinate(0.00501, 0.01))
        assertEquals(0.75f, guidance.distanceAlong.meters().value / route.length, 0.005f)
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
        assertTrue(guidance.remainingDistance.meters().value > 1000f)
    }

    @Test
    fun `first location near both the start and end matches the start`() {
        val start = Coordinate(0.0, 0.0)
        val route = route(listOf(start, Coordinate(0.0, 0.01), Coordinate(0.00001, 0.00001)))
        val guidance = route.navigate(Coordinate(0.00001, 0.00002))
        assertFalse(guidance.arrived)
        assertTrue(guidance.remainingDistance.meters().value > 1000f)
    }

    @Test
    fun `rejoining the route after a shortcut follows the new position`() {
        val route = route(listOf(
            Coordinate(0.0, 0.0), Coordinate(0.0, 0.01),
            Coordinate(0.001, 0.01), Coordinate(0.001, 0.0), Coordinate(0.002, 0.0)
        ))
        route.navigate(Coordinate(0.0, 0.0))
        route.navigate(Coordinate(0.0, 0.0002))
        // Cuts across to the return leg 55 m away
        val shortcut = Coordinate(0.0006, 0.0003)
        assertEquals(0.02f, route.navigateFraction(shortcut), 0.01f)
        assertEquals(0.02f, route.navigateFraction(shortcut), 0.01f)
        val rejoined = Coordinate(0.001, 0.0003)
        route.navigate(Coordinate(0.0009, 0.0003))
        assertTrue(route.navigateFraction(rejoined) > 0.5f)
    }

    @Test
    fun `one outlier across a short loop does not complete it`() {
        val start = Coordinate(0.0, 0.0)
        val opposite = Coordinate(0.0004505, 0.0004505)
        val coordinates = listOf(start, Coordinate(0.0, opposite.longitude), opposite,
            Coordinate(opposite.latitude, 0.0), start)
        val route = route(coordinates)
        route.navigate(start)
        route.navigate(opposite)
        for (location in listOf(start, Coordinate(0.0, 0.000009))) {
            val guidance = route.navigate(location)
            assertFalse(guidance.arrived)
            assertTrue(guidance.remainingDistance.meters().value > 180f)
        }
        for ((a, b) in coordinates.zipWithNext()) {
            for (step in 1..10) {
                route.navigate(Coordinate(
                    a.latitude + (b.latitude - a.latitude) * step / 10,
                    a.longitude + (b.longitude - a.longitude) * step / 10
                ))
            }
        }
        assertTrue(route.navigate(start).arrived)
    }

    @Test
    fun `a single outlier does not move the progress to another part of the route`() {
        val route = route(listOf(
            Coordinate(0.0, 0.0), Coordinate(0.0, 0.01),
            Coordinate(0.0003, 0.01), Coordinate(0.0003, 0.0)
        ))
        route.navigate(Coordinate(0.0, 0.0))
        route.navigate(Coordinate(0.0, 0.0005))
        assertEquals(0.03f, route.navigateFraction(Coordinate(0.0003, 0.0006)), 0.005f)
        assertEquals(0.035f, route.navigateFraction(Coordinate(0.0, 0.0007)), 0.005f)
    }

    @Test
    fun `targets the lookahead distance ahead on a dense path`() {
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
        var guidance = route.navigate(coordinates.first())
        for (i in 1..50) {
            guidance = route.navigate(Coordinate(0.00001, 0.0001 * i))
        }
        assertEquals(0.5f, guidance.distanceAlong.meters().value / route.length, 0.01f)
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
        var previousDistance = 0f
        var arrived = false
        for (recordedPoint in recorded) {
            val location = Coordinate(
                recordedPoint.latitude + random.nextGaussian() * 0.00003,
                recordedPoint.longitude + random.nextGaussian() * 0.00003
            )
            val guidance = route.navigate(location)
            assertTrue(guidance.distanceAlong.meters().value >= previousDistance - 25f)
            assertTrue(guidance.offRouteDistance.meters().value < 25f)
            previousDistance = guidance.distanceAlong.meters().value
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
        route.restorePosition(400f, Coordinate(0.0006, 0.003))
        val nearSeam = Coordinate(0.00002, 0.00002)
        route.navigate(nearSeam)
        val guidance = route.navigate(Coordinate(0.000021, 0.00002))
        assertFalse(guidance.arrived)
        assertTrue(guidance.remainingDistance.meters().value > 1000f)
    }

    @Test
    fun `a closer part of the route far behind is not rejoined`() {
        val route = route(listOf(
            Coordinate(0.0, 0.0), Coordinate(0.0, 0.003), Coordinate(0.0003, 0.003), Coordinate(0.0003, 0.0)
        ))
        // Midway along the return leg, which runs parallel to the first leg
        route.restorePosition(0.76f * route.length, Coordinate(0.0003, 0.0015))
        route.navigate(Coordinate(0.00006, 0.0015))
        val guidance = route.navigate(Coordinate(0.00006, 0.0015001))

        assertEquals(0.76f, guidance.distanceAlong.meters().value / route.length, 0.05f)
    }

    @Test
    fun `arrival radius widens with accuracy`() {
        val end = points.last().coordinate
        val shortOfEnd = Coordinate(0.0, end.longitude - 20.0 / 111_195.0)

        val withoutAccuracy = createRoute(points).apply { restorePosition(length, end) }.navigate(shortOfEnd, null)
        val withAccuracy = createRoute(points).apply { restorePosition(length, end) }
            .navigate(shortOfEnd, Distance.meters(40f))

        assertFalse(withoutAccuracy.arrived)
        assertTrue(withAccuracy.arrived)
    }

    @Test
    fun `arrival is kept until moving well away from the end`() {
        val route = createRoute(points)
        val end = points.last().coordinate
        val shortOfEnd = Coordinate(0.0, end.longitude - 20.0 / 111_195.0)
        val wellShortOfEnd = Coordinate(0.0, end.longitude - 40.0 / 111_195.0)
        route.restorePosition(route.length, end)

        assertTrue(route.navigate(end).arrived)
        assertTrue(route.navigate(shortOfEnd).arrived)
        assertFalse(route.navigate(wellShortOfEnd).arrived)
    }

    @Test
    fun `poor accuracy does not stop a nearby parallel leg from being rejoined`() {
        val route = route(listOf(
            Coordinate(0.0, 0.0), Coordinate(0.0, 0.01),
            Coordinate(0.0005, 0.01), Coordinate(0.0005, 0.0), Coordinate(0.001, 0.0)
        ))
        val accuracy = Distance.meters(75f)
        route.navigate(Coordinate(0.0, 0.0), accuracy)
        route.navigate(Coordinate(0.0, 0.0002), accuracy)
        // Cuts across to the return leg 55 m away
        route.navigate(Coordinate(0.0005, 0.0003), accuracy)
        val guidance = route.navigate(Coordinate(0.0005, 0.00031), accuracy)

        assertTrue(guidance.distanceAlong.meters().value / route.length > 0.5f)
    }

    private class TestClock : Clock() {
        private var now = Instant.EPOCH

        fun advance(seconds: Long) {
            now = now.plusSeconds(seconds)
        }

        override fun getZone(): ZoneId = ZoneId.of("UTC")
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = now
    }

    private fun meters(east: Double, north: Double) = Coordinate(north / 111_195.0, east / 111_195.0)

    // A hairpin: 200 m east, 20 m north, then 200 m back west, which ends 20 m from the start
    private val hairpin = listOf(meters(0.0, 0.0), meters(200.0, 0.0), meters(200.0, 20.0), meters(0.0, 20.0))

    @Test
    fun `a long gap between locations searches as far as a hiker could have walked`() {
        val clock = TestClock()
        val route = createRoute(hairpin.mapIndexed { index, it -> PathPoint(index.toLong(), 1, it, 0f) }, clock)
        route.navigate(hairpin.first())

        clock.advance(210)
        val guidance = route.navigate(hairpin.last())

        assertTrue(guidance.distanceAlong.meters().value / route.length > 0.9f)
    }

    @Test
    fun `a short gap between locations does not search as far as a hiker could have walked`() {
        val clock = TestClock()
        val route = createRoute(hairpin.mapIndexed { index, it -> PathPoint(index.toLong(), 1, it, 0f) }, clock)
        route.navigate(hairpin.first())

        clock.advance(1)
        val guidance = route.navigate(hairpin.last())

        assertTrue(guidance.distanceAlong.meters().value / route.length < 0.1f)
    }
}
