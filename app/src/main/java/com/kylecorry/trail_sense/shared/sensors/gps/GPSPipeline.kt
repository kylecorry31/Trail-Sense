package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.logging.Logger
import java.time.Instant
import kotlinx.coroutines.CancellationException

/** Callers must serialize lifecycle and update calls, including across suspension points. */
class GPSPipeline(
    private val modules: List<GPSModule>,
    private val logger: () -> Logger = { getAppService() }
) {
    private val data = ModularGPSData(eventTime = Instant.EPOCH)
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

    @Suppress("TooGenericExceptionCaught")
    suspend fun update(gps: ModularGPSData): GPSUpdateResult {
        ensureInitialized()
        candidate.populateFromGPS(gps)

        if (modules.any { module ->
                try {
                    !module.update(data, candidate)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger().error(TAG, "${module.javaClass.name} failed to update GPS", e)
                    false
                }
            }) {
            return GPSUpdateResult.Rejected
        }

        if (candidate.location == Coordinate.zero) {
            return GPSUpdateResult.Rejected
        }

        val isSameReading = candidate.id == data.id
        candidate.copyInto(data)
        return if (isSameReading) GPSUpdateResult.SameFixUpdated else GPSUpdateResult.NewFixAccepted
    }

    companion object {
        private const val TAG = "GPSPipeline"
    }
}
