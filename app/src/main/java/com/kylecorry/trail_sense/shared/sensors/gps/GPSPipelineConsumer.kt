package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.sol.units.Coordinate

internal class GPSPipelineConsumer(
    private val pipeline: SharedGPSPipeline,
    private val notifyTimeout: () -> Unit
) {
    private val delivered = ModularGPSData().also { pipeline.reading.copyInto(it) }

    val reading: ModularGPSData
        get() = pipeline.reading

    suspend fun start() {
        pipeline.start(this, notifyTimeout)
    }

    suspend fun stop() {
        pipeline.stop(this)
    }

    suspend fun update(gps: ModularGPSData): Boolean {
        val latest = pipeline.update(gps)
        if (latest.location == Coordinate.zero) return false
        val isNewToConsumer = latest.time.toEpochMilli() != delivered.time.toEpochMilli()
        latest.copyInto(delivered)
        return isNewToConsumer
    }
}
