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
     * @return true if a recent newer startup fix was accepted, the shared fix changed after restarting, or the shared reading is timed out
     */
    suspend fun start(initialReading: ModularGPSData? = null): Boolean {
        val acceptedStartupFix = pipeline.start(this, ::onTimeout, initialReading)
        val hasDeliveredFix = deliveredId != null
        val latest = pipeline.reading
        val hasNewFix = deliver(latest)
        val shouldNotifyFix = hasNewFix && (acceptedStartupFix || hasDeliveredFix)
        return shouldNotifyFix || latest.isTimedOut
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
