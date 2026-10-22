package com.kylecorry.trail_sense.shared.data

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.filters.LoessFilter2D
import com.kylecorry.sol.units.Reading
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.Random
import kotlin.math.pow

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
            null,
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

    private val options = DataUtils.AutomaticSmoothingOptions(0.1f, 1f, 50f, 1f)

    private fun gaussian(count: Int, scale: Float = 1f): List<Float> {
        val random = Random(1)
        return List(count) { scale * random.nextGaussian().toFloat() }
    }

    private fun points(ys: List<Float>) = ys.mapIndexed { index, y -> Vector2(index.toFloat(), y) }

    @Test
    fun smoothAutomaticallyOnlySmoothsNoisyData() {
        val clean = points(gaussian(200, 0.05f))
        val noisy = points(gaussian(200, 2f))

        // The noise of the clean data is already below the target, so the narrowest span is used
        smoothAutomatically(clean, 0.5f).zip(clean).forEach { (smoothed, original) ->
            assertEquals(original.y, smoothed.y, 0.0001f)
        }
        assertTrue(smoothAutomatically(noisy, 0.5f).sumOf { it.y.toDouble().let { y -> y * y } } < noisy.sumOf { it.y.toDouble().let { y -> y * y } } / 2)
    }

    @Test
    fun smoothAutomaticallySmoothsMoreAsTheSmoothingIncreases() {
        val data = points(gaussian(200))

        // No smoothing at 0, then wider spans (the energy of adjacent values can tie, since the span is searched in steps)
        val energies = listOf(0f, 0.25f, 0.5f, 1f).map { smoothing ->
            smoothAutomatically(data, smoothing).sumOf { it.y.toDouble().let { y -> y * y } }
        }

        assertEquals(energies.sortedDescending(), energies, "Was $energies")
        assertTrue(energies.last() < energies.first())
    }

    @Test
    fun smoothAutomaticallyUsesTheAutomaticAmountByDefault() {
        val data = points(gaussian(100))

        assertEquals(
            smoothAutomatically(data, 0.5f),
            DataUtils.smoothAutomatically(
                data,
                options,
                x = { _, point -> point.x },
                y = { it.y },
                merge = { point, y -> point.copy(y = y) }
            )
        )
    }

    @Test
    fun smoothAutomaticallyScalesTheSpanByTheMaximumSmoothingFactorAtTheEnds() {
        val data = points(gaussian(200, 5f))
        // The noise can't be reduced to the target, so the widest span is used
        val strict = DataUtils.AutomaticSmoothingOptions(0.0001f, 1f, 50f, 1f, 10f)

        listOf(0.25f, 0.5f, 1f).forEach { smoothing ->
            val factor = 10f.pow(2 * smoothing - 1)
            val filter = LoessFilter2D(
                (0.25f * factor).coerceAtMost(1f),
                1,
                minimumSpanSize = 10,
                maximumSpanDistance = 50f * factor
            )
            assertEquals(filter.filter(data), smoothAutomatically(data, smoothing, strict), "Smoothing $smoothing")
        }
    }

    @Test
    fun smoothAutomaticallyReturnsTheDataWhenThereIsNothingToSmooth() {
        val data = gaussian(20, 5f).mapIndexed { index, y -> Vector2(index.toFloat(), y) }

        assertEquals(data, smoothAutomatically(data, 0f))
        assertEquals(data.take(2), smoothAutomatically(data.take(2), 0.5f))
        assertEquals(emptyList<Vector2>(), smoothAutomatically(emptyList(), 0.5f))
    }

    @Test
    fun smoothAutomaticallyKeepsXAndRemovesNoise() {
        val trend = List(100) { it * 0.5f }
        val noise = gaussian(100, 3f)
        val data = trend.indices.map { Vector2(it.toFloat(), trend[it] + noise[it]) }

        val smoothed = smoothAutomatically(data, 0.5f)

        assertEquals(data.map { it.x }, smoothed.map { it.x })
        val before = data.indices.sumOf { ((data[it].y - trend[it]) * (data[it].y - trend[it])).toDouble() }
        val after = smoothed.indices.sumOf { ((smoothed[it].y - trend[it]) * (smoothed[it].y - trend[it])).toDouble() }
        assertTrue(after < before / 2, "Error went from $before to $after")
    }

    @Test
    fun smoothAutomaticallyLeavesOutValuesWithoutAY() {
        val data = List(30) { Vector2(it.toFloat(), 10f) }

        val smoothed = DataUtils.smoothAutomatically(
            data,
            options,
            x = { _, point -> point.x },
            y = { if (it.x == 5f) null else it.y },
            merge = { point, y -> point.copy(y = y) }
        )

        // The unknown y is smoothed as 0, but it must not be treated as noise in the rest of the data
        assertEquals(10f, smoothed[20].y, 0.01f)
    }

    @Test
    fun smoothAutomaticallyLimitsTheWindowToTheSpanFraction() {
        val data = gaussian(200, 3f).mapIndexed { index, y -> Vector2(index.toFloat(), y) }
        val wide = DataUtils.AutomaticSmoothingOptions(0.0001f, 40f, 50f, 1f, spanFraction = 1f)
        val limited = DataUtils.AutomaticSmoothingOptions(0.0001f, 40f, 50f, 1f, spanFraction = 0.05f)

        val all = smoothAutomatically(data, 0.5f, wide)
        val few = smoothAutomatically(data, 0.5f, limited)

        // Every window is capped to 10 points, so it can't average as much noise away
        assertTrue(few.sumOf { it.y.toDouble().let { y -> y * y } } > all.sumOf { it.y.toDouble().let { y -> y * y } })
    }

    private fun smoothAutomatically(
        data: List<Vector2>,
        smoothing: Float,
        options: DataUtils.AutomaticSmoothingOptions = this.options
    ): List<Vector2> {
        return DataUtils.smoothAutomatically(
            data,
            options,
            smoothing,
            x = { _, point -> point.x },
            y = { it.y },
            merge = { point, y -> point.copy(y = y) }
        )
    }
}
