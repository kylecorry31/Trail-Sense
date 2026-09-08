package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.trail_sense.shared.sensors.gps.modules.AccuracyFilterGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.modules.BadReadingFilterGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.modules.CacheGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.modules.KalmanGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.modules.MeanSeaLevelGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.modules.SameFixGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.modules.SpeedGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.modules.TimeoutGPSModule
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class SharedGPSPipeline(private val factory: (suspend (() -> Boolean) -> Unit) -> GPSPipeline) {
    private var pipeline = factory(::onTimeout)
    private val consumers = mutableMapOf<Any, () -> Unit>()
    private val mutex = Mutex()

    @Volatile
    private var latest: ModularGPSData

    init {
        // Some consumers rely on the hasValidReading property being correct, which depends on the pipeline being initialized
        // This is fine for now since reinitializing the pipeline is just a cache read right now
        runBlocking { pipeline.reinitialize() }
        latest = snapshot()
    }

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

    suspend fun update(gps: ModularGPSData): ModularGPSData? = mutex.withLock {
        if (pipeline.update(gps) == GPSUpdateResult.Rejected) return@withLock null
        latest = snapshot()
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
                        SameFixGPSModule(),
                        BadReadingFilterGPSModule(),
                        MeanSeaLevelGPSModule(),
                        KalmanGPSModule(),
                        AccuracyFilterGPSModule(),
                        SpeedGPSModule(),
                        TimeoutGPSModule(notifyTimeout),
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
