package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.sol.units.Coordinate

internal class GPSPipelineConsumer(
    private val pipeline: SharedGPSPipeline,
    private val notifyTimeout: () -> Unit
) {
    private var deliveredTimeMillis: Long? = null

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
        val timeMillis = latest.time.toEpochMilli()
        val isNewToConsumer = timeMillis != deliveredTimeMillis
        deliveredTimeMillis = timeMillis
        return isNewToConsumer
    }
}
