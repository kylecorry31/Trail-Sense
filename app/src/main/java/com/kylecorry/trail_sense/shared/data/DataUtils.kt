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

    /**
     * How the amount of smoothing is chosen for data of a given kind. The span distances are in the units of the
     * x values.
     * @param targetNoise the standard deviation of the noise that is acceptable after smoothing
     * @param minimumSpan the narrowest span distance to use for the default smoothing
     * @param maximumSpan the widest span distance to use for the default smoothing
     * @param spanStep the amount to widen the span distance by when searching for the default smoothing
     * @param maxSmoothingFactor how many times more or less smoothing than the automatic amount each end of the
     * strength range gives
     * @param spanFraction the fraction of the points which can be included when smoothing a point, at the default
     * smoothing (it scales with the smoothing strength). The span distance is the driving factor, so this has to be
     * large enough to include all of the points within it.
     */
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
     * Smooths the data by the right amount for how noisy it is: a clean dataset is barely smoothed, while a noisy
     * one gets a wide span. The data is returned as is if there are fewer than 3 values or no smoothing is requested.
     * @param data the values, in ascending order of x
     * @param options how the amount of smoothing is chosen
     * @param smoothingStrength the strength from 0 (none) to 1 (most), where the default is the automatic amount
     * @param x the x value (ex. time or distance), in the units of the span distances in the options
     * @param y the y value to smooth, null if it is unknown (it is skipped when estimating the noise)
     * @param merge combines the value with its smoothed y
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
        val span = getAutomaticSpanDistance(xs, ys, options, smoothingStrength)
        val smoothness = (options.spanFraction * getSmoothingFactor(smoothingStrength, options)).coerceAtMost(1f)
        return smooth(data, smoothness, span, { index, _ -> Vector2(xs[index], ys[index] ?: 0f) }) { value, smoothed ->
            merge(value, smoothed.y)
        }
    }

    /**
     * Gets the span distance (in the units of the x values) to use as the maximumSpanDistance when smoothing.
     * It is the narrowest span which reduces the estimated noise of the data to a target, so a clean
     * dataset is barely smoothed while a noisy one gets a wide span.
     * @param xs the x values in ascending order
     * @param ys the y values, a null value is skipped when estimating the noise
     * @param options how the span is chosen
     * @param smoothingStrength the strength from 0 (least) to 1 (most), where the default is the automatic amount
     */
    private fun getAutomaticSpanDistance(
        xs: List<Float>,
        ys: List<Float?>,
        options: AutomaticSmoothingOptions,
        smoothingStrength: Float = DEFAULT_AUTOMATIC_SMOOTHING_STRENGTH
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

    // The default smoothing is a factor of 1, and each end of the range is the max factor more or less smoothing
    private fun getSmoothingFactor(smoothing: Float, options: AutomaticSmoothingOptions): Float {
        return options.maxSmoothingFactor.pow(2 * smoothing.coerceAtMost(1f) - 1)
    }

    const val DEFAULT_AUTOMATIC_SMOOTHING_STRENGTH = 0.5f
    const val DEFAULT_MAX_SMOOTHING_FACTOR = 4f
    const val DEFAULT_SPAN_FRACTION = 0.25f
}
