package com.kylecorry.trail_sense

import android.content.Context
import com.kylecorry.notify.Notify
import com.kylecorry.trail_sense.astronomy.infrastructure.SunsetAlarmService
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.services.BacktrackAlwaysOnService
import com.kylecorry.trail_sense.tools.clock.infrastructure.NextMinuteBroadcastReceiver
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightService
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.SosService
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.StrobeService
import com.kylecorry.trail_sense.tools.speedometer.infrastructure.PedometerService
import com.kylecorry.trail_sense.tools.waterpurification.infrastructure.WaterPurificationTimerService
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.WhiteNoiseService
import com.kylecorry.trail_sense.weather.infrastructure.services.WeatherUpdateService

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

    const val CHANNEL_BACKGROUND_UPDATES = "background_updates"

    fun createChannels(context: Context) {
        val notify = Notify(context)
        // Flashlight
        notify.createChannel(
            StrobeService.CHANNEL_ID,
            context.getString(R.string.flashlight_title),
            context.getString(R.string.flashlight_title),
            Notify.CHANNEL_IMPORTANCE_LOW,
            muteSound = true
        )

        notify.createChannel(
            FlashlightService.CHANNEL_ID,
            context.getString(R.string.flashlight_title),
            context.getString(R.string.flashlight_title),
            Notify.CHANNEL_IMPORTANCE_LOW,
            muteSound = true
        )

        notify.createChannel(
            SosService.CHANNEL_ID,
            context.getString(R.string.flashlight_title),
            context.getString(R.string.flashlight_title),
            Notify.CHANNEL_IMPORTANCE_LOW,
            muteSound = true
        )


        // Backtrack
        notify.createChannel(
            BacktrackAlwaysOnService.FOREGROUND_CHANNEL_ID,
            context.getString(R.string.backtrack_notification_channel),
            context.getString(R.string.backtrack_notification_channel_description),
            Notify.CHANNEL_IMPORTANCE_LOW,
            muteSound = true
        )

        // Background updates
        notify.createChannel(
            CHANNEL_BACKGROUND_UPDATES,
            context.getString(R.string.updates),
            context.getString(R.string.updates),
            Notify.CHANNEL_IMPORTANCE_LOW,
            muteSound = true
        )

        // Clock
        notify.createChannel(
            NextMinuteBroadcastReceiver.CHANNEL_ID,
            context.getString(R.string.notification_channel_clock_sync),
            context.getString(R.string.notification_channel_clock_sync_description),
            Notify.CHANNEL_IMPORTANCE_HIGH,
            false
        )

        // Water boil timer
        notify.createChannel(
            WaterPurificationTimerService.CHANNEL_ID,
            context.getString(R.string.water_boil_timer_channel),
            context.getString(R.string.water_boil_timer_channel_description),
            Notify.CHANNEL_IMPORTANCE_HIGH,
            false
        )

        // White noise
        notify.createChannel(
            WhiteNoiseService.NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.tool_white_noise_title),
            context.getString(R.string.tool_white_noise_title),
            Notify.CHANNEL_IMPORTANCE_LOW
        )

        // Storm alert
        notify.createChannel(
            WeatherUpdateService.STORM_CHANNEL_ID,
            context.getString(R.string.notification_storm_alert_channel_name),
            context.getString(R.string.notification_storm_alert_channel_desc),
            Notify.CHANNEL_IMPORTANCE_HIGH
        )

        notify.createChannel(
            WeatherUpdateService.WEATHER_CHANNEL_ID,
            context.getString(R.string.weather),
            context.getString(R.string.notification_monitoring_weather),
            Notify.CHANNEL_IMPORTANCE_LOW,
            true
        )

        notify.createChannel(
            WeatherUpdateService.DAILY_CHANNEL_ID,
            context.getString(R.string.todays_forecast),
            context.getString(R.string.todays_forecast),
            Notify.CHANNEL_IMPORTANCE_LOW,
            true
        )

        // Sunset
        notify.createChannel(
            SunsetAlarmService.NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.sunset_alert_channel_title),
            context.getString(R.string.sunset_alert_channel_description),
            Notify.CHANNEL_IMPORTANCE_HIGH,
            false
        )

        // Odometer
        notify.createChannel(
            PedometerService.CHANNEL_ID,
            context.getString(R.string.odometer),
            context.getString(R.string.odometer),
            Notify.CHANNEL_IMPORTANCE_LOW,
            true
        )
    }

}