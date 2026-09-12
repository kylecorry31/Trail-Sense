package com.kylecorry.trail_sense.tools.paths.infrastructure.services

import android.content.Context
import android.content.Intent
import com.kylecorry.andromeda.background.IAlwaysOnTaskScheduler
import com.kylecorry.andromeda.background.TaskSchedulerFactory
import com.kylecorry.andromeda.background.services.ForegroundInfo
import com.kylecorry.andromeda.background.services.IntervalService
import com.kylecorry.andromeda.sense.location.GPS
import com.kylecorry.luna.concurrency.CoroutineQueueRunner
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.luna.time.FlowableTimer
import com.kylecorry.luna.time.ITimer
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.tryStartForegroundOrNotify
import com.kylecorry.trail_sense.shared.sensors.gps.GPSSource
import com.kylecorry.trail_sense.shared.sensors.gps.GPSSourceSelector
import com.kylecorry.trail_sense.tools.paths.PathsToolRegistration
import com.kylecorry.trail_sense.tools.paths.infrastructure.alerts.BacktrackAlerter
import com.kylecorry.trail_sense.tools.paths.infrastructure.commands.BacktrackCommand
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import java.time.Duration

class BacktrackService :
    IntervalService(wakelockDuration = Duration.ofSeconds(60), useOneTimeWorkers = true) {
    private val prefs by lazy { UserPreferences(applicationContext) }

    private val backtrackCommand by lazy {
        BacktrackCommand(this)
    }

    override val uniqueId: Int
        get() = 7238542

    override val holdWakelockWhenBelowThreshold: Boolean
        get() = prefs.backtrackKeepDeviceAwake

    override fun getNonWorkerTimer(action: suspend () -> Unit): ITimer {
        val canWakeWithLocationUpdates = !prefs.backtrackKeepDeviceAwake &&
                GPSSourceSelector(this).getSource(useCache = false) == GPSSource.Device

        if (!canWakeWithLocationUpdates) {
            return CoroutineTimer { action() }
        }

        return FlowableTimer({ periodMillis ->
            // This intentionally does not use the CustomGPS because it only needs to use the GPS as a wakeup source
            // This has the side effect of warming up the GPS for backtrack
            GPS(
                this,
                frequency = Duration.ofMillis(periodMillis),
                listenToNmea = false,
                listenToGnssStatusChanges = false
            )
        }, unregisterWhileRunning = true, action = action)
    }

    override fun getForegroundInfo(): ForegroundInfo {
        val units = prefs.baseDistanceUnits
        return ForegroundInfo(
            BacktrackAlerter.NOTIFICATION_ID,
            BacktrackAlerter.getNotification(this, Distance.from(0f, units))
        )
    }


    override val period: Duration
        get() = prefs.backtrackRecordFrequency

    private val runner = CoroutineQueueRunner()

    override suspend fun doWork() {
        runner.skipIfRunning {
            backtrackCommand.execute()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        return tryStartForegroundOrNotify {
            super.onStartCommand(intent, flags, startId)
        }
    }

    override fun onDestroy() {
        isRunning = false
        runner.cancel()
        stopService(true)
        super.onDestroy()
    }

    companion object {
        const val FOREGROUND_CHANNEL_ID = "Backtrack"

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
