@file:Suppress("MemberVisibilityCanBePrivate")

package com.kylecorry.trail_sense.shared.data

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.filters.LoessFilter2D
import com.kylecorry.sol.math.statistics.Statistics
import com.kylecorry.sol.units.Reading
import java.time.Duration
import java.time.Instant
import kotlin.math.pow

object DataUtils {

    class AutomaticSmoothingOptions(
        val targetNoise: Float,
        val minimumSpan: Float,
        val maximumSpan: Float,
        val spanStep: Float,
        val maxSmoothingFactor: Float = DEFAULT_MAX_SMOOTHING_FACTOR,
        val spanFraction: Float = DEFAULT_SPAN_FRACTION
    )

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
            null,
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
        maximumSpanDistance: Float? = null,
        select: (index: Int, value: T) -> Vector2,
        merge: (value: T, smoothed: Vector2) -> T
    ): List<T> {
        val smoothed = smooth(data.mapIndexed { index, value -> select(index, value) }, smoothness, maximumSpanDistance)
        return data.zip(smoothed).map {
            merge(it.first, it.second)
        }
    }

    fun smooth(data: List<Vector2>, smoothness: Float = 0.06f, maximumSpanDistance: Float? = null): List<Vector2> {
        val filter = LoessFilter2D(
            smoothness,
            1,
            minimumSpanSize = if (smoothness == 0f) 0 else 10,
            maximumSpanDistance = maximumSpanDistance
        )
        return filter.filter(data)
    }

    /**
     * Smooths the data based on how noisy it is: clean data is barely smoothed while noisy data gets a wide span.
     * @param smoothingStrength 0 (none) to 1 (most), where 0.5 is the automatic amount
     * @param y the value to smooth, null values are ignored when estimating the noise
     */
    fun <T> smoothAutomatically(
        data: List<T>,
        options: AutomaticSmoothingOptions,
        smoothingStrength: Float = DEFAULT_AUTOMATIC_SMOOTHING_STRENGTH,
        x: (index: Int, value: T) -> Float,
        y: (value: T) -> Float?,
        merge: (value: T, smoothed: Float) -> T
    ): List<T> {
        if (data.size < 3 || smoothingStrength <= 0f) {
            return data
        }
        val xs = data.mapIndexed(x)
        val ys = data.map(y)
        val span = getNarrowestSpanForTargetNoise(xs, ys, options, smoothingStrength)
        val smoothness = (options.spanFraction * getSmoothingFactor(smoothingStrength, options)).coerceAtMost(1f)
        return smooth(data, smoothness, span, { index, _ -> Vector2(xs[index], ys[index] ?: 0f) }) { value, smoothed ->
            merge(value, smoothed.y)
        }
    }

    private fun getNarrowestSpanForTargetNoise(
        xs: List<Float>,
        ys: List<Float?>,
        options: AutomaticSmoothingOptions,
        smoothingStrength: Float
    ): Float {
        val factor = getSmoothingFactor(smoothingStrength, options)
        return LoessFilter2D.getSpanForNoise(
            xs,
            Statistics.estimateNoise(xs, ys),
            options.targetNoise / factor,
            options.minimumSpan * factor,
            options.maximumSpan * factor,
            options.spanStep * factor
        )
    }

    private fun getSmoothingFactor(smoothing: Float, options: AutomaticSmoothingOptions): Float {
        return options.maxSmoothingFactor.pow(2 * smoothing.coerceAtMost(1f) - 1)
    }

    const val DEFAULT_AUTOMATIC_SMOOTHING_STRENGTH = 0.5f
    const val DEFAULT_MAX_SMOOTHING_FACTOR = 4f
    const val DEFAULT_SPAN_FRACTION = 0.25f
}
