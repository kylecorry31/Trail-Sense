package com.kylecorry.trail_sense.shared.hooks

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance

internal class DistanceHookTrigger {

    private var lastLocation: Coordinate? = null
    private val lock = Any()
    private var lastReturnValue = false

    fun getValue(location: Coordinate, threshold: Distance, highAccuracy: Boolean = true): Boolean {
        synchronized(lock) {
            if (lastLocation == null) {
                lastLocation = location
                lastReturnValue = !lastReturnValue
                return lastReturnValue
            }

            val distance = location.distanceTo(lastLocation!!, highAccuracy)
            if (distance >= threshold.meters().distance) {
                lastLocation = location
                lastReturnValue = !lastReturnValue
                return lastReturnValue
            }

            return lastReturnValue
        }
    }

}