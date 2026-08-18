package com.kylecorry.trail_sense.tools.paths.domain.hiking

import com.kylecorry.sol.science.geography.Geography
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HikingServiceTest {

    private val service = HikingService()

    @Test
    fun distancesAreCumulativeAndStartAtZero() {
        val points = listOf(coordinate(0.0), coordinate(0.001), coordinate(0.003))
        val firstLeg = points[0].distanceTo(points[1])
        val secondLeg = points[1].distanceTo(points[2])

        val distances = service.getDistances(points)

        assertEquals(3, distances.size)
        assertEquals(0f, distances[0], 0.001f)
        assertEquals(firstLeg, distances[1], 0.001f)
        assertEquals(firstLeg + secondLeg, distances[2], 0.001f)
    }

    @Test
    fun eachLegIsAtLeastTheMinimumDistance() {
        // Repeated coordinates (ex. a mocked GPS) would otherwise all share a distance of 0
        val points = List(3) { coordinate(0.0) }

        val distances = service.getDistances(points, 0.1f)

        assertEquals(listOf(0.1f, 0.2f, 0.3f), distances)
    }

    @Test
    fun noPointsHaveNoDistances() {
        assertEquals(emptyList<Float>(), service.getDistances(emptyList()))
    }

    @Test
    fun elevationGainSumsTheClimbsAndLossSumsTheDescents() {
        val path = path(100f, 150f, 120f, 180f)

        val (loss, gain) = service.getElevationLossGain(path)

        assertEquals(110f, gain.meters().value, 0.001f)
        assertEquals(-30f, loss.meters().value, 0.001f)
        assertEquals(gain.meters().value, service.getElevationGain(path).meters().value, 0.001f)
    }

    @Test
    fun pointsWithoutAnElevationAreSkipped() {
        val path = path(100f, null, 150f)

        val (loss, gain) = service.getElevationLossGain(path)

        assertEquals(50f, gain.meters().value, 0.001f)
        assertEquals(0f, loss.meters().value, 0.001f)
    }

    @Test
    fun anEmptyPathHasNoElevationChange() {
        val (loss, gain) = service.getElevationLossGain(emptyList())

        assertEquals(0f, gain.meters().value, 0.001f)
        assertEquals(0f, loss.meters().value, 0.001f)
    }

    @Test
    fun averagePaceSlowsAsTheHikeGetsHarder() {
        assertEquals(
            1.5f,
            service.getAveragePace(HikingDifficulty.Easy, 1f).value,
            0.001f
        )
        assertEquals(
            1.4f,
            service.getAveragePace(HikingDifficulty.Moderate, 1f).value,
            0.001f
        )
        assertEquals(
            1.2f,
            service.getAveragePace(HikingDifficulty.Hard, 1f).value,
            0.001f
        )
    }

    @Test
    fun averagePaceIsScaledByTheFactor() {
        val pace = service.getAveragePace(HikingDifficulty.Easy, 2f)

        assertEquals(3f, pace.value, 0.001f)
        assertEquals(DistanceUnits.Miles, pace.distanceUnits)
        assertEquals(TimeUnits.Hours, pace.timeUnits)
    }

    @Test
    fun flatHikeDurationIsDistanceOverPace() {
        val path = path(0f, 0f, 0f)
        val distance = Geography.getPathDistance(path.map { it.coordinate }).meters().value

        val duration = service.getHikingDuration(path, oneMeterPerSecond)

        assertEquals(distance.toDouble(), duration.seconds.toDouble(), 1.0)
    }

    @Test
    fun scarfsRuleAddsSevenPointNineTwoMetersOfFlatDistancePerMeterClimbed() {
        val flat = service.getHikingDuration(path(0f, 0f, 0f), oneMeterPerSecond)
        val climbing = service.getHikingDuration(path(0f, 60f, 100f), oneMeterPerSecond)

        assertEquals(
            7.92 * 100,
            (climbing.seconds - flat.seconds).toDouble(),
            1.0
        )
    }

    @Test
    fun descendingDoesNotAddTimeToTheHike() {
        val flat = service.getHikingDuration(path(0f, 0f, 0f), oneMeterPerSecond)
        val descending = service.getHikingDuration(path(100f, 50f, 0f), oneMeterPerSecond)

        assertEquals(flat, descending)
    }

    @Test
    fun aFasterPaceTakesProportionallyLessTime() {
        val path = path(0f, 60f, 100f)

        val slow = service.getHikingDuration(path, oneMeterPerSecond)
        val fast = service.getHikingDuration(
            path,
            Speed.from(2f, DistanceUnits.Meters, TimeUnits.Seconds)
        )

        assertEquals(slow.seconds / 2.0, fast.seconds.toDouble(), 1.0)
    }

    @Test
    fun theDifficultyDeterminesThePaceUsedForTheDuration() {
        val path = path(0f, 0f, 0f)

        val easy = service.getHikingDuration(path, 1f, HikingDifficulty.Easy)
        val hard = service.getHikingDuration(path, 1f, HikingDifficulty.Hard)

        assertEquals(
            service.getHikingDuration(path, service.getAveragePace(HikingDifficulty.Easy, 1f)),
            easy
        )
        assertTrue(hard > easy, "A hard hike is slower than an easy one")
    }

    @Test
    fun slopesAreReturnedBetweenConsecutivePoints() {
        val path = path(0f, 100f, 50f)
        val run = path[0].coordinate.distanceTo(path[1].coordinate)

        val slopes = service.getSlopes(path)

        assertEquals(2, slopes.size)
        assertEquals(path[0], slopes[0].first)
        assertEquals(path[1], slopes[0].second)
        assertEquals(100f / run * 100f, slopes[0].third, 0.1f)
        assertTrue(slopes[1].third < 0f, "The second leg descends")
    }

    @Test
    fun aSinglePointHasNoSlopes() {
        assertTrue(service.getSlopes(path(0f)).isEmpty())
    }

    @Test
    fun correctingElevationsOfAnEmptyPathReturnsNothing() {
        assertEquals(emptyList<PathPoint>(), service.correctElevations(emptyList()))
    }

    @Test
    fun correctingElevationsLeavesMissingElevationsAlone() {
        val path = path(100f, null, 100f, 100f, null, 100f, 100f, 100f, 100f, 100f, 100f, 100f)

        val corrected = service.correctElevations(path)

        assertNull(corrected[1].elevation)
        assertNull(corrected[4].elevation)
    }

    @Test
    fun correctingElevationsOfAFlatPathChangesNothing() {
        val path = path(*Array(12) { 100f })

        val corrected = service.correctElevations(path)

        corrected.forEach {
            assertEquals(100f, it.elevation!!, 0.01f)
        }
    }

    @Test
    fun correctingElevationsSmoothsOutAnOutlier() {
        val elevations = Array<Float?>(15) { 100f }
        elevations[7] = 200f

        val corrected = service.correctElevations(path(*elevations))

        assertTrue(
            corrected[7].elevation!! < 200f,
            "The spike should be pulled toward its neighbors, was ${corrected[7].elevation}"
        )
        assertEquals(15, corrected.size)
    }

    private fun path(vararg elevations: Float?): List<PathPoint> {
        return elevations.mapIndexed { index, elevation ->
            PathPoint(
                index.toLong(),
                1,
                coordinate(index * 0.001),
                elevation
            )
        }
    }

    private fun coordinate(latitude: Double): Coordinate {
        return Coordinate(latitude, 0.0)
    }

    companion object {
        private val oneMeterPerSecond =
            Speed.from(1f, DistanceUnits.Meters, TimeUnits.Seconds)
    }
}
