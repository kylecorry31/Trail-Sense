package com.kylecorry.trail_sense.weather.infrastructure.alerts

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.SensorManager
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.PressureUnits
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.NavigationUtils
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.alerts.IValueAlerter
import com.kylecorry.trail_sense.weather.infrastructure.CurrentWeather
import com.kylecorry.trail_sense.weather.infrastructure.IWeatherPreferences
import com.kylecorry.trail_sense.weather.infrastructure.WeatherUpdateScheduler
import com.kylecorry.trail_sense.weather.infrastructure.receivers.WeatherStopMonitoringReceiver
import java.time.Instant

class CurrentWeatherAlerter(
    private val context: Context,
    private val formatter: FormatService,
    private val units: PressureUnits,
    private val prefs: IWeatherPreferences
) : IValueAlerter<CurrentWeather> {

    override fun alert(value: CurrentWeather) {
        val forecast = value.prediction
        val tendency = value.pressureTendency
        val lastReading = value.observation?.pressureReading()
        val pressure = (lastReading ?: Reading(
            Pressure.hpa(SensorManager.PRESSURE_STANDARD_ATMOSPHERE),
            Instant.now()
        )).value
        val icon = formatter.getWeatherImage(forecast.primaryHourly)
        val weather = formatter.formatWeather(forecast.primaryHourly)
        val arrival = formatter.formatWeatherArrival(forecast.hourlyArrival).lowercase()

        val description = if (arrival.isNotEmpty()) {
            "$weather $arrival"
        } else {
            weather
        }

        val newNotification = getNotification(
            if (prefs.shouldShowPressureInNotification) context.getString(
                R.string.weather_notification_desc_format,
                description,
                formatter.formatPressure(pressure.hpa(), Units.getDecimalPlaces(units)),
                getTendencyString(tendency, units)
            ) else description,
            icon
        )
        updateNotificationText(newNotification)
    }

    private fun getNotification(text: String, icon: Int): Notification {
        val stopIntent = Intent(context, WeatherStopMonitoringReceiver::class.java)
        val openIntent = NavigationUtils.pendingIntent(context, R.id.action_weather)

        val stopPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(context, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val stopAction = Notify.action(
            context.getString(R.string.stop),
            stopPendingIntent,
            R.drawable.ic_cancel
        )

        val title = context.getString(R.string.weather)

        return Notify.persistent(
            context,
            WEATHER_CHANNEL_ID,
            title,
            text,
            icon,
            showBigIcon = prefs.showColoredNotificationIcon,
            group = NotificationChannels.GROUP_WEATHER,
            intent = openIntent,
            actions = listOf(stopAction)
        )
    }

    private fun getTendencyString(
        tendency: PressureTendency,
        units: PressureUnits
    ): String {
        val pressure = Pressure.hpa(tendency.amount).convertTo(units)
        val formatted = formatter.formatPressure(
            pressure,
            Units.getDecimalPlaces(units) + 1
        )
        return context.getString(R.string.pressure_tendency_format_2, formatted)
    }

    private fun updateNotificationText(notification: Notification) {
        Notify.send(context, WeatherUpdateScheduler.WEATHER_NOTIFICATION_ID, notification)
    }

    companion object {
        const val WEATHER_CHANNEL_ID = "Weather"

        fun getDefaultNotification(context: Context): Notification {
            val stopIntent = Intent(context, WeatherStopMonitoringReceiver::class.java)
            val openIntent = NavigationUtils.pendingIntent(context, R.id.action_weather)

            val stopPendingIntent: PendingIntent =
                PendingIntent.getBroadcast(context, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

            val stopAction = Notify.action(
                context.getString(R.string.stop),
                stopPendingIntent,
                R.drawable.ic_cancel
            )

            val title = context.getString(R.string.weather)

            return Notify.persistent(
                context,
                CurrentWeatherAlerter.WEATHER_CHANNEL_ID,
                title,
                title,
                R.drawable.ic_weather,
                group = NotificationChannels.GROUP_WEATHER,
                intent = openIntent,
                actions = listOf(stopAction),
                showForegroundImmediate = true
            )
        }
    }
}