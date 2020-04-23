package com.kylecorry.trail_sense.navigation.domain

import com.kylecorry.trail_sense.navigation.domain.compass.Bearing
import com.kylecorry.trail_sense.shared.Coordinate

class NavigationService {

    fun navigate(from: Coordinate, to: Coordinate): NavigationVector {
        val direction = LocationMath.getBearing(from, to)
        val distance = LocationMath.getDistance(from, to)
        return NavigationVector(Bearing(direction), distance)
    }

}