package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import androidx.work.*
import androidx.work.impl.WorkContinuationImpl
import androidx.work.impl.WorkManagerImpl
import com.kylecorry.trail_sense.weather.infrastructure.receivers.WeatherUpdateReceiver
import com.kylecorry.trail_sense.weather.infrastructure.receivers.WeatherUpdateService
import java.time.Duration
import java.util.concurrent.TimeUnit

class WeatherUpdateWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        applicationContext.startService(WeatherUpdateService.intent(applicationContext))
        return Result.success()
    }

    companion object {
        const val WORK_TAG = "com.kylecorry.trail_sense.WeatherUpdateWorker"

        fun start(
            context: Context,
            interval: Duration
        ) {
            val workManager = WorkManager.getInstance(context.applicationContext)

            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresDeviceIdle(false)
                .build()

            val request = PeriodicWorkRequest.Builder(
                WeatherUpdateWorker::class.java,
                interval.toMinutes(),
                TimeUnit.MINUTES
            ).addTag(WORK_TAG).setConstraints(constraints).build()

            workManager.enqueueUniquePeriodicWork(
                WORK_TAG,
                ExistingPeriodicWorkPolicy.REPLACE,
                request
            )
        }

        fun stop(context: Context) {
            val workManager = WorkManager.getInstance(context.applicationContext)
            workManager.cancelUniqueWork(WORK_TAG)
        }
    }

}