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
 * Calls onTimeout when accepted updates stop arriving.
 * Run after modules which can reject a reading so rejected readings do not reset the timeout.
 */
class TimeoutGPSModule(
    private val onTimeout: suspend (() -> Boolean) -> Unit,
    private val logger: Logger = getAppService(),
    private val timerFactory: (suspend () -> Unit) -> ITimer = { action -> CoroutineTimer { action() } }
) : GPSModule {

    private var timeout: ITimer? = null
    private var timeoutToken: Any? = null
    private lateinit var data: ModularGPSData

    private var isStarted = false

    override suspend fun start(data: ModularGPSData) {
        this.data = data
        isStarted = true
        scheduleTimeout()
    }

    override suspend fun stop(data: ModularGPSData) {
        isStarted = false
        timeoutToken = null
        timeout?.stop()
        timeout = null
    }

    override suspend fun update(previousData: ModularGPSData, newData: ModularGPSData): Boolean {
        // Secondary-field updates are not new fixes and must not postpone the timeout.
        if (newData.time.toEpochMilli() == previousData.time.toEpochMilli()) {
            newData.isTimedOut = previousData.isTimedOut
            return true
        }
        if (isStarted) {
            scheduleTimeout()
        }
        newData.isTimedOut = false
        return true
    }

    private fun scheduleTimeout() {
        val token = Any()
        timeoutToken = token
        timeout?.stop()
        timeout = timerFactory {
            onTimeout { acceptTimeout(token) }
        }.also { it.once(TIMEOUT_DURATION) }
    }

    private fun acceptTimeout(token: Any): Boolean {
        // A queued callback may belong to a superseded fix or a previous start/stop session.
        if (!isStarted || token !== timeoutToken) {
            return false
        }
        timeoutToken = null

        logger.debug(TAG, "Timed out after ${TIMEOUT_DURATION.seconds}s")
        logger.debug(
            TAG,
            "Keeping a reading from ${Duration.between(data.time, Instant.now()).toMillis()}ms ago"
        )
        return true
    }

    companion object {
        private val TIMEOUT_DURATION = SensorService.GPS_READ_TIMEOUT
        private const val TAG = "TimeoutGPSModule"
    }
}
