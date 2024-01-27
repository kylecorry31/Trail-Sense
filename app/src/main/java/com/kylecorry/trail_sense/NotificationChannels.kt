package com.kylecorry.trail_sense

import android.content.Context
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.commands.SunsetAlarmCommand
import com.kylecorry.trail_sense.navigation.paths.infrastructure.services.BacktrackService
import com.kylecorry.trail_sense.tools.clock.infrastructure.NextMinuteBroadcastReceiver
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightService
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.DistanceAlerter
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounterService
import com.kylecorry.trail_sense.tools.waterpurification.infrastructure.WaterPurificationTimerService
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.WhiteNoiseService
import com.kylecorry.trail_sense.weather.infrastructure.alerts.CurrentWeatherAlerter
import com.kylecorry.trail_sense.weather.infrastructure.alerts.DailyWeatherAlerter
import com.kylecorry.trail_sense.weather.infrastructure.alerts.StormAlerter

object NotificationChannels {

    const val GROUP_UPDATES = "trail_sense_updates"
    const val GROUP_FLASHLIGHT = "trail_sense_flashlight"
    const val GROUP_WEATHER = "trail_sense_weather"
    const val GROUP_DAILY_WEATHER = "trail_sense_daily_weather"
    const val GROUP_ASTRONOMY_ALERTS = "trail_sense_astronomy_alerts"
    const val GROUP_STORM = "trail_sense_storm"
    const val GROUP_SUNSET = "trail_sense_sunset"
    const val GROUP_PEDOMETER = "trail_sense_pedometer"
    const val GROUP_WATER = "trail_sense_water"
    const val GROUP_CLOCK = "trail_sense_clock"
    const val GROUP_SERVICE_RESTART = "trail_sense_service_restart"

    const val CHANNEL_ASTRONOMY_ALERTS = "astronomy_alerts"
    const val CHANNEL_SERVICE_RESTART = "service_restart"

    // Legacy (intended for deletion)
    const val CHANNEL_BACKGROUND_UPDATES = "background_updates"
    const val CHANNEL_BACKGROUND_LAUNCHER = "background_launcher"

    fun createChannels(context: Context) {
        // Flashlight
        Notify.createChannel(
            context,
            FlashlightService.CHANNEL_ID,
            context.getString(R.string.flashlight_title),
            context.getString(R.string.flashlight_title),
            Notify.CHANNEL_IMPORTANCE_LOW,
            muteSound = true
        )

        // Backtrack
        Notify.createChannel(
            context,
            BacktrackService.FOREGROUND_CHANNEL_ID,
            context.getString(R.string.backtrack),
            context.getString(R.string.backtrack_notification_channel_description),
            Notify.CHANNEL_IMPORTANCE_LOW,
            muteSound = true
        )

        // Clock
        Notify.createChannel(
            context,
            NextMinuteBroadcastReceiver.CHANNEL_ID,
            context.getString(R.string.notification_channel_clock_sync),
            context.getString(R.string.notification_channel_clock_sync_description),
            Notify.CHANNEL_IMPORTANCE_HIGH,
            false
        )

        // Water boil timer
        Notify.createChannel(
            context,
            WaterPurificationTimerService.CHANNEL_ID,
            context.getString(R.string.water_boil_timer),
            context.getString(R.string.water_boil_timer_channel_description),
            Notify.CHANNEL_IMPORTANCE_HIGH,
            false
        )

        // White noise
        Notify.createChannel(
            context,
            WhiteNoiseService.NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.tool_white_noise_title),
            context.getString(R.string.tool_white_noise_title),
            Notify.CHANNEL_IMPORTANCE_LOW
        )

        // Storm alert
        Notify.createChannel(
            context,
            StormAlerter.STORM_CHANNEL_ID,
            context.getString(R.string.storm_alerts),
            context.getString(R.string.storm_alerts),
            Notify.CHANNEL_IMPORTANCE_HIGH
        )

        Notify.createChannel(
            context,
            CurrentWeatherAlerter.WEATHER_CHANNEL_ID,
            context.getString(R.string.weather_monitor),
            context.getString(R.string.notification_monitoring_weather),
            Notify.CHANNEL_IMPORTANCE_LOW,
            true
        )

        Notify.createChannel(
            context,
            DailyWeatherAlerter.DAILY_CHANNEL_ID,
            context.getString(R.string.todays_forecast),
            context.getString(R.string.todays_forecast),
            Notify.CHANNEL_IMPORTANCE_LOW,
            true
        )

        // Sunset
        Notify.createChannel(
            context,
            SunsetAlarmCommand.NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.sunset_alert_channel_title),
            context.getString(R.string.sunset_alerts),
            Notify.CHANNEL_IMPORTANCE_HIGH,
            false
        )

        // Astronomy alerts
        Notify.createChannel(
            context,
            CHANNEL_ASTRONOMY_ALERTS,
            context.getString(R.string.astronomy_alerts),
            context.getString(R.string.astronomy_alerts),
            Notify.CHANNEL_IMPORTANCE_LOW,
            false
        )

        // Pedometer
        Notify.createChannel(
            context,
            StepCounterService.CHANNEL_ID,
            context.getString(R.string.pedometer),
            context.getString(R.string.pedometer),
            Notify.CHANNEL_IMPORTANCE_LOW,
            true
        )

        Notify.createChannel(
            context,
            DistanceAlerter.NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.distance_alert),
            context.getString(R.string.distance_alert),
            Notify.CHANNEL_IMPORTANCE_HIGH,
            false
        )

        // Service restart
        Notify.createChannel(
            context,
            CHANNEL_SERVICE_RESTART,
            context.getString(R.string.service_restart),
            context.getString(R.string.service_restart_channel_description),
            Notify.CHANNEL_IMPORTANCE_LOW,
            true
        )

        // CHANNEL CLEANUP SECTION
        Notify.deleteChannel(context, CHANNEL_BACKGROUND_UPDATES)
        Notify.deleteChannel(context, CHANNEL_BACKGROUND_LAUNCHER)
    }

}