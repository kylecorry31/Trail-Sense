package com.kylecorry.trail_sense.tools.astronomy

import android.content.Context
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.commands.AstronomyAlertCommand
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.commands.SunsetAlarmCommand
import com.kylecorry.trail_sense.tools.astronomy.quickactions.QuickActionNightMode
import com.kylecorry.trail_sense.tools.astronomy.quickactions.QuickActionSunsetAlert
import com.kylecorry.trail_sense.tools.diagnostics.domain.DiagnosticCode
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolDiagnostic
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolDiagnosticFactory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolNotificationChannel
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolService
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import java.time.Duration

object AstronomyToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.ASTRONOMY,
            context.getString(R.string.astronomy),
            R.drawable.ic_astronomy,
            R.id.action_astronomy,
            ToolCategory.Time,
            guideId = R.raw.guide_tool_astronomy,
            settingsNavAction = R.id.astronomySettingsFragment,
            quickActions = listOf(
                ToolQuickAction(
                    Tools.QUICK_ACTION_SUNSET_ALERT,
                    context.getString(R.string.sunset_alerts),
                    ::QuickActionSunsetAlert
                ),
                ToolQuickAction(
                    Tools.QUICK_ACTION_NIGHT_MODE,
                    context.getString(R.string.night),
                    ::QuickActionNightMode
                )
            ),
            notificationChannels = listOf(
                ToolNotificationChannel(
                    SunsetAlarmCommand.NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.sunset_alert_channel_title),
                    context.getString(R.string.sunset_alerts),
                    Notify.CHANNEL_IMPORTANCE_HIGH,
                    false
                ),
                ToolNotificationChannel(
                    AstronomyAlertCommand.NOTIFICATION_CHANNEL,
                    context.getString(R.string.astronomy_alerts),
                    context.getString(R.string.astronomy_alerts),
                    Notify.CHANNEL_IMPORTANCE_LOW,
                    false
                )
            ),
            services = listOf(
                ToolService(
                    context.getString(R.string.sunset_alerts),
                    getFrequency = { Duration.ofDays(1) },
                    isActive = {
                        UserPreferences(it).astronomy.sendSunsetAlerts
                    },
                    disable = {
                        UserPreferences(it).astronomy.sendSunsetAlerts = false
                    }
                )
            ),
            diagnostics = listOf(
                ToolDiagnostic.alarm,
                ToolDiagnostic.gps,
                ToolDiagnostic.notification(
                    SunsetAlarmCommand.NOTIFICATION_CHANNEL_ID,
                    DiagnosticCode.SunsetAlertsBlocked
                )
            ),
            diagnostics2 = listOf(
                ToolDiagnosticFactory.gps(context),
                ToolDiagnosticFactory.backgroundLocation(context),
                ToolDiagnosticFactory.alarm(context),
                ToolDiagnosticFactory.notification(
                    SunsetAlarmCommand.NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.sunset_alerts)
                )
            )
        )
    }
}