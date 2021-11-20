package com.kylecorry.trail_sense.shared

import android.graphics.Path
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.MainActivity
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint

fun Fragment.requireMainActivity(): MainActivity {
    return requireActivity() as MainActivity
}

fun Fragment.requireBottomNavigation(): BottomNavigationView {
    return requireActivity().findViewById(R.id.bottom_navigation)
}

fun List<Coordinate>.toCanvasPath(
    path: Path = Path(),
    toPixelCoordinate: (coordinate: Coordinate) -> PixelCoordinate
): Path {
    val pixelWaypoints = map {
        toPixelCoordinate(it)
    }
    for (i in 1 until pixelWaypoints.size) {
        if (i == 1) {
            val start = pixelWaypoints[0]
            path.moveTo(start.x, start.y)
        }

        val end = pixelWaypoints[i]
        path.lineTo(end.x, end.y)
    }
    return path
}

fun IGPS.getPathPoint(pathId: Long): PathPoint {
    return PathPoint(
        -1,
        pathId,
        location,
        altitude,
        time
    )
}

fun CoordinateBounds.intersects(other: CoordinateBounds): Boolean {
    val inOther =
        other.contains(northEast) || other.contains(northWest) || other.contains(southEast) || other.contains(
            southWest
        )

    val otherIn =
        contains(other.northEast) || contains(other.northWest) || contains(other.southEast) || contains(
            other.southWest
        )

    return inOther || otherIn
}

fun CoordinateBounds.Companion.from(geofence: Geofence): CoordinateBounds {
    val north = geofence.center.plus(geofence.radius, Bearing.from(CompassDirection.North)).latitude
    val south = geofence.center.plus(geofence.radius, Bearing.from(CompassDirection.South)).latitude
    val east = geofence.center.plus(geofence.radius, Bearing.from(CompassDirection.East)).longitude
    val west = geofence.center.plus(geofence.radius, Bearing.from(CompassDirection.West)).longitude

    return CoordinateBounds(north, east, south, west)
}