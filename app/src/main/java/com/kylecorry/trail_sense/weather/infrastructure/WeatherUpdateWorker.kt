package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import androidx.work.*
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.weather.infrastructure.service.WeatherUpdateService
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer
import java.time.Duration
import java.util.concurrent.TimeUnit

class WeatherUpdateWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        applicationContext.startService(WeatherUpdateService.intent(applicationContext))
        val prefs = UserPreferences(applicationContext)
        val intervalometer = Intervalometer {
            start(applicationContext, prefs.weather.weatherUpdateFrequency)
        }
        intervalometer.once(Duration.ofSeconds(1))
        return Result.success()
    }

    companion object {
        private const val WORK_TAG = "com.kylecorry.trail_sense.WeatherUpdateWorker"

        fun start(
            context: Context,
            interval: Duration
        ) {
            val workManager = WorkManager.getInstance(context.applicationContext)

            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val request = OneTimeWorkRequest
                .Builder(WeatherUpdateWorker::class.java)
                .addTag(WORK_TAG)
                .setInitialDelay(interval.toMillis(), TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniqueWork(
                WORK_TAG,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun stop(context: Context) {
            val workManager = WorkManager.getInstance(context.applicationContext)
            workManager.cancelUniqueWork(WORK_TAG)
        }
    }

}