package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.jobs.DeferredTaskScheduler
import com.kylecorry.andromeda.jobs.ITaskScheduler
import com.kylecorry.trail_sense.weather.infrastructure.services.WeatherUpdateService

class WeatherUpdateWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        WeatherUpdateService.start(applicationContext)
        return Result.success()
    }

    companion object {
        private const val WORK_TAG = "com.kylecorry.trail_sense.WeatherUpdateWorker"

        fun scheduler(context: Context): ITaskScheduler {
            return DeferredTaskScheduler(
                context,
                WeatherUpdateWorker::class.java,
                WORK_TAG
            )
        }
    }

}