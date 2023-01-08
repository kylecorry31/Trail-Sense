package com.kylecorry.trail_sense.weather.infrastructure.alerts

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.PressureUnits
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.NavigationUtils
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.alerts.IValueAlerter
import com.kylecorry.trail_sense.weather.domain.CurrentWeather
import com.kylecorry.trail_sense.weather.infrastructure.IWeatherPreferences
import com.kylecorry.trail_sense.weather.infrastructure.WeatherUpdateScheduler
import com.kylecorry.trail_sense.weather.infrastructure.receivers.WeatherStopMonitoringReceiver

class CurrentWeatherAlerter(
    private val context: Context,
    private val formatter: FormatService,
    private val pressureUnits: PressureUnits,
    private val temperatureUnits: TemperatureUnits,
    private val prefs: IWeatherPreferences
) : IValueAlerter<CurrentWeather> {

    override fun alert(value: CurrentWeather) {
        val forecast = value.prediction
        val tendency = value.pressureTendency
        val lastPressure = value.observation?.pressureReading()?.value?.convertTo(pressureUnits)
        val lastTemperature = value.observation?.temperature?.convertTo(temperatureUnits)
        val icon = formatter.getWeatherImage(forecast.primaryHourly)
        val weather = formatter.formatWeather(forecast.primaryHourly)
        val arrival = formatter.formatWeatherArrival(forecast.hourlyArrival)

        val descriptionStringBuilder = StringBuilder()
        descriptionStringBuilder.append(weather)

        if (arrival.isNotEmpty()) {
            descriptionStringBuilder.append(" $arrival")
        }

        if (prefs.shouldShowTemperatureInNotification && lastTemperature != null) {
            descriptionStringBuilder.append(" ${context.getString(R.string.dot)} ")
            descriptionStringBuilder.append(formatter.formatTemperature(lastTemperature))
        }

        if (prefs.shouldShowPressureInNotification && lastPressure != null) {
            descriptionStringBuilder.append(" ${context.getString(R.string.dot)} ")
            descriptionStringBuilder.append(
                formatter.formatPressure(lastPressure, Units.getDecimalPlaces(pressureUnits))
            )
            descriptionStringBuilder.append(" (${getTendencyString(tendency, pressureUnits)})")
        }

        val newNotification = getNotification(
            descriptionStringBuilder.toString(),
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