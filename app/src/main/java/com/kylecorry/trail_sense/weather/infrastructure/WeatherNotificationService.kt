package com.kylecorry.trail_sense.weather.infrastructure

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import androidx.core.content.getSystemService
import com.kylecorry.trail_sense.MainActivity
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.SystemUtils
import com.kylecorry.trail_sense.weather.domain.forcasting.Weather

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
            stopPendingIntent)
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
                .setContentIntent(openPendingIntent)
                .build()
        }
    }

    fun updateNotificationForecast(context: Context, forecast: Weather){
        val icon = when (forecast) {
            Weather.ImprovingFast -> R.drawable.sunny
            Weather.ImprovingSlow -> R.drawable.partially_cloudy
            Weather.WorseningSlow -> R.drawable.cloudy
            Weather.WorseningFast -> R.drawable.light_rain
            Weather.Storm -> R.drawable.storm
            else -> R.drawable.steady
        }

        val description = context.getString(when (forecast) {
            Weather.ImprovingFast -> R.string.weather_improving_fast
            Weather.ImprovingSlow -> R.string.weather_improving_slow
            Weather.WorseningSlow -> R.string.weather_worsening_slow
            Weather.WorseningFast -> R.string.weather_worsening_fast
            Weather.Storm -> R.string.weather_storm_incoming
            else -> R.string.weather_not_changing
        })

        val newNotification = getNotification(context, description, icon)
        updateNotificationText(context, newNotification)
    }

    private fun updateNotificationText(context: Context, notification: Notification){
        SystemUtils.sendNotification(context, WEATHER_NOTIFICATION_ID, notification)
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