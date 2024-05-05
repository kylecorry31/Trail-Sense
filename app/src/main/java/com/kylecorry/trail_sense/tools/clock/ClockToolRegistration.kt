package com.kylecorry.trail_sense.tools.clock

import android.content.Context
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.clock.infrastructure.NextMinuteBroadcastReceiver
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolNotificationChannel
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object ClockToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.CLOCK,
            context.getString(R.string.tool_clock_title),
            R.drawable.ic_tool_clock,
            R.id.toolClockFragment,
            ToolCategory.Time,
            guideId = R.raw.guide_tool_clock,
            settingsNavAction = R.id.clockSettingsFragment,
            notificationChannels = listOf(
                ToolNotificationChannel(
                    NextMinuteBroadcastReceiver.CHANNEL_ID,
                    context.getString(R.string.notification_channel_clock_sync),
                    context.getString(R.string.notification_channel_clock_sync_description),
                    Notify.CHANNEL_IMPORTANCE_HIGH,
                    false
                )
            ),
            diagnostics = listOf(
                ToolDiagnosticFactory.alarm(context),
                ToolDiagnosticFactory.gps(context),
                ToolDiagnosticFactory.notification(
                    NextMinuteBroadcastReceiver.CHANNEL_ID,
                    context.getString(R.string.notification_channel_clock_sync)
                )
            )
        )
    }
}