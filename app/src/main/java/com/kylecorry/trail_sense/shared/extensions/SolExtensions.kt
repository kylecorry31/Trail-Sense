package com.kylecorry.trail_sense.shared.extensions

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