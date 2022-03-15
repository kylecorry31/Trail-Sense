package com.kylecorry.trail_sense.navigation.paths.infrastructure

import android.content.Context
import com.kylecorry.andromeda.jobs.IOneTimeTaskScheduler
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.navigation.paths.infrastructure.services.BacktrackAlwaysOnService
import com.kylecorry.trail_sense.shared.UserPreferences
import kotlinx.coroutines.runBlocking
import java.time.Duration

object BacktrackScheduler {
    fun start(context: Context, startNewPath: Boolean) {
        val prefs = UserPreferences(context)

        if (startNewPath) {
            runBlocking {
                PathService.getInstance(context).endBacktrackPath()
            }
        }

        if (prefs.isLowPowerModeOn && prefs.lowPowerModeDisablesBacktrack) {
            return
        }

        if (prefs.backtrackRecordFrequency >= Duration.ofMinutes(15)) {
            val scheduler = getScheduler(context)
            scheduler.once()
        } else {
            BacktrackAlwaysOnService.start(context)
        }
    }

    fun stop(context: Context) {
        val scheduler = getScheduler(context)
        scheduler.cancel()
        context.stopService(BacktrackAlwaysOnService.intent(context))
    }

    fun isOn(context: Context): Boolean {
        return BacktrackIsEnabled().isSatisfiedBy(context)
    }

    fun isDisabled(context: Context): Boolean {
        return BacktrackIsAvailable().not().isSatisfiedBy(context)
    }

    fun getScheduler(context: Context): IOneTimeTaskScheduler {
        return BacktrackWorker.scheduler(context)
    }
}