package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.andromeda.core.time.SystemTimeProvider
import com.kylecorry.andromeda.core.time.TimeProvider
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.logging.Logger
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
import java.time.Duration

internal class SharedGPSPipeline(
    private val logger: Logger = getAppService(),
    private val timeProvider: TimeProvider = SystemTimeProvider(),
    private val factory: (suspend (() -> Boolean) -> Unit) -> GPSPipeline
) {
    private var pipeline = factory(::onTimeout)
    private val consumers = mutableMapOf<Any, () -> Unit>()
    private val mutex = Mutex()

    @Volatile
    private var latest: ModularGPSData

    private var lastInput: ModularGPSData? = null

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

    suspend fun start(
        consumer: Any,
        notifyTimeout: () -> Unit = {},
        initialReading: ModularGPSData? = null
    ): Boolean = mutex.withLock {
        if (consumers.putIfAbsent(consumer, notifyTimeout) == null && consumers.size == 1) {
            logger.info(TAG, "Started")
            lastInput = null
            pipeline.start()
            latest = snapshot()
        }
        if (initialReading == null) return@withLock false
        val shouldNotify = initialReading.age(timeProvider) in Duration.ZERO..STARTUP_READING_THRESHOLD &&
            initialReading.eventTimeElapsedNanos > latest.eventTimeElapsedNanos
        if (shouldNotify || latest.location == Coordinate.zero) {
            return@withLock updateLocked(initialReading) != null && shouldNotify
        }
        false
    }

    suspend fun stop(consumer: Any) = mutex.withLock {
        if (consumers.remove(consumer) != null && consumers.isEmpty()) {
            logger.info(TAG, "Stopped")
            pipeline.stop()
        }
    }

    suspend fun update(gps: ModularGPSData): ModularGPSData? = mutex.withLock {
        updateLocked(gps)
    }

    private suspend fun updateLocked(gps: ModularGPSData): ModularGPSData? {
        lastInput = ModularGPSData().also { gps.copyInto(it) }
        if (pipeline.update(gps) == GPSUpdateResult.Rejected) return null
        latest = snapshot()
        return latest
    }

    suspend fun clearCache(clear: () -> Unit) = mutex.withLock {
        if (consumers.isNotEmpty()) pipeline.stop()
        clear()
        lastInput = null
        pipeline = factory(::onTimeout)
        pipeline.reinitialize()
        latest = snapshot()
        if (consumers.isNotEmpty()) pipeline.start()
    }

    private suspend fun onTimeout(acceptTimeout: () -> Boolean) {
        val listeners = mutex.withLock {
            if (!acceptTimeout()) return@withLock emptyList()
            pipeline.reading.isTimedOut = true
            // The last input may have been rejected, so give it another chance to become the fix
            lastInput?.let { pipeline.update(it) }
            latest = snapshot()
            consumers.values.toList()
        }
        listeners.forEach { it() }
    }

    private fun snapshot() = ModularGPSData().also { pipeline.reading.copyInto(it) }

    companion object {
        private const val TAG = "SharedGPSPipeline"
        private val STARTUP_READING_THRESHOLD = Duration.ofSeconds(5)

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
