package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.andromeda.sense.location.ISatelliteGPS
import com.kylecorry.sol.units.Coordinate

internal class GPSPipelineConsumer(
    private val pipeline: SharedGPSPipeline,
    private val notifyTimeout: () -> Unit
) {
    private val delivered = ModularGPSData().also { pipeline.reading.copyInto(it) }

    val reading: ModularGPSData
        get() = pipeline.reading

    fun start() {
        pipeline.start(this, notifyTimeout)
    }

    fun stop() {
        pipeline.stop(this)
    }

    fun update(gps: ISatelliteGPS): Boolean {
        val latest = pipeline.update(gps)
        if (latest.location == Coordinate.zero) return false
        val isNewToConsumer = latest.time.toEpochMilli() != delivered.time.toEpochMilli()
        latest.copyInto(delivered)
        return isNewToConsumer
    }
}
