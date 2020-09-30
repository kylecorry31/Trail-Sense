package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.kylecorry.trail_sense.weather.infrastructure.receivers.WeatherUpdateReceiver
import java.time.Duration
import java.util.concurrent.TimeUnit

class WeatherUpdateWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        applicationContext.sendBroadcast(WeatherUpdateReceiver.intent(applicationContext))
        return Result.success()
    }

    companion object {
        const val WORK_TAG = "com.kylecorry.trail_sense.WeatherUpdateWorker"

        fun start(
            context: Context,
            interval: Duration
        ) {
            val workManager = WorkManager.getInstance(context.applicationContext)
            stop(context)
            val request = PeriodicWorkRequest.Builder(
                WeatherUpdateWorker::class.java,
                interval.toMinutes(),
                TimeUnit.MINUTES
            ).addTag(WORK_TAG).build()

            workManager.enqueue(request)
        }

        fun stop(context: Context) {
            val workManager = WorkManager.getInstance(context.applicationContext)
            workManager.cancelAllWorkByTag(WORK_TAG)
        }
    }

}