package com.kylecorry.trail_sense.shared.hooks

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
}