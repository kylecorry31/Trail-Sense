package com.kylecorry.trail_sense.shared

import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.sol.math.Vector2
import com.kylecorry.trail_sense.MainActivity
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint

fun Fragment.requireMainActivity(): MainActivity {
    return requireActivity() as MainActivity
}

fun Fragment.requireBottomNavigation(): BottomNavigationView {
    return requireActivity().findViewById(R.id.bottom_navigation)
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

fun PixelCoordinate.toVector2(): Vector2 {
    return Vector2(x, y)
}

fun Vector2.toPixel(): PixelCoordinate {
    return PixelCoordinate(x, y)
}