package com.kylecorry.trail_sense.navigation.domain

import android.location.Location
import com.kylecorry.trail_sense.navigation.domain.compass.Bearing
import com.kylecorry.trail_sense.navigation.domain.compass.DeclinationCalculator
import com.kylecorry.trail_sense.shared.domain.Coordinate

class NavigationService {

    private val declinationCalculator = DeclinationCalculator()

    fun navigate(from: Coordinate, to: Coordinate, currentAltitude: Float = 0f, usingTrueNorth: Boolean = true): NavigationVector {
        val results = FloatArray(3)
        Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, results)

        val declinationAdjustment = if (usingTrueNorth){
            0f
        } else {
            -getDeclination(from, currentAltitude)
        }

        return NavigationVector(Bearing(results[1]).withDeclination(declinationAdjustment), results[0])
    }

    fun getDeclination(location: Coordinate, altitude: Float): Float {
        return declinationCalculator.calculate(location, altitude)
    }

}