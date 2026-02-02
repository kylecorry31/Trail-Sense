package com.kylecorry.trail_sense.shared.andromeda_temp

import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import kotlin.math.ceil
import kotlin.math.floor

fun CoordinateBounds.grid(resolution: Double): List<Coordinate> {
    val latitudes = Interpolation.getMultiplesBetween(
        south - resolution,
        north + resolution,
        resolution
    )

    val longitudes = Interpolation.getMultiplesBetween(
        west - resolution,
        (if (west < east) east else east + 360) + resolution,
        resolution
    )

    val points = mutableListOf<Coordinate>()
    for (lat in latitudes) {
        for (lon in longitudes) {
            points.add(Coordinate(lat, lon))
        }
    }
    return points
}

fun Interpolation.getMultiplesBetween2(
    start: Double,
    end: Double,
    multiple: Double
): DoubleArray {
    val startMultiple = ceil(start / multiple).toInt()
    val endMultiple = floor(end / multiple).toInt()
    val size = endMultiple - startMultiple + 1
    if (size <= 0) return DoubleArray(0)

    val result = DoubleArray(size)
    var value = startMultiple * multiple
    for (i in 0 until size) {
        result[i] = value
        value += multiple
    }
    return result
}