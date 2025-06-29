package com.kylecorry.trail_sense.shared.andromeda_temp

import com.kylecorry.sol.math.interpolation.Interpolation
import kotlin.math.ceil
import kotlin.math.floor

fun Float.floorToInt(): Int {
    return floor(this).toInt()
}

fun Float.ceilToInt(): Int {
    return ceil(this).toInt()
}

fun Interpolation.getMultiplesBetween(
    start: Double,
    end: Double,
    multiple: Double
): List<Double> {
    val startMultiple = ceil(start / multiple)
    val endMultiple = floor(end / multiple)
    return (startMultiple.toInt()..endMultiple.toInt()).map { it * multiple }
}

fun Interpolation.getMultiplesBetween(
    start: Float,
    end: Float,
    multiple: Float
): List<Float> {
    val startMultiple = ceil(start / multiple)
    val endMultiple = floor(end / multiple)
    return (startMultiple.toInt()..endMultiple.toInt()).map { it * multiple }
}

data class IsolineSegment<T>(
    val start: T,
    val end: T,
    val upDirection: Float
)

fun <T> Interpolation.getIsoline(
    grid: List<List<Pair<T, Float>>>,
    threshold: Float,
    interpolator: (percent: Float, a: T, b: T) -> T,
    angleCalculator: (T, T) -> Float = { a, b -> 0f }
): List<IsolineSegment<T>> {
    return MarchingSquares.getIsoline(
        grid,
        threshold,
        interpolator,
        angleCalculator
    )
}

fun <T> Interpolation.getIsolineCalculators(
    grid: List<List<Pair<T, Float>>>,
    threshold: Float,
    interpolator: (percent: Float, a: T, b: T) -> T,
    angleCalculator: (T, T) -> Float = { a, b -> 0f }
): List<() -> List<IsolineSegment<T>>> {
    return MarchingSquares.getIsolineCalculators(
        grid,
        threshold,
        interpolator,
        angleCalculator
    )
}