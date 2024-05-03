package com.kylecorry.trail_sense.tools.weather

import android.content.Context
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.diagnostics.domain.DiagnosticCode
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolDiagnostic
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolNotificationChannel
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolService
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherMonitorDiagnostic
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherMonitorIsEnabled
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherUpdateScheduler
import com.kylecorry.trail_sense.tools.weather.infrastructure.alerts.CurrentWeatherAlerter
import com.kylecorry.trail_sense.tools.weather.infrastructure.alerts.DailyWeatherAlerter
import com.kylecorry.trail_sense.tools.weather.infrastructure.alerts.StormAlerter
import com.kylecorry.trail_sense.tools.weather.quickactions.QuickActionWeatherMonitor

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
            quickActions = listOf(
                ToolQuickAction(
                    Tools.QUICK_ACTION_WEATHER_MONITOR,
                    context.getString(R.string.weather_monitor),
                    ::QuickActionWeatherMonitor
                )
            ),
            isAvailable = { Sensors.hasBarometer(it) },
            tiles = listOf(
                "com.kylecorry.trail_sense.tools.weather.tiles.WeatherMonitorTile"
            ),
            notificationChannels = listOf(
                ToolNotificationChannel(
                    StormAlerter.STORM_CHANNEL_ID,
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
            services = listOf(
                ToolService(
                    context.getString(R.string.weather_monitor),
                    getFrequency = { UserPreferences(it).weather.weatherUpdateFrequency },
                    isActive = {
                        WeatherMonitorIsEnabled().isSatisfiedBy(it)
                    },
                    disable = {
                        UserPreferences(it).weather.shouldMonitorWeather = false
                        WeatherUpdateScheduler.stop(it)
                    }
                )
            ),
            diagnostics = listOf(
                ToolDiagnostic.barometer,
                ToolDiagnostic("weather-monitor-diagnostic") { WeatherMonitorDiagnostic(it.requireContext()) },
                ToolDiagnostic.notification(
                    StormAlerter.STORM_CHANNEL_ID,
                    DiagnosticCode.StormAlertsBlocked
                ),
                ToolDiagnostic.notification(
                    DailyWeatherAlerter.DAILY_CHANNEL_ID,
                    DiagnosticCode.DailyForecastNotificationsBlocked
                ),
                ToolDiagnostic.notification(
                    CurrentWeatherAlerter.WEATHER_CHANNEL_ID,
                    DiagnosticCode.WeatherNotificationsBlocked
                )
            )
        )
    }
}