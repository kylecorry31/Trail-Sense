package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import androidx.work.*
import com.kylecorry.trail_sense.weather.infrastructure.services.WeatherUpdateService
import com.kylecorry.trailsensecore.infrastructure.tasks.DeferredTaskScheduler
import com.kylecorry.trailsensecore.infrastructure.tasks.ITaskScheduler

class WeatherUpdateWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        WeatherUpdateService.start(applicationContext)
        return Result.success()
    }

    companion object {
        private const val WORK_TAG = "com.kylecorry.trail_sense.WeatherUpdateWorker"

        fun scheduler(context: Context): ITaskScheduler {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()
            return DeferredTaskScheduler(
                context,
                WeatherUpdateWorker::class.java,
                WORK_TAG,
                constraints
            )
        }
    }

}