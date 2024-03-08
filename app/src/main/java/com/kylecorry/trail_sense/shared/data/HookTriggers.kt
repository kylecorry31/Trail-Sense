package com.kylecorry.trail_sense.shared.data

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import java.time.Duration
import java.time.Instant

class HookTriggers {
    private val locationTriggers = mutableMapOf<String, LocationHookTrigger>()
    private val locationLock = Any()

    private val timeTriggers = mutableMapOf<String, TimeHookTrigger>()
    private val timeLock = Any()

    fun location(name: String, location: Coordinate, threshold: Distance): Boolean {
        val conditional = synchronized(locationLock) {
            locationTriggers.getOrPut(name) { LocationHookTrigger() }
        }
        return conditional.getValue(location, threshold)
    }

    fun time(name: String, threshold: Duration): Boolean {
        val conditional = synchronized(timeLock) {
            timeTriggers.getOrPut(name) { TimeHookTrigger() }
        }
        return conditional.getValue(Instant.now(), threshold)
    }

    private class LocationHookTrigger {

        private var lastLocation: Coordinate? = null
        private val lock = Any()
        private var lastReturnValue = false

        fun getValue(location: Coordinate, threshold: Distance): Boolean {
            synchronized(lock) {
                if (lastLocation == null) {
                    lastLocation = location
                    lastReturnValue = !lastReturnValue
                    return lastReturnValue
                }

                val distance = location.distanceTo(lastLocation!!)
                if (distance >= threshold.meters().distance) {
                    lastLocation = location
                    lastReturnValue = !lastReturnValue
                    return lastReturnValue
                }

                return lastReturnValue
            }
        }

    }

    private class TimeHookTrigger {

        private var lastTime: Instant? = null
        private val lock = Any()
        private var lastReturnValue = false

        fun getValue(time: Instant, threshold: Duration): Boolean {
            synchronized(lock) {
                if (lastTime == null) {
                    lastTime = time
                    lastReturnValue = !lastReturnValue
                    return lastReturnValue
                }

                if (Duration.between(lastTime, time) >= threshold) {
                    lastTime = time
                    lastReturnValue = !lastReturnValue
                    return lastReturnValue
                }

                return lastReturnValue
            }
        }
    }
}