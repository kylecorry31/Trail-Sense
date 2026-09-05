package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.andromeda.sense.location.ISatelliteGPS
import com.kylecorry.sol.units.Coordinate
import java.time.Instant

class GPSPipeline(
    private val modules: List<GPSModule>,
) {
    private val data = ModularGPSData(time = Instant.EPOCH)
    private val candidate = ModularGPSData()
    val reading: ModularGPSData
        get() = data

    var hadValidReading = false
        private set

    init {
        reinitialize()
    }

    @Synchronized
    fun reinitialize(): Boolean {
        var changed = false
        modules.forEach {
            if (it.initialize(data)) {
                changed = true
            }
        }
        return changed
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
        val isSameReading = candidate.time.toEpochMilli() == data.time.toEpochMilli()
        candidate.copyInto(data)

        return if (data.location != Coordinate.zero) {
            hadValidReading = true
            if (isSameReading) GPSUpdateResult.SameFixUpdated else GPSUpdateResult.NewFixAccepted
        } else {
            GPSUpdateResult.Rejected
        }
    }
}
