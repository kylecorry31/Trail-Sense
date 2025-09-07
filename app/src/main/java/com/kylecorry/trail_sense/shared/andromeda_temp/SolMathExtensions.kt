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

data class IsolineSegment<T>(
    val start: T,
    val end: T
)

fun <T> Interpolation.getIsolineCalculators(
    grid: List<List<Pair<T, Float>>>,
    threshold: Float,
    interpolator: (percent: Float, a: T, b: T) -> T
): List<() -> List<IsolineSegment<T>>> {
    return MarchingSquares.getIsolineCalculators(
        grid,
        threshold,
        interpolator
    )
}