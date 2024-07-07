package com.kylecorry.trail_sense.tools.paths.services

import android.content.Context
import android.util.Log
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.receivers.ServiceRestartAlerter
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.tryStartForegroundOrNotify
import com.kylecorry.trail_sense.shared.permissions.canStartLocationForgroundService
import com.kylecorry.trail_sense.tools.paths.PathsToolRegistration
import com.kylecorry.trail_sense.tools.paths.infrastructure.BacktrackScheduler
import com.kylecorry.trail_sense.tools.paths.infrastructure.services.BacktrackService
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolService
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import java.time.Duration

class BacktrackToolService(private val context: Context) : ToolService {

    private val prefs = UserPreferences(context)

    override val id: String = PathsToolRegistration.SERVICE_BACKTRACK

    override val name: String = context.getString(R.string.backtrack)

    override fun getFrequency(): Duration {
        return prefs.backtrackRecordFrequency
    }

    override fun isRunning(): Boolean {
        return BacktrackService.isRunning
    }

    override fun isEnabled(): Boolean {
        return prefs.backtrackEnabled
    }

    override fun isBlocked(): Boolean {
        return prefs.isLowPowerModeOn && prefs.lowPowerModeDisablesBacktrack
    }

    override suspend fun enable() {
        if (!Permissions.canStartLocationForgroundService(context)) {
            ServiceRestartAlerter(context).alert()
            Log.d("BacktrackSubsystem", "Cannot start backtrack")
            return
        }


        prefs.backtrackEnabled = true
        Tools.broadcast(PathsToolRegistration.BROADCAST_BACKTRACK_ENABLED)
        restart()
    }

    override suspend fun disable() {
        prefs.backtrackEnabled = false
        Tools.broadcast(PathsToolRegistration.BROADCAST_BACKTRACK_DISABLED)
        stop()
    }

    override suspend fun restart() {
        if (isEnabled() && !isBlocked()) {
            start(false)
        } else {
            stop()
        }
    }

    override suspend fun stop() {
        BacktrackScheduler.stop(context)
        // TODO: Broadcast that the service has stopped
        Tools.broadcast(PathsToolRegistration.BROADCAST_BACKTRACK_STATE_CHANGED)
    }

    private suspend fun start(startNewPath: Boolean) {
        if (!isEnabled() || isBlocked()) {
            // Can't start
            return
        }

        if (isRunning()) {
            // Already running
            return
        }

        tryStartForegroundOrNotify(context) {
            BacktrackScheduler.start(context, startNewPath)
            // TODO: Broadcast that the service has started
        }

        Tools.broadcast(PathsToolRegistration.BROADCAST_BACKTRACK_STATE_CHANGED)
    }
}