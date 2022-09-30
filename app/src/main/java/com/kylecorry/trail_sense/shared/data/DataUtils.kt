package com.kylecorry.trail_sense.shared.data

import com.kylecorry.sol.math.SolMath.cosDegrees
import com.kylecorry.sol.math.SolMath.sinDegrees
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.filters.LoessFilter
import com.kylecorry.sol.math.filters.LoessFilter2D
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.navigation.domain.hiking.HikingService
import java.time.Duration
import java.time.Instant

object DataUtils {

    enum class GeospatialSmoothingType {
        Nearby,
        Path,
        FromStart
    }

    fun <T> smoothGeospatial(
        data: List<T>,
        smoothness: Float = 0.1f,
        type: GeospatialSmoothingType,
        location: (value: T) -> Coordinate,
        select: (value: T) -> Float,
        merge: (value: T, smoothed: Float) -> T
    ): List<T> {
        return when (type) {
            GeospatialSmoothingType.Nearby -> { // TODO: This isn't performant enough, but is the preferred one
                val start = data.firstOrNull()?.let(location) ?: Coordinate.zero
                smoothMultivariate(
                    data,
                    smoothness,
                    { _, reading ->
                        val distance = location(reading).distanceTo(start) / 1000f
                        val direction = start.bearingTo(location(reading))
                        val east = sinDegrees(direction.value) * distance
                        val north = cosDegrees(direction.value) * distance
                        listOf(east, north) to select(reading)
                    }
                ) { reading, smoothed ->
                    merge(reading, smoothed)
                }
            }
            GeospatialSmoothingType.Path -> {
                val distances = HikingService().getDistances(data.map(location))
                smooth(
                    data,
                    smoothness,
                    { index, value -> Vector2(distances[index] / 1000f, select(value)) }
                ) { point, smoothed ->
                    merge(point, smoothed.y)
                }
            }
            GeospatialSmoothingType.FromStart -> {
                val start = data.firstOrNull()?.let(location) ?: Coordinate.zero
                smooth(
                    data,
                    smoothness,
                    { _, value ->
                        Vector2(
                            location(value).distanceTo(start) / 1000f,
                            select(value)
                        )
                    }
                ) { point, smoothed ->
                    merge(point, smoothed.y)
                }
            }
        }
    }

    fun smoothTemporal(data: List<Reading<Float>>, smoothness: Float = 0.1f): List<Reading<Float>> {
        return smoothTemporal(data, smoothness, { it }) { _, smoothed -> smoothed }
    }

    fun <T> smoothTemporal(
        data: List<Reading<T>>,
        smoothness: Float = 0.1f,
        select: (value: T) -> Float,
        merge: (value: T, smoothed: Float) -> T
    ): List<Reading<T>> {
        val start = data.firstOrNull()?.time ?: Instant.now()
        return smooth(
            data,
            smoothness,
            { _, reading ->
                Vector2(
                    Duration.between(start, reading.time).toMillis() / 1000f,
                    select(reading.value)
                )
            }
        ) { reading, smoothed ->
            reading.copy(value = merge(reading.value, smoothed.y))
        }
    }

    fun <T> smooth(
        data: List<T>,
        smoothness: Float = 0.1f,
        select: (index: Int, value: T) -> Vector2,
        merge: (value: T, smoothed: Vector2) -> T
    ): List<T> {
        val smoothed = smooth(data.mapIndexed { index, value -> select(index, value) }, smoothness)
        return data.zip(smoothed).map {
            merge(it.first, it.second)
        }
    }

    fun smooth(data: List<Vector2>, smoothness: Float = 0.06f): List<Vector2> {
        val filter = LoessFilter2D(smoothness, 1, minimumSpanSize = if (smoothness == 0f) 0 else 10)
        return filter.filter(data)
    }

    private fun <T> smoothMultivariate(
        data: List<T>,
        smoothness: Float = 0.1f,
        select: (index: Int, value: T) -> Pair<List<Float>, Float>,
        merge: (value: T, smoothed: Float) -> T
    ): List<T> {
        val d = data.mapIndexed { index, value -> select(index, value) }.unzip()
        val smoothed = smoothMultivariate(d.first, d.second, smoothness)
        return data.zip(smoothed).map {
            merge(it.first, it.second)
        }
    }

    private fun smoothMultivariate(
        xs: List<List<Float>>,
        ys: List<Float>,
        smoothness: Float = 0.06f
    ): List<Float> {
        val filter = LoessFilter(
            smoothness,
            1,
            minimumSpanSize = if (smoothness == 0f) 0 else 10
        )
        return filter.filter(xs, ys)
    }

}