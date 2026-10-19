package com.kylecorry.trail_sense.tools.navigation.domain.paths

import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint

internal object PathInterpolationUtils {
    fun pointAt(points: List<PathPoint>, geometry: RouteGeometry, snap: Snap): PathPoint {
        return points[snap.segment].copy(coordinate = geometry.coordinateOf(snap), elevation = elevationAt(points, snap))
    }

    fun elevationAt(points: List<PathPoint>, snap: Snap): Float? {
        val index = snap.segment
        if (snap.percentAlongSegment == 0f) return points[index].elevation
        if (snap.percentAlongSegment == 1f) return points[index + 1].elevation
        return Interpolation.lerp(
            snap.percentAlongSegment,
            points[index].elevation ?: return null,
            points[index + 1].elevation ?: return null
        )
    }
}
