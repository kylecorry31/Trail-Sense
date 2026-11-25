package com.kylecorry.trail_sense.tools.paths.domain

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Distance
import java.time.Duration
import java.time.Instant

data class PathMetadata(
    val distance: Distance,
    val waypoints: Int,
    val times: Range<Instant>?,
    val bounds: CoordinateBounds
) {

    fun getDuration(): Duration? {
        val start = times?.start ?: return null
        val end = times.end
        return Duration.between(start, end)
    }

    companion object {
        val empty = PathMetadata(Distance.meters(0f), 0, null, CoordinateBounds.empty)
    }
}
