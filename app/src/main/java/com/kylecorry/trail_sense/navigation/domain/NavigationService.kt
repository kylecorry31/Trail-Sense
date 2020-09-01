package com.kylecorry.trail_sense.navigation.domain

import android.location.Location
import com.kylecorry.trail_sense.navigation.domain.compass.Bearing
import com.kylecorry.trail_sense.navigation.domain.compass.DeclinationCalculator
import com.kylecorry.trail_sense.shared.domain.Coordinate
import java.time.Duration
import kotlin.math.roundToLong

class NavigationService {

    fun navigate(from: Coordinate, to: Coordinate, declination: Float, usingTrueNorth: Boolean = true): NavigationVector {
        val results = FloatArray(3)
        Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, results)

        val declinationAdjustment = if (usingTrueNorth){
            0f
        } else {
            -declination
        }

        return NavigationVector(Bearing(results[1]).withDeclination(declinationAdjustment), results[0])
    }

    fun navigate(from: Position, to: Beacon, declination: Float, usingTrueNorth: Boolean = true): NavigationVector {
        val originalVector = navigate(from.location, to.coordinate, declination, usingTrueNorth)
        val altitudeChange = if (to.elevation != null) to.elevation - from.altitude else null
        return originalVector.copy(altitudeChange = altitudeChange)
    }

    fun eta(distance: Float, speed: Float): Duration? {
        if (speed == 0f){
            return null
        }

        return Duration.ofSeconds((distance / speed).roundToLong())
    }

}