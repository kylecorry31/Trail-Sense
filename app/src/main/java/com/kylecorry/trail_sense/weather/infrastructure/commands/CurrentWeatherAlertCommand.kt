package com.kylecorry.trail_sense.weather.infrastructure.commands

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.SensorManager
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.sol.science.meteorology.Weather
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.PressureUnits
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.NavigationUtils
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.weather.domain.PressureReading
import com.kylecorry.trail_sense.weather.infrastructure.WeatherUpdateScheduler
import com.kylecorry.trail_sense.weather.infrastructure.receivers.WeatherStopMonitoringReceiver
import java.time.Instant

class CurrentWeatherAlertCommand(
    private val context: Context,
    private val hourly: Weather,
    private val tendency: PressureTendency,
    private val lastReading: PressureReading?
) : IWeatherAlertCommand {

    private val prefs by lazy { UserPreferences(context) }
    private val formatService by lazy { FormatService(context) }

    override fun execute() {
        if (prefs.weather.shouldShowWeatherNotification) {
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
            context.getString(R.string.stop_monitoring),
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
        forecast: Weather,
        tendency: PressureTendency,
        lastReading: PressureReading?
    ) {
        val units = prefs.pressureUnits
        val pressure = lastReading ?: PressureReading(
            Instant.now(),
            SensorManager.PRESSURE_STANDARD_ATMOSPHERE
        )
        val icon = when (forecast) {
            Weather.ImprovingFast -> if (pressure.isLow()) R.drawable.cloudy else R.drawable.sunny
            Weather.ImprovingSlow -> if (pressure.isHigh()) R.drawable.sunny else R.drawable.partially_cloudy
            Weather.WorseningSlow -> if (pressure.isLow()) R.drawable.light_rain else R.drawable.cloudy
            Weather.WorseningFast -> if (pressure.isLow()) R.drawable.heavy_rain else R.drawable.light_rain
            Weather.Storm -> R.drawable.storm
            else -> R.drawable.steady
        }

        val description = formatService.formatShortTermWeather(
            forecast
        )

        val newNotification = getNotification(
            if (prefs.weather.shouldShowPressureInNotification) context.getString(
                R.string.weather_notification_desc_format,
                description,
                getPressureString(pressure.value, units),
                getTendencyString(tendency, units)
            ) else description,
            icon
        )
        updateNotificationText(newNotification)
    }

    private fun getPressureString(
        pressure: Float?,
        units: PressureUnits
    ): String {
        if (pressure == null) {
            return "?"
        }
        val p = Pressure(pressure, PressureUnits.Hpa).convertTo(units)
        return formatService.formatPressure(p, Units.getDecimalPlaces(units))
    }

    private fun getTendencyString(
        tendency: PressureTendency,
        units: PressureUnits
    ): String {
        val pressure = Pressure(tendency.amount, PressureUnits.Hpa).convertTo(units)
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
    }

}