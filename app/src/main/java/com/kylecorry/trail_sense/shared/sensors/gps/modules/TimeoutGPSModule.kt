package com.kylecorry.trail_sense.shared.sensors.gps.modules

import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.luna.time.ITimer
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.gps.GPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData
import java.time.Duration
import java.time.Instant

/**
 * Marks the GPS as timed out and notifies listeners when accepted updates stop arriving.
 * Run after modules which can reject a reading so rejected readings do not reset the timeout.
 */
class TimeoutGPSModule(
    private val notifyListeners: suspend () -> Unit,
    private val logger: Logger = getAppService(),
    timerFactory: (suspend () -> Unit) -> ITimer = { action -> CoroutineTimer { action() } }
) : GPSModule {

    private val timeout = timerFactory { onTimeout() }
    private lateinit var data: ModularGPSData

    @Volatile
    private var isStarted = false

    override suspend fun start(data: ModularGPSData) {
        this.data = data
        isStarted = true
        timeout.once(TIMEOUT_DURATION)
    }

    override suspend fun stop(data: ModularGPSData) {
        isStarted = false
        timeout.stop()
    }

    override suspend fun update(previousData: ModularGPSData, newData: ModularGPSData): Boolean {
        // Secondary-field updates are not new fixes and must not postpone the timeout.
        if (newData.time.toEpochMilli() == previousData.time.toEpochMilli()) {
            newData.isTimedOut = previousData.isTimedOut
            return true
        }
        if (isStarted) {
            timeout.once(TIMEOUT_DURATION)
        }
        newData.isTimedOut = false
        return true
    }

    private suspend fun onTimeout() {
        if (!isStarted) {
            return
        }

        logger.debug(TAG, "Timed out after ${TIMEOUT_DURATION.seconds}s")

        logger.debug(
            TAG,
            "Keeping a reading from ${Duration.between(data.time, Instant.now()).toMillis()}ms ago"
        )
        data.isTimedOut = true

        notifyListeners()
    }

    companion object {
        private val TIMEOUT_DURATION = SensorService.GPS_READ_TIMEOUT
        private const val TAG = "TimeoutGPSModule"
    }
}
