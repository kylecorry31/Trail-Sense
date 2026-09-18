package com.kylecorry.trail_sense.tools.paths.domain

import com.kylecorry.sol.math.filters.RDPFilter
import com.kylecorry.sol.science.geography.Geography
import kotlin.math.absoluteValue

object PathSimplifier {
    fun simplify(points: List<PathPoint>, quality: PathSimplificationQuality): List<PathPoint> {
        val epsilon = when (quality) {
            PathSimplificationQuality.Low -> 8f
            PathSimplificationQuality.Medium -> 4f
            PathSimplificationQuality.High -> 2f
        }
        return RDPFilter<PathPoint>(epsilon) { point, start, end ->
            Geography.getCrossTrackDistance(
                point.coordinate,
                start.coordinate,
                end.coordinate
            ).value.absoluteValue
        }.filter(points)
    }
}
