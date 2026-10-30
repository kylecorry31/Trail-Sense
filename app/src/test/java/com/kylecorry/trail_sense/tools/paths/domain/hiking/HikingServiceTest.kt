package com.kylecorry.trail_sense.tools.paths.domain.hiking

import com.kylecorry.sol.science.geography.Geography
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HikingServiceTest {


    @Test
    fun distancesAreCumulativeAndStartAtZero() {
        val points = listOf(coordinate(0.0), coordinate(0.001), coordinate(0.003))
        val firstLeg = points[0].distanceTo(points[1])
        val secondLeg = points[1].distanceTo(points[2])

        val distances = HikingService.getDistances(points)

        assertEquals(3, distances.size)
        assertEquals(0f, distances[0], 0.001f)
        assertEquals(firstLeg, distances[1], 0.001f)
        assertEquals(firstLeg + secondLeg, distances[2], 0.001f)
    }

    @Test
    fun eachLegIsAtLeastTheMinimumDistance() {
        // Repeated coordinates (ex. a mocked GPS) would otherwise all share a distance of 0
        val points = List(3) { coordinate(0.0) }

        val distances = HikingService.getDistances(points, 0.1f)

        assertEquals(listOf(0.1f, 0.2f, 0.3f), distances)
    }

    @Test
    fun noPointsHaveNoDistances() {
        assertEquals(emptyList<Float>(), HikingService.getDistances(emptyList()))
    }

    @Test
    fun elevationGainSumsTheClimbsAndLossSumsTheDescents() {
        val path = path(100f, 150f, 120f, 180f)

        val (loss, gain) = HikingService.getElevationLossGain(path)

        assertEquals(110f, gain.meters().value, 0.001f)
        assertEquals(-30f, loss.meters().value, 0.001f)
        assertEquals(gain.meters().value, HikingService.getElevationGain(path).meters().value, 0.001f)
    }

    @Test
    fun pointsWithoutAnElevationAreSkipped() {
        val path = path(100f, null, 150f)

        val (loss, gain) = HikingService.getElevationLossGain(path)

        assertEquals(50f, gain.meters().value, 0.001f)
        assertEquals(0f, loss.meters().value, 0.001f)
    }

    @Test
    fun anEmptyPathHasNoElevationChange() {
        val (loss, gain) = HikingService.getElevationLossGain(emptyList())

        assertEquals(0f, gain.meters().value, 0.001f)
        assertEquals(0f, loss.meters().value, 0.001f)
    }

    @Test
    fun averagePaceSlowsAsTheHikeGetsHarder() {
        assertEquals(
            1.5f,
            HikingService.getAveragePace(HikingDifficulty.Easy, 1f).value,
            0.001f
        )
        assertEquals(
            1.4f,
            HikingService.getAveragePace(HikingDifficulty.Moderate, 1f).value,
            0.001f
        )
        assertEquals(
            1.2f,
            HikingService.getAveragePace(HikingDifficulty.Hard, 1f).value,
            0.001f
        )
    }

    @Test
    fun averagePaceIsScaledByTheFactor() {
        val pace = HikingService.getAveragePace(HikingDifficulty.Easy, 2f)

        assertEquals(3f, pace.value, 0.001f)
        assertEquals(DistanceUnits.Miles, pace.distanceUnits)
        assertEquals(TimeUnits.Hours, pace.timeUnits)
    }

    @Test
    fun flatHikeDurationIsDistanceOverPace() {
        val path = path(0f, 0f, 0f)
        val distance = Geography.getPathDistance(path.map { it.coordinate }).meters().value

        val duration = HikingService.getHikingDuration(path, oneMeterPerSecond)

        assertEquals(distance.toDouble(), duration.seconds.toDouble(), 1.0)
    }

    @Test
    fun scarfsRuleAddsSevenPointNineTwoMetersOfFlatDistancePerMeterClimbed() {
        val flat = HikingService.getHikingDuration(path(0f, 0f, 0f), oneMeterPerSecond)
        val climbing = HikingService.getHikingDuration(path(0f, 60f, 100f), oneMeterPerSecond)

        assertEquals(
            7.92 * 100,
            (climbing.seconds - flat.seconds).toDouble(),
            1.0
        )
    }

    @Test
    fun durationFromDistanceAndGainConvertsUnitsAndAppliesScarfsRule() {
        val speed = Speed.from(3.6f, DistanceUnits.Kilometers, TimeUnits.Hours)

        val flat = HikingService.getHikingDuration(Distance.kilometers(3.6f), Distance.meters(0f), speed)
        val climbing = HikingService.getHikingDuration(Distance.kilometers(3.6f), Distance.meters(100f), speed)

        assertEquals(3600L, flat.seconds)
        assertEquals((3600 + 7.92 * 100).toLong(), climbing.seconds)
    }

    @Test
    fun descendingDoesNotAddTimeToTheHike() {
        val flat = HikingService.getHikingDuration(path(0f, 0f, 0f), oneMeterPerSecond)
        val descending = HikingService.getHikingDuration(path(100f, 50f, 0f), oneMeterPerSecond)

        assertEquals(flat, descending)
    }

    @Test
    fun aFasterPaceTakesProportionallyLessTime() {
        val path = path(0f, 60f, 100f)

        val slow = HikingService.getHikingDuration(path, oneMeterPerSecond)
        val fast = HikingService.getHikingDuration(
            path,
            Speed.from(2f, DistanceUnits.Meters, TimeUnits.Seconds)
        )

        assertEquals(slow.seconds / 2.0, fast.seconds.toDouble(), 1.0)
    }

    @Test
    fun theDifficultyDeterminesThePaceUsedForTheDuration() {
        val path = path(0f, 0f, 0f)

        val easy = HikingService.getHikingDuration(path, 1f, HikingDifficulty.Easy)
        val hard = HikingService.getHikingDuration(path, 1f, HikingDifficulty.Hard)

        assertEquals(
            HikingService.getHikingDuration(path, HikingService.getAveragePace(HikingDifficulty.Easy, 1f)),
            easy
        )
        assertTrue(hard > easy, "A hard hike is slower than an easy one")
    }

    @Test
    fun slopesAreReturnedBetweenConsecutivePoints() {
        val path = path(0f, 100f, 50f)
        val run = path[0].coordinate.distanceTo(path[1].coordinate)

        val slopes = HikingService.getSlopes(path)

        assertEquals(2, slopes.size)
        assertEquals(path[0], slopes[0].first)
        assertEquals(path[1], slopes[0].second)
        assertEquals(100f / run * 100f, slopes[0].third, 0.1f)
        assertTrue(slopes[1].third < 0f, "The second leg descends")
    }

    @Test
    fun aSinglePointHasNoSlopes() {
        assertTrue(HikingService.getSlopes(path(0f)).isEmpty())
    }

    @Test
    fun correctingElevationsOfAnEmptyPathReturnsNothing() {
        assertEquals(emptyList<PathPoint>(), HikingService.correctElevations(emptyList()))
    }

    @Test
    fun correctingElevationsLeavesMissingElevationsAlone() {
        val path = path(100f, null, 100f, 100f, null, 100f, 100f, 100f, 100f, 100f, 100f, 100f)

        val corrected = HikingService.correctElevations(path)

        assertNull(corrected[1].elevation)
        assertNull(corrected[4].elevation)
    }

    @Test
    fun correctingElevationsOfAFlatPathChangesNothing() {
        val path = path(*Array(12) { 100f })

        val corrected = HikingService.correctElevations(path)

        corrected.forEach {
            assertEquals(100f, it.elevation!!, 0.01f)
        }
    }

    @Test
    fun correctingElevationsSmoothsOutAnOutlier() {
        val elevations = Array<Float?>(15) { 100f }
        elevations[7] = 200f

        val corrected = HikingService.correctElevations(path(*elevations))

        assertTrue(
            corrected[7].elevation!! < 200f,
            "The spike should be pulled toward its neighbors, was ${corrected[7].elevation}"
        )
        assertEquals(15, corrected.size)
    }

    @Test
    fun correctingElevationsKeepsACleanClimb() {
        val elevations = Array<Float?>(40) { 100f + it * 5f }

        val corrected = HikingService.correctElevations(path(*elevations))

        corrected.forEachIndexed { index, point ->
            assertEquals(elevations[index]!!, point.elevation!!, 0.5f)
        }
    }

    @Test
    fun correctingElevationsRemovesNoiseFromAClimb() {
        val trend = List(40) { 100f + it * 5f }
        val noisy = Array<Float?>(40) { trend[it] + if (it % 2 == 0) 15f else -15f }

        val corrected = HikingService.correctElevations(path(*noisy))

        val error = corrected.indices.map { (corrected[it].elevation!! - trend[it]).let { d -> d * d } }.average()
        assertTrue(error < 15f * 15f / 4, "The noise should be at least halved, mean squared error was $error")
    }

    @Test
    fun correctingElevationsWithNoSmoothingChangesNothing() {
        val path = path(*Array<Float?>(20) { 100f + (it % 3) * 10f })

        assertEquals(path, HikingService.correctElevations(path, 0f))
    }

    @Test
    fun correctingElevationsUsesMediumSmoothingByDefault() {
        val path = path(*Array<Float?>(40) { 100f + it * 5f + if (it % 2 == 0) 15f else -15f })

        assertEquals(HikingService.correctElevations(path, 0.5f), HikingService.correctElevations(path))
    }

    @Test
    fun moreSmoothingRemovesMoreNoise() {
        val trend = List(40) { 100f + it * 5f }
        val noisy = path(*Array<Float?>(40) { trend[it] + if (it % 2 == 0) 15f else -15f })

        val errors = listOf(0.25f, 0.5f, 0.75f).map { smoothing ->
            val corrected = HikingService.correctElevations(noisy, smoothing)
            corrected.indices.map { (corrected[it].elevation!! - trend[it]).let { d -> d * d } }.average()
        }

        assertTrue(errors[0] >= errors[1] && errors[1] >= errors[2], "More smoothing should not add error, was $errors")
        assertTrue(errors[2] < errors[0], "The most smoothing should have less error than the least, was $errors")
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
