package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.jobs.IntervalWorker
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.Background
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.weather.infrastructure.subsystem.WeatherSubsystem
import java.time.Duration

class WeatherUpdateWorker(context: Context, params: WorkerParameters) :
    IntervalWorker(context, params, wakelockDuration = Duration.ofSeconds(15)) {

    override val uniqueId: Int = Background.WeatherMonitor

    override fun isEnabled(context: Context): Boolean {
        return UserPreferences(context).weather.shouldMonitorWeather
    }

    override fun getFrequency(context: Context): Duration {
        return UserPreferences(context).weather.weatherUpdateFrequency
    }

    override suspend fun execute(context: Context) {
        WeatherSubsystem.getInstance(context).updateWeather(true)
    }

    override fun getForegroundInfo(context: Context): ForegroundInfo {
        return ForegroundInfo(
            37892,
            Notify.background(
                applicationContext,
                NotificationChannels.CHANNEL_BACKGROUND_UPDATES,
                applicationContext.getString(R.string.notification_weather_update_title),
                applicationContext.getString(R.string.updating_weather),
                R.drawable.ic_update
            ),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                0
            }
        )
    }

}