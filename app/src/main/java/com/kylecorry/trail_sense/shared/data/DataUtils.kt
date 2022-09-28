package com.kylecorry.trail_sense.shared.data

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.filters.LoessFilter2D
import com.kylecorry.sol.units.Reading
import java.time.Duration
import java.time.Instant

object DataUtils {

    fun smooth(data: List<Vector2>, smoothness: Float = 0.06f): List<Vector2> {
        val filter = LoessFilter2D(smoothness, 1, minimumSpanSize = if (smoothness == 0f) 0 else 10)
        return filter.filter(data)
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

}