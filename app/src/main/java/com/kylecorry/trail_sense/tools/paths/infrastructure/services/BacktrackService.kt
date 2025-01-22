package com.kylecorry.trail_sense.tools.paths.infrastructure.services

import android.content.Context
import android.content.Intent
import com.kylecorry.andromeda.background.IAlwaysOnTaskScheduler
import com.kylecorry.andromeda.background.TaskSchedulerFactory
import com.kylecorry.andromeda.background.services.ForegroundInfo
import com.kylecorry.andromeda.background.services.IntervalService
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.receivers.ServiceRestartAlerter
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.paths.PathsToolRegistration
import com.kylecorry.trail_sense.tools.paths.infrastructure.alerts.BacktrackAlerter
import com.kylecorry.trail_sense.tools.paths.infrastructure.commands.BacktrackCommand
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import java.time.Duration

class BacktrackService :
    IntervalService(wakelockDuration = Duration.ofSeconds(30), useOneTimeWorkers = true) {
    private val prefs by lazy { UserPreferences(applicationContext) }

    private val backtrackCommand by lazy {
        BacktrackCommand(this)
    }

    override val uniqueId: Int
        get() = 7238542

    override fun getForegroundInfo(): ForegroundInfo {
        val units = prefs.baseDistanceUnits
        return ForegroundInfo(
            BacktrackAlerter.NOTIFICATION_ID,
            BacktrackAlerter.getNotification(this, Distance(0f, units))
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
        try {
            return super.onStartCommand(intent, flags, startId)
        } catch (e: Exception) {
            ServiceRestartAlerter(this).alert()
            stopSelf()
            return START_NOT_STICKY
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