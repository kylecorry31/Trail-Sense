package com.kylecorry.trail_sense.tools.paths.services

import android.content.Context
import android.os.Bundle
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.receivers.ServiceRestartAlerter
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.tryStartForegroundOrNotify
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.shared.permissions.canStartLocationForegroundService
import com.kylecorry.trail_sense.shared.permissions.isAppForeground
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.tools.paths.PathsToolRegistration
import com.kylecorry.trail_sense.tools.paths.infrastructure.BacktrackScheduler
import com.kylecorry.trail_sense.tools.paths.infrastructure.services.BacktrackService
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolService
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import java.time.Duration

class BacktrackToolService(private val context: Context) : ToolService {

    private val prefs = UserPreferences(context)
    private val sharedPreferences = PreferencesSubsystem.getInstance(context).preferences
    private val stateChangePrefKeys = listOf(
        R.string.pref_backtrack_enabled,
        R.string.pref_low_power_mode,
        R.string.pref_low_power_mode_backtrack
    ).map { context.getString(it) }

    private val frequencyChangePrefKeys = listOf(
        R.string.pref_backtrack_frequency
    ).map { context.getString(it) }

    override val id: String = PathsToolRegistration.SERVICE_BACKTRACK

    override val name: String = context.getString(R.string.backtrack)

    init {
        sharedPreferences.onChange.subscribe(this::onPreferencesChanged)
    }

    override fun getFrequency(): Duration {
        return prefs.paths.backtrackRecordFrequency
    }

    override fun isRunning(): Boolean {
        return BacktrackService.isRunning
    }

    override fun isEnabled(): Boolean {
        return prefs.paths.backtrackEnabled
    }

    override fun isBlocked(): Boolean {
        return prefs.isLowPowerModeOn && prefs.lowPowerModeDisablesBacktrack
    }

    override suspend fun enable() {
        if (!Permissions.canStartLocationForegroundService(context)) {
            ServiceRestartAlerter(context).alert()
            getAppService<Logger>().warn(
                TAG,
                "Cannot start backtrack: missing location permission for foreground service " +
                    "(app in foreground: ${isAppForeground()}, background location: ${Permissions.isBackgroundLocationEnabled(context)})"
            )
            return
        }

        prefs.paths.backtrackEnabled = true
        Tools.broadcast(PathsToolRegistration.BROADCAST_BACKTRACK_ENABLED)
        if (!isBlocked()) {
            start(true)
        }
    }

    override suspend fun disable() {
        prefs.paths.backtrackEnabled = false
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
        }
    }

    private fun onPreferencesChanged(preference: String): Boolean {
        if (preference in stateChangePrefKeys) {
            Tools.broadcast(PathsToolRegistration.BROADCAST_BACKTRACK_STATE_CHANGED)
        }

        if (preference in frequencyChangePrefKeys) {
            Tools.broadcast(
                PathsToolRegistration.BROADCAST_BACKTRACK_FREQUENCY_CHANGED,
                Bundle().apply {
                    putLong(PathsToolRegistration.BROADCAST_PARAM_BACKTRACK_FREQUENCY, prefs.paths.backtrackRecordFrequency.toMillis())
                }
            )
        }

        return true
    }

    protected fun finalize() {
        sharedPreferences.onChange.unsubscribe(this::onPreferencesChanged)
    }

    companion object {
        private const val TAG = "BacktrackToolService"
    }
}
