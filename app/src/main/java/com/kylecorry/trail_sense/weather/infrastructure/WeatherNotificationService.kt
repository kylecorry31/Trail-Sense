package com.kylecorry.trail_sense.weather.infrastructure

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.getSystemService
import com.kylecorry.trail_sense.MainActivity
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.system.NotificationUtils
import com.kylecorry.trail_sense.weather.domain.PressureReading
import com.kylecorry.trail_sense.weather.domain.PressureUnitUtils
import com.kylecorry.trail_sense.weather.domain.PressureUnits
import com.kylecorry.trail_sense.weather.domain.WeatherService
import com.kylecorry.trail_sense.weather.domain.classifier.PressureClassification
import com.kylecorry.trail_sense.weather.domain.forcasting.Weather
import com.kylecorry.trail_sense.weather.domain.tendency.PressureTendency
import com.kylecorry.trail_sense.weather.infrastructure.receivers.WeatherStopMonitoringReceiver

object WeatherNotificationService {

    const val WEATHER_NOTIFICATION_ID = 1

    fun getNotification(context: Context, text: String, icon: Int): Notification {
        createNotificationChannel(context)

        val stopIntent = Intent(context, WeatherStopMonitoringReceiver::class.java)
        val openIntent = MainActivity.weatherIntent(context)

        val stopPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(context, 0, stopIntent, 0)
        val openPendingIntent: PendingIntent =
            PendingIntent.getActivity(context, 0, openIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        val stopAction = Notification.Action.Builder(
            Icon.createWithResource("", R.drawable.ic_cancel),
            context.getString(R.string.stop_monitoring),
            stopPendingIntent
        )
            .build()

        val title = context.getString(R.string.action_weather)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, "Weather")
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(icon)
                .addAction(stopAction)
                .setOnlyAlertOnce(true)
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentIntent(openPendingIntent)
                .build()
        } else {
            Notification.Builder(context)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(icon)
                .addAction(stopAction)
                .setOnlyAlertOnce(true)
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentIntent(openPendingIntent)
                .build()
        }
    }

    fun updateNotificationForecast(
        context: Context,
        forecast: Weather,
        readings: List<PressureReading>
    ) {
        val weatherService = WeatherService(0f, 0f, 0f)
        val tendency = weatherService.getTendency(readings)
        val prefs = UserPreferences(context)
        val units = prefs.pressureUnits
        val pressure = readings.lastOrNull()?.value
        val classification = weatherService.classifyPressure(
            pressure ?: SensorManager.PRESSURE_STANDARD_ATMOSPHERE
        )

        val icon = when (forecast) {
            Weather.ImprovingFast -> if (classification == PressureClassification.Low) R.drawable.cloudy else R.drawable.sunny
            Weather.ImprovingSlow -> if (classification == PressureClassification.High) R.drawable.sunny else R.drawable.partially_cloudy
            Weather.WorseningSlow -> if (classification == PressureClassification.Low) R.drawable.light_rain else R.drawable.cloudy
            Weather.WorseningFast -> if (classification == PressureClassification.Low) R.drawable.heavy_rain else R.drawable.light_rain
            Weather.Storm -> R.drawable.storm
            else -> R.drawable.steady
        }

        val description = context.getString(
            when (forecast) {
                Weather.ImprovingFast -> R.string.weather_improving_fast
                Weather.ImprovingSlow -> R.string.weather_improving_slow
                Weather.WorseningSlow -> R.string.weather_worsening_slow
                Weather.WorseningFast -> R.string.weather_worsening_fast
                Weather.Storm -> R.string.weather_storm_incoming
                else -> R.string.weather_not_changing
            }
        )

        val newNotification = getNotification(
            context,
            if (prefs.weather.shouldShowPressureInNotification) context.getString(
                R.string.weather_notification_desc_format,
                description,
                getPressureString(context, pressure, units),
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
        val format = PressureUnitUtils.getDecimalFormat(units)
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
        NotificationUtils.send(context, WEATHER_NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.action_weather)
            val descriptionText = context.getString(R.string.notification_monitoring_weather)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel("Weather", name, importance).apply {
                description = descriptionText
                enableVibration(false)
                setShowBadge(false)
            }
            val notificationManager = context.getSystemService<NotificationManager>()
            notificationManager?.createNotificationChannel(channel)
        }
    }

}