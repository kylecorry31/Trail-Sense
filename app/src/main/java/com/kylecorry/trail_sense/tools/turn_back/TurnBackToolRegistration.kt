package com.kylecorry.trail_sense.tools.turn_back

import android.content.Context
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolNotificationChannel
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory
import com.kylecorry.trail_sense.tools.turn_back.infrastructure.receivers.TurnBackAlarmReceiver
import com.kylecorry.trail_sense.tools.turn_back.services.TurnBackToolService

object TurnBackToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.TURN_BACK,
            context.getString(R.string.tool_turn_back),
            R.drawable.ic_undo,
            R.id.turnBackFragment,
            ToolCategory.Time,
            guideId = R.raw.guide_tool_turn_back,
            settingsNavAction = R.id.fragmentTurnBackSettings,
            notificationChannels = listOf(
                ToolNotificationChannel(
                    NOTIFICATION_CHANNEL_TURN_BACK_ALERT,
                    context.getString(R.string.tool_turn_back),
                    context.getString(R.string.turn_back_alerts),
                    Notify.CHANNEL_IMPORTANCE_HIGH,
                    false
                )
            ),
            services = listOf(TurnBackToolService(context)),
            diagnostics = listOf(
                ToolDiagnosticFactory.alarm(context),
                ToolDiagnosticFactory.gps(context),
                ToolDiagnosticFactory.notification(
                    NOTIFICATION_CHANNEL_TURN_BACK_ALERT,
                    context.getString(R.string.turn_back_alerts)
                )
            )
        )
    }

    const val SERVICE_TURN_BACK = "turn_back-service-turn-back"
    const val NOTIFICATION_CHANNEL_TURN_BACK_ALERT = TurnBackAlarmReceiver.NOTIFICATION_CHANNEL_ID
}