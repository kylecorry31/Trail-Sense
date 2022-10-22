package com.kylecorry.trail_sense.diagnostics

import android.app.NotificationManager
import android.content.Context
import android.hardware.Sensor
import android.os.Build
import androidx.core.content.getSystemService
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.trail_sense.astronomy.infrastructure.SunsetAlarmService
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightService
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounterService
import com.kylecorry.trail_sense.weather.infrastructure.alerts.DailyWeatherAlerter
import com.kylecorry.trail_sense.weather.infrastructure.alerts.StormAlerter
import com.kylecorry.trail_sense.weather.infrastructure.commands.CurrentWeatherAlertCommand

class NotificationDiagnostic(private val context: Context) : IDiagnostic {

    override fun scan(): List<DiagnosticCode> {
        val codes = mutableListOf<DiagnosticCode>()

        if (isChannelBlocked(context, FlashlightService.CHANNEL_ID)) {
            codes.add(DiagnosticCode.FlashlightNotificationsBlocked)
        }

        if (isChannelBlocked(context, SunsetAlarmService.NOTIFICATION_CHANNEL_ID)) {
            codes.add(DiagnosticCode.SunsetAlertsBlocked)
        }

        if (isChannelBlocked(context, StormAlerter.STORM_CHANNEL_ID)) {
            codes.add(DiagnosticCode.StormAlertsBlocked)
        }

        if (isChannelBlocked(context, DailyWeatherAlerter.DAILY_CHANNEL_ID)) {
            codes.add(DiagnosticCode.DailyForecastNotificationsBlocked)
        }

        if (isChannelBlocked(context, StepCounterService.CHANNEL_ID) && Sensors.hasSensor(
                context,
                Sensor.TYPE_STEP_COUNTER
            )
        ) {
            codes.add(DiagnosticCode.PedometerNotificationsBlocked)
        }

        if (isChannelBlocked(
                context,
                CurrentWeatherAlertCommand.WEATHER_CHANNEL_ID
            ) && Sensors.hasBarometer(context)
        ) {
            codes.add(DiagnosticCode.WeatherNotificationsBlocked)
        }

        return codes
    }

    /**
     * Determines if a channel is blocked
     */
    private fun isChannelBlocked(context: Context, channelId: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return false
        }

        if (areNotificationsBlocked(context)) {
            return true
        }

        try {
            val channel =
                getNotificationManager(context)?.getNotificationChannel(channelId) ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val groupId = channel.group
                val groupBlocked =
                    getNotificationManager(context)?.getNotificationChannelGroup(groupId)?.isBlocked == true
                if (groupBlocked) {
                    return true
                }
            }

            return channel.importance == NotificationManager.IMPORTANCE_NONE
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Determines if notifications are blocked for the app
     */
    private fun areNotificationsBlocked(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            getNotificationManager(context)?.areNotificationsEnabled() == false
        } else {
            false
        }
    }

    private fun getNotificationManager(context: Context): NotificationManager? {
        return context.getSystemService()
    }

}