package com.kylecorry.trail_sense.navigation.paths.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.shared.Background
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.permissions.AllowForegroundWorkersCommand
import kotlinx.coroutines.runBlocking

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

        Background.start(context, Background.Backtrack, prefs.backtrackRecordFrequency)
    }

    fun stop(context: Context) {
        Background.stop(context, Background.Backtrack)
        AllowForegroundWorkersCommand(context).execute()
    }

    fun isOn(context: Context): Boolean {
        return BacktrackIsEnabled().isSatisfiedBy(context)
    }

    fun isDisabled(context: Context): Boolean {
        return BacktrackIsAvailable().not().isSatisfiedBy(context)
    }

}