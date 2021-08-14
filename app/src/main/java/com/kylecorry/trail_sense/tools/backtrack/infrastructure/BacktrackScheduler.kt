package com.kylecorry.trail_sense.tools.backtrack.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.services.BacktrackAlwaysOnService
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.services.BacktrackService
import com.kylecorry.andromeda.jobs.ITaskScheduler
import java.time.Duration

object BacktrackScheduler {
    fun start(context: Context) {
        val prefs = UserPreferences(context)
        if (prefs.isLowPowerModeOn && prefs.lowPowerModeDisablesBacktrack){
            return
        }
        if (prefs.backtrackRecordFrequency >= Duration.ofMinutes(10)) {
            val scheduler = getScheduler(context)
            scheduler.schedule(Duration.ZERO)
        } else {
            BacktrackAlwaysOnService.start(context)
        }
    }

    fun stop(context: Context) {
        val scheduler = getScheduler(context)
        scheduler.cancel()
        context.stopService(BacktrackAlwaysOnService.intent(context))
        context.stopService(BacktrackService.intent(context))
    }

    fun isOn(context: Context): Boolean {
        val prefs = UserPreferences(context)
        return prefs.backtrackEnabled && !isDisabled(context)
    }

    fun isDisabled(context: Context): Boolean {
        val prefs = UserPreferences(context)
        return prefs.isLowPowerModeOn && prefs.lowPowerModeDisablesBacktrack
    }

    fun getScheduler(context: Context): ITaskScheduler {
        return BacktrackWorker.scheduler(context)
    }
}