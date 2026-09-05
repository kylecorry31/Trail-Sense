package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.andromeda.sense.location.ISatelliteGPS
import java.time.Instant

internal class SharedGPSPipeline(private val factory: (() -> Unit, () -> Boolean) -> GPSPipeline) {
    private var pipeline = factory(::onTimeout, ::retryUpdate)
    private var lastSource: ModularGPSData? = null
    private val consumers = mutableMapOf<Any, () -> Unit>()

    @Volatile
    private var latest = snapshot()

    val reading: ModularGPSData
        get() = latest

    val isTimedOut: Boolean
        get() = latest.isTimedOut

    @Synchronized
    fun start(consumer: Any, notifyTimeout: () -> Unit = {}) {
        if (consumers.putIfAbsent(consumer, notifyTimeout) == null && consumers.size == 1) {
            pipeline.start()
        }
    }

    @Synchronized
    fun stop(consumer: Any) {
        if (consumers.remove(consumer) != null && consumers.isEmpty()) {
            pipeline.stop()
        }
    }

    @Synchronized
    fun update(gps: ISatelliteGPS): ModularGPSData {
        // A slower subscription may deliver a fix already superseded by another consumer.
        // Do not rewind shared state, even when optional rejection is disabled.
        val previousTime = pipeline.reading.time
        val futureThreshold = Instant.now().plusMillis(500)
        if (gps.time >= previousTime || previousTime > futureThreshold) {
            val lastSourceTime = lastSource?.time
            // Preserve newer rejected fixes for retry when an older subscription calls back.
            if (lastSourceTime == null || gps.time >= lastSourceTime || lastSourceTime > futureThreshold) {
                lastSource = ModularGPSData().also { it.populateFromGPS(gps) }
            }
            if (pipeline.update(gps) != GPSUpdateResult.Rejected) {
                latest = snapshot()
            }
        }
        return latest
    }

    @Synchronized
    fun clearCache(clear: () -> Unit) {
        if (consumers.isNotEmpty()) pipeline.stop()
        clear()
        lastSource = null
        pipeline = factory(::onTimeout, ::retryUpdate)
        latest = snapshot()
        if (consumers.isNotEmpty()) pipeline.start()
    }

    @Synchronized
    private fun retryUpdate(): Boolean {
        val source = lastSource ?: return false
        val result = pipeline.update(source)
        if (result != GPSUpdateResult.Rejected) {
            latest = snapshot()
        }
        return result == GPSUpdateResult.NewFixAccepted
    }

    private fun onTimeout() {
        val listeners = synchronized(this) {
            latest = snapshot()
            consumers.values.toList()
        }
        listeners.forEach { it() }
    }

    private fun snapshot() = ModularGPSData().also { pipeline.reading.copyInto(it) }

    companion object {
        private var instance: SharedGPSPipeline? = null

        @Synchronized
        fun getInstance(): SharedGPSPipeline {
            return instance ?: SharedGPSPipeline { notifyTimeout, retryUpdate ->
                GPSPipeline(
                    listOf(
                        BadReadingRejectionGPSModule(),
                        AccuracyRequirementGPSModule(),
                        MeanSeaLevelGPSModule(),
                        SpeedGPSModule(),
                        TimeoutGPSModule(notifyTimeout, retryUpdate),
                        KalmanGPSModule(),
                        CacheGPSModule()
                    )
                )
            }.also { instance = it }
        }

        @Synchronized
        fun clearSharedCache(clear: () -> Unit) {
            val current = instance
            if (current == null) clear() else current.clearCache(clear)
        }
    }
}
