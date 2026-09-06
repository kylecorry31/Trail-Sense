package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.trail_sense.shared.sensors.gps.modules.AccuracyFilterGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.modules.BadReadingFilterGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.modules.CacheGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.modules.KalmanGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.modules.MeanSeaLevelGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.modules.SatelliteFixFilterGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.modules.SpeedGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.modules.TimeoutGPSModule
import java.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class SharedGPSPipeline(private val factory: (suspend (() -> Boolean) -> Unit) -> GPSPipeline) {
    private var pipeline = factory(::onTimeout)
    private val consumers = mutableMapOf<Any, () -> Unit>()
    private val mutex = Mutex()

    @Volatile
    private var latest = snapshot()

    val reading: ModularGPSData
        get() = latest

    val isTimedOut: Boolean
        get() = latest.isTimedOut

    suspend fun start(consumer: Any, notifyTimeout: () -> Unit = {}) = mutex.withLock {
        if (consumers.putIfAbsent(consumer, notifyTimeout) == null && consumers.size == 1) {
            pipeline.start()
            latest = snapshot()
        }
    }

    suspend fun stop(consumer: Any) = mutex.withLock {
        if (consumers.remove(consumer) != null && consumers.isEmpty()) {
            pipeline.stop()
        }
    }

    suspend fun update(gps: ModularGPSData): ModularGPSData = mutex.withLock {
        if (pipeline.ensureInitialized()) latest = snapshot()
        // A slower subscription may deliver a fix already superseded by another consumer.
        // Do not rewind shared state, even when optional rejection is disabled.
        val previousTime = pipeline.reading.time
        if (gps.time >= previousTime || previousTime > Instant.now().plusMillis(500)) {
            if (pipeline.update(gps) != GPSUpdateResult.Rejected) {
                latest = snapshot()
            }
        }
        latest
    }

    suspend fun clearCache(clear: () -> Unit) = mutex.withLock {
        if (consumers.isNotEmpty()) pipeline.stop()
        clear()
        pipeline = factory(::onTimeout)
        pipeline.reinitialize()
        latest = snapshot()
        if (consumers.isNotEmpty()) pipeline.start()
    }

    private suspend fun onTimeout(acceptTimeout: () -> Boolean) {
        val listeners = mutex.withLock {
            if (!acceptTimeout()) return@withLock emptyList()
            pipeline.reading.isTimedOut = true
            latest = snapshot()
            consumers.values.toList()
        }
        listeners.forEach { it() }
    }

    private fun snapshot() = ModularGPSData().also { pipeline.reading.copyInto(it) }

    companion object {
        @Volatile
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

        suspend fun clearSharedCache(clear: () -> Unit) {
            val current = instance
            if (current == null) clear() else current.clearCache(clear)
        }
    }
}
