package com.kylecorry.trail_sense

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
import com.kylecorry.trail_sense.astronomy.infrastructure.receivers.SunsetAlarmReceiver
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.services.BacktrackService
import com.kylecorry.trail_sense.tools.clock.infrastructure.NextMinuteBroadcastReceiver
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightService
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.SosService
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.StrobeService
import com.kylecorry.trail_sense.tools.speedometer.infrastructure.PedometerService
import com.kylecorry.trail_sense.tools.waterpurification.infrastructure.WaterPurificationTimerService
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.WhiteNoiseService
import com.kylecorry.trail_sense.weather.infrastructure.services.WeatherUpdateService
import com.kylecorry.trailsensecore.infrastructure.system.NotificationUtils

object NotificationChannels {

    const val GROUP_UPDATES = "trail_sense_updates"
    const val GROUP_FLASHLIGHT = "trail_sense_flashlight"
    const val GROUP_WEATHER = "trail_sense_weather"
    const val GROUP_DAILY_WEATHER = "trail_sense_daily_weather"
    const val GROUP_STORM = "trail_sense_storm"
    const val GROUP_SUNSET = "trail_sense_sunset"
    const val GROUP_PEDOMETER = "trail_sense_pedometer"
    const val GROUP_WATER = "trail_sense_water"
    const val GROUP_CLOCK = "trail_sense_clock"

    fun createChannels(context: Context) {
        // Flashlight
        NotificationUtils.createChannel(
            context,
            StrobeService.CHANNEL_ID,
            context.getString(R.string.flashlight_title),
            context.getString(R.string.flashlight_title),
            NotificationUtils.CHANNEL_IMPORTANCE_LOW,
            muteSound = true
        )

        NotificationUtils.createChannel(
            context,
            FlashlightService.CHANNEL_ID,
            context.getString(R.string.flashlight_title),
            context.getString(R.string.flashlight_title),
            NotificationUtils.CHANNEL_IMPORTANCE_LOW,
            muteSound = true
        )

        NotificationUtils.createChannel(
            context,
            SosService.CHANNEL_ID,
            context.getString(R.string.flashlight_title),
            context.getString(R.string.flashlight_title),
            NotificationUtils.CHANNEL_IMPORTANCE_LOW,
            muteSound = true
        )


        // Backtrack
        NotificationUtils.createChannel(
            context,
            BacktrackService.FOREGROUND_CHANNEL_ID,
            context.getString(R.string.backtrack_notification_channel),
            context.getString(R.string.backtrack_notification_channel_description),
            NotificationUtils.CHANNEL_IMPORTANCE_LOW,
            muteSound = true
        )

        // Clock
        NotificationUtils.createChannel(
            context,
            NextMinuteBroadcastReceiver.CHANNEL_ID,
            context.getString(R.string.notification_channel_clock_sync),
            context.getString(R.string.notification_channel_clock_sync_description),
            NotificationUtils.CHANNEL_IMPORTANCE_HIGH,
            false
        )

        // Water boil timer
        NotificationUtils.createChannel(
            context,
            WaterPurificationTimerService.CHANNEL_ID,
            context.getString(R.string.water_boil_timer_channel),
            context.getString(R.string.water_boil_timer_channel_description),
            NotificationUtils.CHANNEL_IMPORTANCE_HIGH,
            false
        )

        // White noise
        NotificationUtils.createChannel(
            context,
            WhiteNoiseService.NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.tool_white_noise_title),
            context.getString(R.string.tool_white_noise_title),
            NotificationUtils.CHANNEL_IMPORTANCE_LOW
        )

        // Storm alert
        NotificationUtils.createChannel(
            context,
            WeatherUpdateService.STORM_CHANNEL_ID,
            context.getString(R.string.notification_storm_alert_channel_name),
            context.getString(R.string.notification_storm_alert_channel_desc),
            NotificationUtils.CHANNEL_IMPORTANCE_HIGH
        )

        // Weather
        NotificationUtils.createChannel(
            context,
            WeatherUpdateService.FOREGROUND_CHANNEL_ID,
            context.getString(R.string.weather_update_notification_channel),
            context.getString(R.string.weather_update_notification_channel_desc),
            NotificationUtils.CHANNEL_IMPORTANCE_LOW,
            true
        )

        NotificationUtils.createChannel(
            context,
            WeatherUpdateService.WEATHER_CHANNEL_ID,
            context.getString(R.string.weather),
            context.getString(R.string.notification_monitoring_weather),
            NotificationUtils.CHANNEL_IMPORTANCE_LOW,
            true
        )

        NotificationUtils.createChannel(
            context,
            WeatherUpdateService.DAILY_CHANNEL_ID,
            context.getString(R.string.todays_forecast),
            context.getString(R.string.todays_forecast),
            NotificationUtils.CHANNEL_IMPORTANCE_LOW,
            true
        )

        // Sunset
        NotificationUtils.createChannel(
            context,
            SunsetAlarmReceiver.NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.sunset_alert_channel_title),
            context.getString(R.string.sunset_alert_channel_description),
            NotificationUtils.CHANNEL_IMPORTANCE_HIGH,
            false
        )

        // Odometer
        NotificationUtils.createChannel(
            context,
            PedometerService.CHANNEL_ID,
            context.getString(R.string.odometer),
            context.getString(R.string.odometer),
            NotificationUtils.CHANNEL_IMPORTANCE_LOW,
            true
        )
    }

}