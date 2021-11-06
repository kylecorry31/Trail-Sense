package com.kylecorry.trail_sense.shared

import android.graphics.Path
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.MainActivity
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.paths.PathPoint

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
        val start = pixelWaypoints[i - 1]
        val end = pixelWaypoints[i]
        path.moveTo(start.x, start.y)
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