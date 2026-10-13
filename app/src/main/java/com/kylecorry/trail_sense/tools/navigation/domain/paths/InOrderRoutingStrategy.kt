package com.kylecorry.trail_sense.tools.navigation.domain.paths

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint

internal class InOrderRoutingStrategy(private val reversed: Boolean) : RoutingStrategy {
    override fun buildRoute(points: List<PathPoint>, location: Coordinate): List<PathPoint> {
        val path = if (reversed) points.reversed() else points
        val pathGeometry = RouteGeometry(path.map { it.coordinate })
        if (pathGeometry.length == 0f) return listOf(points.first())
        val snap = pathGeometry.candidateSnaps(location).first()
        return listOf(PathInterpolationUtils.pointAt(path, pathGeometry, snap)) + path.drop(snap.segment + 1)
    }
}
