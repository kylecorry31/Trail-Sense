package com.kylecorry.trail_sense.tools.paths.infrastructure.services

import android.content.Context
import android.content.Intent
import com.kylecorry.andromeda.background.IAlwaysOnTaskScheduler
import com.kylecorry.andromeda.background.TaskSchedulerFactory
import com.kylecorry.andromeda.background.services.ForegroundInfo
import com.kylecorry.andromeda.background.services.IntervalService
import com.kylecorry.andromeda.sense.location.GPS
import com.kylecorry.andromeda.sense.location.LocationRequestConfig
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.luna.time.FlowableTimer
import com.kylecorry.luna.time.ITimer
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.tryStartForegroundOrNotify
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.shared.sensors.gps.GPSSource
import com.kylecorry.trail_sense.shared.sensors.gps.GPSSourceSelector
import com.kylecorry.trail_sense.tools.paths.PathsToolRegistration
import com.kylecorry.trail_sense.tools.paths.infrastructure.alerts.BacktrackAlerter
import com.kylecorry.trail_sense.tools.paths.infrastructure.commands.BacktrackCommand
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.sync.Mutex
import java.time.Duration

class BacktrackService :
    IntervalService(wakelockDuration = Duration.ofSeconds(60), useOneTimeWorkers = true) {
    private val prefs by lazy { UserPreferences(applicationContext) }

    override val uniqueId: Int
        get() = 7238542

    override val holdWakelockWhenBelowThreshold: Boolean
        get() = prefs.paths.backtrackKeepDeviceAwake

    override fun getNonWorkerTimer(action: suspend () -> Unit): ITimer {
        val canWakeWithLocationUpdates = !prefs.paths.backtrackKeepDeviceAwake &&
                GPSSourceSelector(this).getSource(useCache = false) == GPSSource.Device

        if (!canWakeWithLocationUpdates) {
            getAppService<Logger>().info(TAG, "Using a coroutine timer (keep awake: ${prefs.paths.backtrackKeepDeviceAwake})")
            return CoroutineTimer { action() }
        }

        getAppService<Logger>().info(TAG, "Using location updates as the timer")

        return FlowableTimer({ periodMillis ->
            // This intentionally does not use the CustomGPS because it only needs to use the GPS as a wakeup source
            // This has the side effect of warming up the GPS for backtrack
            GPS(
                this,
                LocationRequestConfig(
                    frequency = Duration.ofMillis(periodMillis),
                    powerUsage = prefs.paths.backtrackGPSPowerUsage
                )
            )
        }, action = action)
    }

    override fun getForegroundInfo(): ForegroundInfo {
        val units = prefs.baseDistanceUnits
        return ForegroundInfo(
            BacktrackAlerter.NOTIFICATION_ID,
            BacktrackAlerter.getNotification(this, Distance.from(0f, units))
        )
    }


    override val period: Duration
        get() = prefs.paths.backtrackRecordFrequency

    private val recordLock = Mutex()

    override suspend fun doWork() {
        if (!recordLock.tryLock()) {
            return
        }

        try {
            BacktrackCommand(this).execute()
        } finally {
            recordLock.unlock()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        getAppService<Logger>().info(
            TAG,
            "Started (period: $period, keep awake: ${prefs.paths.backtrackKeepDeviceAwake}, restarted by system: ${intent == null})"
        )
        isRunning = true
        return tryStartForegroundOrNotify {
            super.onStartCommand(intent, flags, startId)
        }
    }

    override fun onDestroy() {
        getAppService<Logger>().info(TAG, "Stopped")
        isRunning = false
        stopService(true)
        super.onDestroy()
    }

    companion object {
        const val FOREGROUND_CHANNEL_ID = "Backtrack"
        private const val TAG = "BacktrackService"

        var isRunning = false
            private set(value) {
                field = value
                Tools.broadcast(PathsToolRegistration.BROADCAST_BACKTRACK_STATE_CHANGED)
            }

        fun start(context: Context) {
            scheduler(context).start()
        }

        fun stop(context: Context) {
            scheduler(context).cancel()
        }

        private fun scheduler(context: Context): IAlwaysOnTaskScheduler {
            return TaskSchedulerFactory(context).alwaysOn(
                BacktrackService::class.java,
                foreground = true
            )
        }

    }

}
