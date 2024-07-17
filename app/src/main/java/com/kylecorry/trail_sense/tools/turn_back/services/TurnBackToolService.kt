package com.kylecorry.trail_sense.tools.turn_back.services

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolService
import com.kylecorry.trail_sense.tools.turn_back.TurnBackToolRegistration
import com.kylecorry.trail_sense.tools.turn_back.infrastructure.receivers.TurnBackAlarmReceiver
import com.kylecorry.trail_sense.tools.turn_back.ui.TurnBackFragment
import java.time.Duration

class TurnBackToolService(private val context: Context) : ToolService {

    private val prefs = PreferencesSubsystem.getInstance(context)

    override val id: String = TurnBackToolRegistration.SERVICE_TURN_BACK

    override val name: String = context.getString(R.string.tool_turn_back)

    override fun getFrequency(): Duration {
        return Duration.ofDays(1)
    }

    override fun isRunning(): Boolean {
        return isEnabled() && !isBlocked()
    }

    override fun isEnabled(): Boolean {
        return prefs.preferences.getInstant(
            TurnBackFragment.PREF_TURN_BACK_TIME
        ) != null
    }

    override fun isBlocked(): Boolean {
        return false
    }

    override suspend fun enable() {
        // This does not support direct enabling
        restart()
    }

    override suspend fun disable() {
        prefs.preferences.remove(TurnBackFragment.PREF_TURN_BACK_TIME)
        prefs.preferences.remove(TurnBackFragment.PREF_TURN_BACK_RETURN_TIME)
        stop()
    }

    override suspend fun restart() {
        // This will short circuit if the tool is not active
        TurnBackAlarmReceiver.start(context)
    }

    override suspend fun stop() {
        TurnBackAlarmReceiver.stop(context)
    }
}