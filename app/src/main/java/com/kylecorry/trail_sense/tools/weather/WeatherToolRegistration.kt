package com.kylecorry.trail_sense.tools.weather

import android.content.Context
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolBroadcast
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolNotificationChannel
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolSummarySize
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolWidget
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnostic
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory
import com.kylecorry.trail_sense.tools.tools.widgets.RefreshWidgetAction
import com.kylecorry.trail_sense.tools.weather.actions.PauseWeatherMonitorAction
import com.kylecorry.trail_sense.tools.weather.actions.ResumeWeatherMonitorAction
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherMonitorDiagnosticScanner
import com.kylecorry.trail_sense.tools.weather.infrastructure.alerts.CurrentWeatherAlerter
import com.kylecorry.trail_sense.tools.weather.infrastructure.alerts.DailyWeatherAlerter
import com.kylecorry.trail_sense.tools.weather.infrastructure.alerts.StormAlerter
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import com.kylecorry.trail_sense.tools.weather.quickactions.QuickActionWeatherMonitor
import com.kylecorry.trail_sense.tools.weather.services.WeatherMonitorToolService
import com.kylecorry.trail_sense.tools.weather.widgets.AppWidgetPressure
import com.kylecorry.trail_sense.tools.weather.widgets.AppWidgetPressureChart
import com.kylecorry.trail_sense.tools.weather.widgets.AppWidgetWeather
import com.kylecorry.trail_sense.tools.weather.widgets.PressureChartToolWidgetView
import com.kylecorry.trail_sense.tools.weather.widgets.PressureWidgetView
import com.kylecorry.trail_sense.tools.weather.widgets.WeatherToolWidgetView

object WeatherToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.WEATHER,
            context.getString(R.string.weather),
            R.drawable.cloud,
            R.id.action_weather,
            ToolCategory.Weather,
            guideId = R.raw.guide_tool_weather,
            settingsNavAction = R.id.weatherSettingsFragment,
            initialize = { WeatherSubsystem.getInstance(it) },
            quickActions = listOf(
                ToolQuickAction(
                    Tools.QUICK_ACTION_WEATHER_MONITOR,
                    context.getString(R.string.weather_monitor),
                    ::QuickActionWeatherMonitor
                )
            ),
            widgets = listOf(
                ToolWidget(
                    WIDGET_WEATHER,
                    context.getString(R.string.weather),
                    ToolSummarySize.Half,
                    WeatherToolWidgetView(),
                    AppWidgetWeather::class.java,
                    updateBroadcasts = listOf(BROADCAST_WEATHER_PREDICTION_CHANGED)
                ),
                ToolWidget(
                    WIDGET_PRESSURE_TENDENCY,
                    context.getString(R.string.pressure),
                    ToolSummarySize.Half,
                    PressureWidgetView(),
                    AppWidgetPressure::class.java,
                    updateBroadcasts = listOf(BROADCAST_WEATHER_PREDICTION_CHANGED)
                ),
                ToolWidget(
                    WIDGET_PRESSURE_CHART,
                    context.getString(R.string.pressure_chart),
                    ToolSummarySize.Full,
                    PressureChartToolWidgetView(),
                    AppWidgetPressureChart::class.java,
                    updateBroadcasts = listOf(BROADCAST_WEATHER_PREDICTION_CHANGED)
                )
            ),
            isAvailable = { Sensors.hasBarometer(it) },
            tiles = listOf(
                "com.kylecorry.trail_sense.tools.weather.tiles.WeatherMonitorTile"
            ),
            notificationChannels = listOf(
                ToolNotificationChannel(
                    NOTIFICATION_CHANNEL_STORM_ALERT,
                    context.getString(R.string.storm_alerts),
                    context.getString(R.string.storm_alerts),
                    Notify.CHANNEL_IMPORTANCE_HIGH
                ),
                ToolNotificationChannel(
                    CurrentWeatherAlerter.WEATHER_CHANNEL_ID,
                    context.getString(R.string.weather_monitor),
                    context.getString(R.string.notification_monitoring_weather),
                    Notify.CHANNEL_IMPORTANCE_LOW,
                    true
                ),
                ToolNotificationChannel(
                    DailyWeatherAlerter.DAILY_CHANNEL_ID,
                    context.getString(R.string.todays_forecast),
                    context.getString(R.string.todays_forecast),
                    Notify.CHANNEL_IMPORTANCE_LOW,
                    true
                )
            ),
            services = listOf(WeatherMonitorToolService(context)),
            diagnostics = listOf(
                ToolDiagnosticFactory.barometer(context),
                *ToolDiagnosticFactory.altimeter(context),
                ToolDiagnosticFactory.backgroundLocation(context),
                ToolDiagnostic(
                    "weather-monitor",
                    context.getString(R.string.weather_monitor),
                    scanner = WeatherMonitorDiagnosticScanner()
                ),
                ToolDiagnosticFactory.notification(
                    NOTIFICATION_CHANNEL_STORM_ALERT,
                    context.getString(R.string.storm_alerts),
                ),
                ToolDiagnosticFactory.notification(
                    DailyWeatherAlerter.DAILY_CHANNEL_ID,
                    context.getString(R.string.todays_forecast),
                ),
                ToolDiagnosticFactory.notification(
                    CurrentWeatherAlerter.WEATHER_CHANNEL_ID,
                    context.getString(R.string.weather_monitor),
                ),
                ToolDiagnosticFactory.powerSaver(context),
                ToolDiagnosticFactory.backgroundService(context)
            ).distinctBy { it.id },
            broadcasts = listOf(
                ToolBroadcast(
                    BROADCAST_WEATHER_MONITOR_ENABLED,
                    "Weather monitor enabled"
                ),
                ToolBroadcast(
                    BROADCAST_WEATHER_MONITOR_DISABLED,
                    "Weather monitor disabled"
                ),
                ToolBroadcast(
                    BROADCAST_WEATHER_MONITOR_STATE_CHANGED,
                    "Weather monitor state changed"
                ),
                ToolBroadcast(
                    BROADCAST_WEATHER_MONITOR_FREQUENCY_CHANGED,
                    "Weather monitor frequency changed"
                ),
                ToolBroadcast(
                    BROADCAST_WEATHER_PREDICTION_CHANGED,
                    "Weather prediction changed"
                )
            ),
            actions = listOf(
                ToolAction(
                    ACTION_PAUSE_WEATHER_MONITOR,
                    "Pause weather monitor",
                    PauseWeatherMonitorAction()
                ),
                ToolAction(
                    ACTION_RESUME_WEATHER_MONITOR,
                    "Resume weather monitor",
                    ResumeWeatherMonitorAction()
                ),
                ToolAction(
                    ACTION_REFRESH_WEATHER_WIDGET,
                    "Refresh weather widget",
                    RefreshWidgetAction(WIDGET_WEATHER)
                )
            ),
            singletons = listOf(
                WeatherSubsystem::getInstance
            )
        )
    }

    const val BROADCAST_WEATHER_MONITOR_ENABLED = "weather-broadcast-weather-monitor-enabled"
    const val BROADCAST_WEATHER_MONITOR_DISABLED = "weather-broadcast-weather-monitor-disabled"
    const val BROADCAST_WEATHER_MONITOR_STATE_CHANGED =
        "weather-broadcast-weather-monitor-state-changed"
    const val BROADCAST_WEATHER_MONITOR_FREQUENCY_CHANGED =
        "paths-broadcast-weather-monitor-frequency-changed"
    const val BROADCAST_WEATHER_PREDICTION_CHANGED = "weather-broadcast-weather-prediction-changed"

    const val BROADCAST_PARAM_WEATHER_MONITOR_FREQUENCY =
        "paths-broadcast-param-weather-monitor-frequency"


    const val ACTION_PAUSE_WEATHER_MONITOR = "weather-action-pause-weather-monitor"
    const val ACTION_RESUME_WEATHER_MONITOR = "weather-action-resume-weather-monitor"
    const val ACTION_REFRESH_WEATHER_WIDGET = "weather-action-refresh-weather-widget"

    const val SERVICE_WEATHER_MONITOR = "weather-service-weather-monitor"

    const val WIDGET_WEATHER = "weather-widget-weather"
    const val WIDGET_PRESSURE_TENDENCY = "weather-widget-pressure-tendency"
    const val WIDGET_PRESSURE_CHART = "weather-widget-pressure-chart"

    const val NOTIFICATION_CHANNEL_STORM_ALERT = StormAlerter.STORM_CHANNEL_ID
}