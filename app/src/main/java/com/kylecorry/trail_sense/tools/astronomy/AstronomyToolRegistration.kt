package com.kylecorry.trail_sense.tools.astronomy

import android.content.Context
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.commands.AstronomyAlertCommand
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.commands.SunriseAlarmCommand
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.commands.SunsetAlarmCommand
import com.kylecorry.trail_sense.tools.astronomy.quickactions.QuickActionNightMode
import com.kylecorry.trail_sense.tools.astronomy.quickactions.QuickActionSunriseAlert
import com.kylecorry.trail_sense.tools.astronomy.quickactions.QuickActionSunsetAlert
import com.kylecorry.trail_sense.tools.astronomy.services.AstronomyAlertsToolService
import com.kylecorry.trail_sense.tools.astronomy.services.SunriseAlertsToolService
import com.kylecorry.trail_sense.tools.astronomy.services.SunsetAlertsToolService
import com.kylecorry.trail_sense.tools.astronomy.widgets.AppWidgetMoon
import com.kylecorry.trail_sense.tools.astronomy.widgets.AppWidgetSun
import com.kylecorry.trail_sense.tools.astronomy.widgets.AppWidgetSunAndMoonChart
import com.kylecorry.trail_sense.tools.astronomy.widgets.MoonToolWidgetView
import com.kylecorry.trail_sense.tools.astronomy.widgets.SunAndMoonChartToolWidgetView
import com.kylecorry.trail_sense.tools.astronomy.widgets.SunToolWidgetView
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolBroadcast
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolNotificationChannel
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolSummarySize
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolWidget
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

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
                    Tools.QUICK_ACTION_SUNRISE_ALERT,
                    context.getString(R.string.sunrise_alerts),
                    ::QuickActionSunriseAlert
                ),
                ToolQuickAction(
                    Tools.QUICK_ACTION_NIGHT_MODE,
                    context.getString(R.string.night),
                    ::QuickActionNightMode
                )
            ),
            widgets = listOf(
                ToolWidget(
                    WIDGET_SUN,
                    context.getString(R.string.sun),
                    ToolSummarySize.Half,
                    SunToolWidgetView(),
                    AppWidgetSun::class.java,
                    usesLocation = true
                ),
                ToolWidget(
                    WIDGET_MOON,
                    context.getString(R.string.moon),
                    ToolSummarySize.Half,
                    MoonToolWidgetView(),
                    AppWidgetMoon::class.java,
                    usesLocation = true
                ),
                ToolWidget(
                    WIDGET_SUN_AND_MOON_CHART,
                    context.getString(R.string.sun_moon_chart),
                    ToolSummarySize.Full,
                    SunAndMoonChartToolWidgetView(),
                    AppWidgetSunAndMoonChart::class.java,
                    usesLocation = true
                )
            ),
            notificationChannels = listOf(
                ToolNotificationChannel(
                    NOTIFICATION_CHANNEL_SUNSET_ALERT,
                    context.getString(R.string.sunset_alert_channel_title),
                    context.getString(R.string.sunset_alerts),
                    Notify.CHANNEL_IMPORTANCE_HIGH,
                    false
                ),
                ToolNotificationChannel(
                    NOTIFICATION_CHANNEL_SUNRISE_ALERT,
                    context.getString(R.string.sunrise_alert_channel_title),
                    context.getString(R.string.sunrise_alerts),
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
                SunsetAlertsToolService(context),
                AstronomyAlertsToolService(context),
                SunriseAlertsToolService(context)
            ),
            diagnostics = listOf(
                ToolDiagnosticFactory.gps(context),
                ToolDiagnosticFactory.backgroundLocation(context),
                ToolDiagnosticFactory.alarm(context),
                ToolDiagnosticFactory.notification(
                    SunsetAlarmCommand.NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.sunset_alerts)
                ),
                ToolDiagnosticFactory.notification(
                    SunriseAlarmCommand.NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.sunrise_alerts)
                ),
                ToolDiagnosticFactory.notification(
                    AstronomyAlertCommand.NOTIFICATION_CHANNEL,
                    context.getString(R.string.astronomy_alerts)
                )
            ),
            broadcasts = listOf(
                ToolBroadcast(
                    BROADCAST_SUNSET_ALERTS_ENABLED,
                    "Sunset alerts enabled"
                ),
                ToolBroadcast(
                    BROADCAST_SUNSET_ALERTS_DISABLED,
                    "Sunset alerts disabled"
                ),
                ToolBroadcast(
                    BROADCAST_SUNSET_ALERTS_STATE_CHANGED,
                    "Sunset alerts state changed"
                )
            )
        )
    }

    const val BROADCAST_SUNSET_ALERTS_ENABLED = "astronomy-broadcast-sunset-alerts-enabled"
    const val BROADCAST_SUNSET_ALERTS_DISABLED = "astronomy-broadcast-sunset-alerts-disabled"
    const val BROADCAST_SUNSET_ALERTS_STATE_CHANGED =
        "astronomy-broadcast-sunset-alerts-state-changed"

    const val BROADCAST_SUNRISE_ALERTS_ENABLED = "astronomy-broadcast-sunrise-alerts-enabled"
    const val BROADCAST_SUNRISE_ALERTS_DISABLED = "astronomy-broadcast-sunrise-alerts-disabled"
    const val BROADCAST_SUNRISE_ALERTS_STATE_CHANGED =
        "astronomy-broadcast-sunrise-alerts-state-changed"

    const val SERVICE_SUNSET_ALERTS = "astronomy-service-sunset-alerts"
    const val SERVICE_SUNRISE_ALERTS = "astronomy-service-sunrise-alerts"
    const val SERVICE_ASTRONOMY_ALERTS = "astronomy-service-astronomy-alerts"

    const val WIDGET_SUN = "astronomy-widget-sun"
    const val WIDGET_MOON = "astronomy-widget-moon"
    const val WIDGET_SUN_AND_MOON_CHART = "astronomy-widget-sun-and-moon-chart"

    const val NOTIFICATION_CHANNEL_SUNRISE_ALERT = SunriseAlarmCommand.NOTIFICATION_CHANNEL_ID
    const val NOTIFICATION_CHANNEL_SUNSET_ALERT = SunsetAlarmCommand.NOTIFICATION_CHANNEL_ID
}