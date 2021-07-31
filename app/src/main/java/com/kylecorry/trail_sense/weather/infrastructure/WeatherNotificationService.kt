package com.kylecorry.trail_sense.weather.infrastructure

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.SensorManager
import com.kylecorry.notify.Notify
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.NavigationUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.weather.domain.PressureUnitUtils
import com.kylecorry.trail_sense.weather.infrastructure.receivers.WeatherStopMonitoringReceiver
import com.kylecorry.trail_sense.weather.infrastructure.services.WeatherUpdateService
import com.kylecorry.trailsensecore.domain.units.PressureUnits
import com.kylecorry.trailsensecore.domain.weather.PressureReading
import com.kylecorry.trailsensecore.domain.weather.PressureTendency
import com.kylecorry.trailsensecore.domain.weather.Weather
import java.time.Instant

object WeatherNotificationService {

    const val WEATHER_NOTIFICATION_ID = 1

    fun getNotification(context: Context, text: String, icon: Int): Notification {
        val stopIntent = Intent(context, WeatherStopMonitoringReceiver::class.java)
        val openIntent = NavigationUtils.pendingIntent(context, R.id.action_weather)
        val prefs = UserPreferences(context)

        val stopPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(context, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val notify = Notify(context)

        val stopAction = notify.action(
            context.getString(R.string.stop_monitoring),
            stopPendingIntent,
            R.drawable.ic_cancel
        )

        val title = context.getString(R.string.weather)

        return notify.persistent(
            WeatherUpdateService.WEATHER_CHANNEL_ID,
            title,
            text,
            icon,
            showBigIcon = prefs.weather.showColoredNotificationIcon,
            group = NotificationChannels.GROUP_WEATHER,
            intent = openIntent,
            actions = listOf(stopAction)
        )
    }

    fun updateNotificationForecast(
        context: Context,
        forecast: Weather,
        tendency: PressureTendency,
        lastReading: PressureReading?
    ) {
        val prefs = UserPreferences(context)
        val units = prefs.pressureUnits
        val formatService = FormatServiceV2(context)
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

        val description = formatService.formatShortTermWeather(forecast, prefs.weather.useRelativeWeatherPredictions)

        val newNotification = getNotification(
            context,
            if (prefs.weather.shouldShowPressureInNotification) context.getString(
                R.string.weather_notification_desc_format,
                description,
                getPressureString(context, pressure.value, units),
                getTendencyString(context, tendency, units)
            ) else description,
            icon
        )
        updateNotificationText(context, newNotification)
    }

    private fun getPressureString(
        context: Context,
        pressure: Float?,
        units: PressureUnits
    ): String {
        if (pressure == null) {
            return "?"
        }
        val symbol = getPressureUnitString(context, units)
        val format = PressureUnitUtils.getDecimalFormat(units)
        return context.getString(
            R.string.pressure_format,
            format.format(PressureUnitUtils.convert(pressure, units)),
            symbol
        )
    }

    private fun getTendencyString(
        context: Context,
        tendency: PressureTendency,
        units: PressureUnits
    ): String {
        val symbol = getPressureUnitString(context, units)
        val format = PressureUnitUtils.getTendencyDecimalFormat(units)
        val formattedTendencyAmount =
            format.format(PressureUnitUtils.convert(tendency.amount, units))
        return context.getString(R.string.pressure_tendency_format, formattedTendencyAmount, symbol)
    }

    private fun getPressureUnitString(context: Context, unit: PressureUnits): String {
        return when (unit) {
            PressureUnits.Hpa -> context.getString(R.string.units_hpa)
            PressureUnits.Mbar -> context.getString(R.string.units_mbar)
            PressureUnits.Inhg -> context.getString(R.string.units_inhg_short)
            PressureUnits.Psi -> context.getString(R.string.units_psi)
        }
    }

    private fun updateNotificationText(context: Context, notification: Notification) {
        val notify = Notify(context)
        notify.send(WEATHER_NOTIFICATION_ID, notification)
    }

}