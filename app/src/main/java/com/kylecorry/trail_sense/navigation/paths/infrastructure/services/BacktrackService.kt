package com.kylecorry.trail_sense.navigation.paths.infrastructure.services

import android.content.Context
import com.kylecorry.andromeda.background.IAlwaysOnTaskScheduler
import com.kylecorry.andromeda.background.TaskSchedulerFactory
import com.kylecorry.andromeda.background.services.ForegroundInfo
import com.kylecorry.andromeda.background.services.IntervalService
import com.kylecorry.andromeda.core.coroutines.SingleRunner
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.navigation.paths.infrastructure.alerts.BacktrackAlerter
import com.kylecorry.trail_sense.navigation.paths.infrastructure.commands.BacktrackCommand
import com.kylecorry.trail_sense.shared.UserPreferences
import java.time.Duration

class BacktrackService :
    IntervalService(wakelockDuration = Duration.ofSeconds(30), useOneTimeWorkers = true) {
    private val prefs by lazy { UserPreferences(applicationContext) }

    private val backtrackCommand by lazy {
        BacktrackCommand(this)
    }

    override val uniqueId: Int
        get() = 7238542

    override fun getForegroundInfo(): ForegroundInfo? {
        val units = prefs.baseDistanceUnits
        return ForegroundInfo(
            BacktrackAlerter.NOTIFICATION_ID,
            BacktrackAlerter.getNotification(this, Distance(0f, units))
        )
    }


    override val period: Duration
        get() = prefs.backtrackRecordFrequency

    private val runner = SingleRunner()

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
        const val FOREGROUND_CHANNEL_ID = "Backtrack"

        fun start(context: Context) {
            scheduler(context).start()
        }

        fun stop(context: Context) {
            scheduler(context).cancel()
        }

        fun scheduler(context: Context): IAlwaysOnTaskScheduler {
            return TaskSchedulerFactory(context).alwaysOn(
                BacktrackService::class.java,
                foreground = true
            )
        }

    }

}