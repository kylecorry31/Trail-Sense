package com.kylecorry.trail_sense.tools.turn_back

import android.content.Context
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolNotificationChannel
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolService
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory
import com.kylecorry.trail_sense.tools.turn_back.infrastructure.receivers.TurnBackAlarmReceiver
import com.kylecorry.trail_sense.tools.turn_back.ui.TurnBackFragment
import java.time.Duration

object TurnBackToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.TURN_BACK,
            context.getString(R.string.tool_turn_back),
            R.drawable.ic_undo,
            R.id.turnBackFragment,
            ToolCategory.Time,
            guideId = R.raw.guide_tool_turn_back,
            notificationChannels = listOf(
                ToolNotificationChannel(
                    TurnBackAlarmReceiver.NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.tool_turn_back),
                    context.getString(R.string.turn_back_alerts),
                    Notify.CHANNEL_IMPORTANCE_HIGH,
                    false
                )
            ),
            services = listOf(
                ToolService(
                    context.getString(R.string.tool_turn_back),
                    getFrequency = { Duration.ofDays(1) },
                    isActive = {
                        val prefs = PreferencesSubsystem.getInstance(it)
                        prefs.preferences.getInstant(
                            TurnBackFragment.PREF_TURN_BACK_TIME
                        ) != null
                    },
                    disable = {
                        val prefs = PreferencesSubsystem.getInstance(it)
                        prefs.preferences.remove(TurnBackFragment.PREF_TURN_BACK_TIME)
                        prefs.preferences.remove(TurnBackFragment.PREF_TURN_BACK_RETURN_TIME)
                        TurnBackAlarmReceiver.stop(it)
                    },
                    stop = {
                        TurnBackAlarmReceiver.stop(it)
                    },
                    restart = {
                        // This will short circuit if the tool is not active
                        TurnBackAlarmReceiver.start(context)
                    }
                )
            ),
            diagnostics = listOf(
                ToolDiagnosticFactory.alarm(context),
                ToolDiagnosticFactory.gps(context),
                ToolDiagnosticFactory.notification(
                    TurnBackAlarmReceiver.NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.turn_back_alerts)
                )
            )
        )
    }
}