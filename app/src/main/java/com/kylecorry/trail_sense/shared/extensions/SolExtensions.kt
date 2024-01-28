package com.kylecorry.trail_sense.shared.extensions

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.SolMath.roundNearest
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geofence

fun Size.toAndroidSize(): android.util.Size {
    return android.util.Size(width.toInt(), height.toInt())
}

fun CoordinateBounds.Companion.from(geofences: List<Geofence>): CoordinateBounds {
    val bounds = geofences.map { from(it) }
    val corners = bounds.flatMap {
        listOf(
            it.northEast,
            it.northWest,
            it.southEast,
            it.southWest
        )
    }

    return from(corners)
}

/**
 * Returns the values between min and max, inclusive, that are divisible by divisor
 * @param min The minimum value
 * @param max The maximum value
 * @param divisor The divisor
 * @return The values between min and max, inclusive, that are divisible by divisor
 */
fun getValuesBetween(min: Float, max: Float, divisor: Float): List<Float> {
    val values = mutableListOf<Float>()
    val start = min.roundNearest(divisor)
    var i = start
    while (i <= max) {
        if (i >= min) {
            values.add(i)
        }
        i += divisor
    }
    return values
}

fun <T : Comparable<T>> List<T>.range(): Range<T>? {
    val start = minOrNull() ?: return null
    val end = maxOrNull() ?: return null
    return Range(start, end)
}