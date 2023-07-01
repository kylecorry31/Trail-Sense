package com.kylecorry.trail_sense.navigation.paths.infrastructure

import android.content.Context
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.navigation.paths.infrastructure.alerts.BacktrackAlerter
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.navigation.paths.infrastructure.services.BacktrackAlwaysOnService
import com.kylecorry.trail_sense.shared.UserPreferences
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
        if (startNewPath) {
            runBlocking {
                PathService.getInstance(context).endBacktrackPath()
            }
        }

        if (!BacktrackIsAvailable().isSatisfiedBy(context)) {
            return
        }

        BacktrackAlwaysOnService.start(context)
    }

    fun stop(context: Context) {
        BacktrackAlwaysOnService.stop(context)
        Notify.cancel(context, BacktrackAlerter.NOTIFICATION_ID)
    }

    fun isOn(context: Context): Boolean {
        return BacktrackIsEnabled().isSatisfiedBy(context)
    }

    fun isDisabled(context: Context): Boolean {
        return BacktrackIsAvailable().not().isSatisfiedBy(context)
    }
}