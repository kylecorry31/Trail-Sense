package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.luna.time.ITimer
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.logging.Logger
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

/**
 * Retries the current GPS reading and notifies listeners when accepted updates stop arriving.
 * Run after modules which can reject a reading so rejected readings do not reset the timeout.
 */
class TimeoutGPSModule(
    private val updateGPSData: () -> GPSUpdateResult,
    private val notifyListeners: () -> Unit,
    private val logger: Logger = getAppService(),
    timerFactory: (() -> Unit) -> ITimer = { action -> CoroutineTimer { action() } }
) : GPSModule {

    private val diagnosticId = nextDiagnosticId.getAndIncrement()
    private val timeout = timerFactory { onTimeout() }
    private lateinit var data: ModularGPSData

    @Volatile
    private var isStarted = false

    override fun start(data: ModularGPSData) {
        this.data = data
        isStarted = true
        timeout.once(TIMEOUT_DURATION)
    }

    override fun stop(data: ModularGPSData) {
        isStarted = false
        timeout.stop()
    }

    override fun update(previousData: ModularGPSData, newData: ModularGPSData): Boolean {
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

    private fun onTimeout() {
        if (!isStarted) {
            return
        }

        logger.debug(TAG, "[$diagnosticId] Timed out after ${TIMEOUT_DURATION.seconds}s")

        // Rejected readings and secondary-field updates both leave us without a new fix.
        if (updateGPSData() != GPSUpdateResult.NewFixAccepted) {
            logger.debug(
                TAG,
                "[$diagnosticId] No valid reading to update to, keeping a reading from " +
                        "${Duration.between(data.time, Instant.now()).toMillis()}ms ago"
            )
            data.isTimedOut = true
            timeout.once(TIMEOUT_DURATION)
        }

        notifyListeners()
    }

    companion object {
        private val TIMEOUT_DURATION = Duration.ofSeconds(10)
        private const val TAG = "TimeoutGPSModule"

        // Distinguishes concurrent GPS instances in the logs.
        private val nextDiagnosticId = AtomicInteger(1)
    }
}
