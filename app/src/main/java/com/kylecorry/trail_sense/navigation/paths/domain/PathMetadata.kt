package com.kylecorry.trail_sense.navigation.paths.domain

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Distance
import java.time.Instant

data class PathMetadata(
    val distance: Distance,
    val waypoints: Int,
    val duration: Range<Instant>?,
    val bounds: CoordinateBounds
) {
    companion object {
        val empty = PathMetadata(Distance.meters(0f), 0, null, CoordinateBounds.empty)
    }
}