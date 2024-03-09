package com.kylecorry.trail_sense.shared.data

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import java.time.Duration
import java.time.Instant

class HookTriggers {
    private val distanceTriggers = mutableMapOf<String, DistanceHookTrigger>()
    private val distanceLock = Any()

    private val frequencyTriggers = mutableMapOf<String, FrequencyHookTrigger>()
    private val frequencyLock = Any()

    fun distance(name: String, location: Coordinate, threshold: Distance): Boolean {
        val conditional = synchronized(distanceLock) {
            distanceTriggers.getOrPut(name) { DistanceHookTrigger() }
        }
        return conditional.getValue(location, threshold)
    }

    fun frequency(name: String, threshold: Duration): Boolean {
        val conditional = synchronized(frequencyLock) {
            frequencyTriggers.getOrPut(name) { FrequencyHookTrigger() }
        }
        return conditional.getValue(Instant.now(), threshold)
    }

    private class DistanceHookTrigger {

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

    private class FrequencyHookTrigger {

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