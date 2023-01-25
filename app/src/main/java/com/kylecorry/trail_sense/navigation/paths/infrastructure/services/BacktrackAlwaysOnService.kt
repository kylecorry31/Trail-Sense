package com.kylecorry.trail_sense.navigation.paths.infrastructure.services

import android.app.Notification
import android.content.Context
import com.kylecorry.andromeda.core.coroutines.SingleRunner
import com.kylecorry.andromeda.jobs.IAlwaysOnTaskScheduler
import com.kylecorry.andromeda.jobs.TaskSchedulerFactory
import com.kylecorry.andromeda.services.CoroutineIntervalService
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.navigation.paths.infrastructure.alerts.BacktrackAlerter
import com.kylecorry.trail_sense.navigation.paths.infrastructure.commands.BacktrackCommand
import com.kylecorry.trail_sense.shared.UserPreferences
import java.time.Duration

class BacktrackAlwaysOnService : CoroutineIntervalService(TAG) {
    private val prefs by lazy { UserPreferences(applicationContext) }

    private val backtrackCommand by lazy {
        BacktrackCommand(this)
    }

    override val foregroundNotificationId: Int
        get() = BacktrackAlerter.NOTIFICATION_ID

    override val period: Duration
        get() = prefs.backtrackRecordFrequency

    private val runner = SingleRunner()

    override fun getForegroundNotification(): Notification {
        val units = prefs.baseDistanceUnits
        return BacktrackAlerter.getNotification(this, Distance(0f, units))
    }

    override suspend fun doWork() {
        runner.single({
            backtrackCommand.execute()
        })
    }

    override fun onDestroy() {
        runner.cancel()
        stopService(true)
        super.onDestroy()
    }

    companion object {
        const val TAG = "BacktrackHighPriorityService"
        const val FOREGROUND_CHANNEL_ID = "Backtrack"

        fun start(context: Context) {
            scheduler(context).start()
        }

        fun stop(context: Context) {
            scheduler(context).cancel()
        }

        fun scheduler(context: Context): IAlwaysOnTaskScheduler {
            return TaskSchedulerFactory(context).alwaysOn(BacktrackAlwaysOnService::class.java)
        }

    }

}