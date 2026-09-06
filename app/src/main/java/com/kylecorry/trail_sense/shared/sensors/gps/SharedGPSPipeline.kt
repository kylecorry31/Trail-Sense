package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.andromeda.sense.location.ISatelliteGPS
import com.kylecorry.trail_sense.shared.sensors.gps.modules.BadReadingFilterGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.modules.CacheGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.modules.KalmanGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.modules.MeanSeaLevelGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.modules.AccuracyFilterGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.modules.SatelliteFixFilterGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.modules.SpeedGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.modules.TimeoutGPSModule
import java.time.Instant

internal class SharedGPSPipeline(private val factory: (() -> Unit) -> GPSPipeline) {
    private var pipeline = factory(::onTimeout)
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
        if (gps.time >= previousTime || previousTime > Instant.now().plusMillis(500)) {
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
        pipeline = factory(::onTimeout)
        latest = snapshot()
        if (consumers.isNotEmpty()) pipeline.start()
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
            return instance ?: SharedGPSPipeline { notifyTimeout ->
                GPSPipeline(
                    listOf(
                        BadReadingFilterGPSModule(),
                        SatelliteFixFilterGPSModule(),
                        AccuracyFilterGPSModule(),
                        MeanSeaLevelGPSModule(),
                        SpeedGPSModule(),
                        TimeoutGPSModule(notifyTimeout),
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
