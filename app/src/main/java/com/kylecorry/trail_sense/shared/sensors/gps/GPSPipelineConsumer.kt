package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.sol.units.Coordinate

internal class GPSPipelineConsumer(
    private val pipeline: SharedGPSPipeline,
    private val notify: () -> Unit
) {
    private var deliveredId: Long? = null

    val reading: ModularGPSData
        get() = pipeline.reading

    /**
     * @return true if the shared fix has not been delivered to this consumer after restarting
     */
    suspend fun start(): Boolean {
        pipeline.start(this, ::onTimeout)
        val hasDeliveredFix = deliveredId != null
        return deliver(pipeline.reading) && hasDeliveredFix
    }

    suspend fun stop() {
        pipeline.stop(this)
    }

    suspend fun update(gps: ModularGPSData): Boolean {
        return deliver(pipeline.update(gps) ?: return false)
    }

    private fun onTimeout() {
        val latest = pipeline.reading
        if (deliver(latest) || latest.isTimedOut) {
            notify()
        }
    }

    @Synchronized
    private fun deliver(latest: ModularGPSData): Boolean {
        if (latest.location == Coordinate.zero) return false
        val id = latest.id
        val isNewToConsumer = id != deliveredId
        deliveredId = id
        return isNewToConsumer
    }
}
