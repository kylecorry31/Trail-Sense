package com.kylecorry.trail_sense.navigation.paths.infrastructure

import android.content.Context
import com.kylecorry.andromeda.jobs.IOneTimeTaskScheduler
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.navigation.paths.infrastructure.services.BacktrackAlwaysOnService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.permissions.AllowForegroundWorkersCommand
import kotlinx.coroutines.runBlocking
import java.time.Duration

object BacktrackScheduler {

    fun restart(context: Context) {
        val prefs = UserPreferences(context)
        if (prefs.backtrackEnabled) {
            stop(context)
            start(context, false)
        }
    }

    fun start(context: Context, startNewPath: Boolean) {
        val prefs = UserPreferences(context)

        if (startNewPath) {
            runBlocking {
                PathService.getInstance(context).endBacktrackPath()
            }
        }

        if (!BacktrackIsAvailable().isSatisfiedBy(context)) {
            return
        }

        AllowForegroundWorkersCommand(context).execute()

        val scheduler = getScheduler(context)
        if (prefs.backtrackRecordFrequency >= Duration.ofMinutes(15)) {
            BacktrackAlwaysOnService.stop(context)
            scheduler.once()
        } else {
            scheduler.cancel()
            BacktrackAlwaysOnService.start(context)
        }
    }

    fun stop(context: Context) {
        val scheduler = getScheduler(context)
        scheduler.cancel()
        BacktrackAlwaysOnService.stop(context)
        AllowForegroundWorkersCommand(context).execute()
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