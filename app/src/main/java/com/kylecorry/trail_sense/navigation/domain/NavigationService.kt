package com.kylecorry.trail_sense.navigation.domain

import android.location.Location
import com.kylecorry.trail_sense.navigation.domain.compass.Bearing
import com.kylecorry.trail_sense.shared.Coordinate

class NavigationService {
    
    fun getNavigationVector(from: Coordinate, to: Coordinate): NavigationVector {
        val results = FloatArray(3)
        Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, results)
        return NavigationVector(Bearing(results[1]), results[0])
    }

    fun getNextDestination(from: Coordinate, path: Path): Coordinate {
        val closest = path.wayPoints.minBy { from.distanceTo(it) } ?: return from
        val closestIdx = path.wayPoints.indexOf(closest)

        val arrivedDist = 5f

        if (closestIdx >= path.wayPoints.lastIndex) {
            return closest
        }

        val next = path.wayPoints[closestIdx + 1]


        if (from.distanceTo(closest) <= arrivedDist || from.distanceTo(next) <= closest.distanceTo(next)){
            return next
        }

        return closest
    }

}