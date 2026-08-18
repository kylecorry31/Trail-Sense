package com.kylecorry.trail_sense.shared.data

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class DataUtilsTest {

    @Test
    fun smoothingKeepsTheNumberOfPointsAndTheirXValues() {
        val data = List(20) { Vector2(it.toFloat(), it.toFloat()) }

        val smoothed = DataUtils.smooth(data)

        assertEquals(data.size, smoothed.size)
        data.zip(smoothed).forEach { (original, result) ->
            assertEquals(original.x, result.x, 0.001f)
        }
    }

    @Test
    fun smoothingALineLeavesItUnchanged() {
        val data = List(20) { Vector2(it.toFloat(), 2f * it + 5f) }

        val smoothed = DataUtils.smooth(data)

        data.zip(smoothed).forEach { (original, result) ->
            assertEquals(original.y, result.y, 0.01f)
        }
    }

    @Test
    fun smoothingPullsAnOutlierTowardItsNeighbors() {
        val data = List(20) { Vector2(it.toFloat(), if (it == 10) 100f else 0f) }

        val smoothed = DataUtils.smooth(data, 0.3f)

        assertTrue(
            smoothed[10].y < 50f,
            "The spike should be reduced, was ${smoothed[10].y}"
        )
        assertTrue(
            smoothed.all { it.y.isFinite() && it.y < 100f },
            "No point should exceed the original spike"
        )
    }

    @Test
    fun aSmoothnessOfZeroLeavesTheDataAlone() {
        val data = List(20) { Vector2(it.toFloat(), if (it == 10) 100f else 0f) }

        val smoothed = DataUtils.smooth(data, 0f)

        data.zip(smoothed).forEach { (original, result) ->
            assertEquals(original.y, result.y, 0.001f)
        }
    }

    @Test
    fun smoothingEmptyDataReturnsEmptyData() {
        assertEquals(emptyList<Vector2>(), DataUtils.smooth(emptyList()))
    }

    @Test
    fun smoothingCanSelectAndMergeArbitraryValues() {
        val data = List(20) { it }

        val smoothed = DataUtils.smooth(
            data,
            0.1f,
            { index, value -> Vector2(index.toFloat(), value.toFloat()) }
        ) { _, vector -> vector.y.toInt() }

        assertEquals(data, smoothed)
    }

    @Test
    fun temporalSmoothingKeepsTheReadingTimes() {
        val start = Instant.ofEpochSecond(1704110400)
        val data = List(20) {
            Reading(if (it == 10) 100f else 0f, start.plus(Duration.ofMinutes(it.toLong())))
        }

        val smoothed = DataUtils.smoothTemporal(data, 0.3f)

        assertEquals(data.map { it.time }, smoothed.map { it.time })
        assertTrue(smoothed[10].value < 100f, "The spike should be reduced")
    }

    @Test
    fun temporalSmoothingUsesTheTimeBetweenReadingsRatherThanTheirIndex() {
        val start = Instant.ofEpochSecond(1704110400)
        // The last reading is far away in time, so it should barely influence the others
        val times = (0 until 19).map { it.toLong() } + 10_000L
        val data = times.mapIndexed { index, minutes ->
            Reading(
                if (index == times.lastIndex) 1000f else 0f,
                start.plus(Duration.ofMinutes(minutes))
            )
        }

        val smoothed = DataUtils.smoothTemporal(data, 0.3f)

        assertEquals(0f, smoothed[0].value, 1f)
        assertEquals(0f, smoothed[9].value, 1f)
    }

    @Test
    fun geospatialPathSmoothingUsesDistanceAlongThePath() {
        val data = List(20) {
            Coordinate(it * 0.001, 0.0) to if (it == 10) 100f else 0f
        }

        val smoothed = DataUtils.smoothGeospatial(
            data,
            0.3f,
            DataUtils.GeospatialSmoothingType.Path,
            { it.first },
            { it.second }
        ) { value, smoothedValue -> value.first to smoothedValue }

        assertEquals(data.map { it.first }, smoothed.map { it.first })
        assertTrue(
            smoothed[10].second < 50f,
            "The spike should be reduced, was ${smoothed[10].second}"
        )
    }

    @Test
    fun geospatialSmoothingOfIdenticalCoordinatesFallsBackToOrder() {
        // All points share a location (ex. a mocked GPS), so the minimum distance keeps them distinct
        val data = List(20) { Coordinate.zero to if (it == 10) 100f else 0f }

        val smoothed = DataUtils.smoothGeospatial(
            data,
            0.3f,
            DataUtils.GeospatialSmoothingType.Path,
            { it.first },
            { it.second }
        ) { value, smoothedValue -> value.first to smoothedValue }

        assertEquals(20, smoothed.size)
        assertTrue(smoothed[10].second < 100f, "The spike should be reduced")
    }

    @Test
    fun geospatialFromStartSmoothingUsesDistanceFromTheFirstPoint() {
        val data = List(20) {
            Coordinate(it * 0.001, 0.0) to if (it == 10) 100f else 0f
        }

        val smoothed = DataUtils.smoothGeospatial(
            data,
            0.3f,
            DataUtils.GeospatialSmoothingType.FromStart,
            { it.first },
            { it.second }
        ) { value, smoothedValue -> value.first to smoothedValue }

        assertEquals(20, smoothed.size)
        assertTrue(smoothed[10].second < 100f, "The spike should be reduced")
    }

    @Test
    fun geospatialNearbySmoothingUsesTheEastNorthOffsetFromTheFirstPoint() {
        val data = List(20) {
            Coordinate(it * 0.001, 0.0) to if (it == 10) 100f else 0f
        }

        val smoothed = DataUtils.smoothGeospatial(
            data,
            0.3f,
            DataUtils.GeospatialSmoothingType.Nearby,
            { it.first },
            { it.second }
        ) { value, smoothedValue -> value.first to smoothedValue }

        assertEquals(20, smoothed.size)
        assertTrue(smoothed[10].second < 100f, "The spike should be reduced")
    }

    @Test
    fun geospatialSmoothingOfEmptyDataReturnsEmptyData() {
        val smoothed = DataUtils.smoothGeospatial(
            emptyList<Pair<Coordinate, Float>>(),
            0.1f,
            DataUtils.GeospatialSmoothingType.Path,
            { it.first },
            { it.second }
        ) { value, smoothedValue -> value.first to smoothedValue }

        assertEquals(emptyList<Pair<Coordinate, Float>>(), smoothed)
    }
}
