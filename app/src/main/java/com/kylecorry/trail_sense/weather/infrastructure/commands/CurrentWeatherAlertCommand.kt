package com.kylecorry.trail_sense.weather.infrastructure.commands

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
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.weather.infrastructure.CurrentWeather
import com.kylecorry.trail_sense.weather.infrastructure.WeatherPrediction
import com.kylecorry.trail_sense.weather.infrastructure.WeatherUpdateScheduler
import com.kylecorry.trail_sense.weather.infrastructure.receivers.WeatherStopMonitoringReceiver
import java.time.Instant

class CurrentWeatherAlertCommand(
    private val context: Context
) : IWeatherAlertCommand {

    private val prefs by lazy { UserPreferences(context) }
    private val formatService by lazy { FormatService(context) }

    override fun execute(weather: CurrentWeather) {

        val hourly = weather.prediction
        val tendency = weather.pressureTendency
        val lastReading = weather.observation?.pressureReading()

        if (prefs.weather.shouldShowWeatherNotification && prefs.weather.shouldMonitorWeather) {
            updateNotificationForecast(
                hourly,
                tendency,
                lastReading
            )
        }
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
            showBigIcon = prefs.weather.showColoredNotificationIcon,
            group = NotificationChannels.GROUP_WEATHER,
            intent = openIntent,
            actions = listOf(stopAction)
        )
    }

    private fun updateNotificationForecast(
        forecast: WeatherPrediction,
        tendency: PressureTendency,
        lastReading: Reading<Pressure>?
    ) {
        val units = prefs.pressureUnits
        val pressure = (lastReading ?: Reading(
            Pressure.hpa(SensorManager.PRESSURE_STANDARD_ATMOSPHERE),
            Instant.now()
        )).value
        val icon = formatService.getWeatherImage(forecast.primaryHourly)
        val weather = formatService.formatWeather(forecast.primaryHourly)
        val speed = formatService.formatWeatherSpeed(forecast.hourlyArrival).lowercase()

        val description = if (speed.isNotEmpty()) {
            "$weather $speed"
        } else {
            weather
        }

        val newNotification = getNotification(
            if (prefs.weather.shouldShowPressureInNotification) context.getString(
                R.string.weather_notification_desc_format,
                description,
                formatService.formatPressure(pressure.hpa(), Units.getDecimalPlaces(units)),
                getTendencyString(tendency, units)
            ) else description,
            icon
        )
        updateNotificationText(newNotification)
    }

    private fun getTendencyString(
        tendency: PressureTendency,
        units: PressureUnits
    ): String {
        val pressure = Pressure.hpa(tendency.amount).convertTo(units)
        val formatted = formatService.formatPressure(
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
                WEATHER_CHANNEL_ID,
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