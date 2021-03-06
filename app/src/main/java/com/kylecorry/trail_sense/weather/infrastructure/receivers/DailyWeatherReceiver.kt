package com.kylecorry.trail_sense.weather.infrastructure.receivers

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import com.kylecorry.trail_sense.MainActivity
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.infrastructure.receivers.SunsetAlarmReceiver
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.weather.domain.WeatherService
import com.kylecorry.trail_sense.weather.infrastructure.PressureCalibrationUtils
import com.kylecorry.trail_sense.weather.infrastructure.persistence.PressureRepo
import com.kylecorry.trailsensecore.domain.weather.Weather
import com.kylecorry.trailsensecore.infrastructure.system.AlarmUtils
import com.kylecorry.trailsensecore.infrastructure.system.NotificationUtils
import com.kylecorry.trailsensecore.infrastructure.system.PackageUtils
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class DailyWeatherReceiver : BroadcastReceiver() {

    private lateinit var context: Context
    private val pressureRepo by lazy { PressureRepo.getInstance(context) }
    private val prefs by lazy { UserPreferences(context) }
    private val weatherService by lazy {
        WeatherService(
            prefs.weather.stormAlertThreshold,
            prefs.weather.dailyForecastChangeThreshold,
            prefs.weather.hourlyForecastChangeThreshold,
            prefs.weather.seaLevelFactorInRapidChanges,
            prefs.weather.seaLevelFactorInTemp
        )
    }
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)


    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        this.context = context

        setAlarm(LocalDate.now().plusDays(1).atTime(sendTime))

        val lastSentDate = prefs.weather.dailyWeatherLastSent
        if (LocalDate.now() == lastSentDate) {
            return
        }

        prefs.weather.dailyWeatherLastSent = LocalDate.now()

        serviceScope.launch {
            val readings = withContext(Dispatchers.IO) {
                pressureRepo.getPressuresSync().map { it.toPressureAltitudeReading() }
            }
            val calibrated = PressureCalibrationUtils.calibratePressures(context, readings)

            val dailyForecast = weatherService.getDailyWeather(calibrated)

            sendNotification(dailyForecast)
        }
    }

    private fun sendNotification(forecast: Weather) {
        val icon = when (forecast) {
            Weather.ImprovingSlow -> R.drawable.sunny
            Weather.WorseningSlow -> R.drawable.light_rain
            else -> R.drawable.steady
        }

        val description = when (forecast) {
            Weather.ImprovingSlow -> context.getString(R.string.weather_better_than_yesterday)
            Weather.WorseningSlow -> context.getString(R.string.weather_worse_than_yesterday)
            else -> context.getString(R.string.weather_same_as_yesterday)
        }

        val openIntent = MainActivity.weatherIntent(context)
        val openPendingIntent: PendingIntent =
            PendingIntent.getActivity(context, 0, openIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        val builder = NotificationUtils.builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.todays_forecast))
            .setContentText(description)
            .setSmallIcon(icon)
            .setLargeIcon(Icon.createWithResource(context, icon))
            .setAutoCancel(false)
            .setContentIntent(openPendingIntent)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            builder.setPriority(Notification.PRIORITY_LOW)
        }

        val notification = builder.build()

        NotificationUtils.send(context, NOTIFICATION_ID, notification)
    }

    private fun setAlarm(time: LocalDateTime) {
        cancel(context)
        val newPi = pendingIntent(context)
        AlarmUtils.set(context, time, newPi, exact = true, allowWhileIdle = true)
        Log.i(TAG, "Set next daily forecast alarm at $time")
    }

    companion object {
        const val CHANNEL_ID = "daily-weather"
        private const val NOTIFICATION_ID = 798643
        private const val PI_ID = 28703
        private const val TAG = "DailyWeatherReceiver"

        // TODO: Allow user customization of the time
        private val sendTime = LocalTime.of(9, 0)

        fun intent(context: Context): Intent {
            return Intent(context, DailyWeatherReceiver::class.java)
        }

        private fun alarmIntent(context: Context): Intent {
            val i = Intent("com.kylecorry.trail_sense.DAILY_WEATHER")
            i.`package` = PackageUtils.getPackageName(context)
            i.addCategory("android.intent.category.DEFAULT")
            return i
        }

        fun pendingIntent(context: Context): PendingIntent {
            return PendingIntent.getBroadcast(
                context, PI_ID, alarmIntent(context), PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        fun schedule(context: Context){
            cancel(context)
            val newPi = pendingIntent(context)
            val time = if (LocalTime.now() > sendTime){
                LocalDate.now().atTime(sendTime)
            } else {
                LocalDate.now().plusDays(1).atTime(sendTime)
            }
            AlarmUtils.set(context, time, newPi, exact = true, allowWhileIdle = true)
        }

        fun cancel(context: Context){
            val lastPi = pendingIntent(context)
            AlarmUtils.cancel(context, lastPi)
        }

    }

}