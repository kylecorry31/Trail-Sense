package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.sol.units.Coordinate
import java.time.Instant

/** Callers must serialize lifecycle and update calls, including across suspension points. */
class GPSPipeline(
    private val modules: List<GPSModule>,
) {
    private val data = ModularGPSData(time = Instant.EPOCH)
    private val candidate = ModularGPSData()
    val reading: ModularGPSData
        get() = data

    private var initialized = false

    internal suspend fun ensureInitialized(): Boolean {
        return if (!initialized) reinitialize() else false
    }

    suspend fun reinitialize(): Boolean {
        var changed = false
        modules.forEach {
            if (it.initialize(data)) {
                changed = true
            }
        }
        initialized = true
        return changed
    }

    suspend fun start() {
        ensureInitialized()
        modules.forEach { it.start(data) }
    }

    suspend fun stop() {
        modules.forEach { it.stop(data) }
    }

    suspend fun update(gps: ModularGPSData): GPSUpdateResult {
        ensureInitialized()
        candidate.populateFromGPS(gps)

        if (modules.any { !it.update(data, candidate) }) {
            return GPSUpdateResult.Rejected
        }

        if (candidate.location == Coordinate.zero) {
            return GPSUpdateResult.Rejected
        }

        val isSameReading = candidate.id == data.id
        candidate.copyInto(data)
        return if (isSameReading) GPSUpdateResult.SameFixUpdated else GPSUpdateResult.NewFixAccepted
    }
}
