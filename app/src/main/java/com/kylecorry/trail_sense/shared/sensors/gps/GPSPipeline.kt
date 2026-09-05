package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.andromeda.sense.location.ISatelliteGPS
import com.kylecorry.sol.units.Coordinate

class GPSPipeline(
    modules: List<GPSModule>,
    private val cache: CacheGPSModule
) {
    private val data = ModularGPSData()
    private val candidate = ModularGPSData()
    private val modules = modules + cache

    val reading: ISatelliteGPS
        get() = data

    var hadValidReading = false
        private set

    init {
        cache.restore(data)
    }

    @Synchronized
    fun restoreNewerCachedReading(): Boolean {
        if (!cache.hasNewerReading(data)) {
            return false
        }
        cache.restore(data)
        return true
    }

    fun start() {
        modules.forEach { it.start(data) }
    }

    fun stop() {
        modules.forEach { it.stop(data) }
    }

    @Synchronized
    fun update(gps: ISatelliteGPS): GPSUpdateResult {
        candidate.populateFromGPS(gps)

        if (modules.any { !it.update(data, candidate) }) {
            return GPSUpdateResult.Rejected
        }

        // A cached fix or secondary-field update can have the same timestamp.
        val isSameReading = candidate.time == data.time
        candidate.copyInto(data)

        return if (data.location != Coordinate.zero) {
            hadValidReading = true
            if (isSameReading) GPSUpdateResult.SameFixUpdated else GPSUpdateResult.NewFixAccepted
        } else {
            GPSUpdateResult.Rejected
        }
    }
}
