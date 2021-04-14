package com.kylecorry.trail_sense.tools.backtrack.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.services.BacktrackService
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import com.kylecorry.trailsensecore.infrastructure.tasks.ITaskScheduler
import java.time.Duration

object BacktrackScheduler {
    fun start(context: Context) {
        val prefs = UserPreferences(context)
        if (prefs.isLowPowerModeOn && prefs.lowPowerModeDisablesBacktrack){
            return
        }
        val scheduler = getScheduler(context)
        scheduler.schedule(Duration.ZERO)
    }

    fun stop(context: Context) {
        val scheduler = getScheduler(context)
        scheduler.cancel()
        context.stopService(BacktrackService.intent(context))
    }

    fun getScheduler(context: Context): ITaskScheduler {
        return BacktrackWorker.scheduler(context)
    }
}