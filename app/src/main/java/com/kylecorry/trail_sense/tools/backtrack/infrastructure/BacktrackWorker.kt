package com.kylecorry.trail_sense.tools.backtrack.infrastructure

import android.content.Context
import androidx.work.*
import com.kylecorry.trail_sense.shared.tasks.DeferredTaskScheduler
import com.kylecorry.trail_sense.shared.tasks.ITaskScheduler
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.services.BacktrackService
import com.kylecorry.trail_sense.weather.infrastructure.services.WeatherUpdateService

class BacktrackWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        BacktrackService.start(applicationContext)
        return Result.success()
    }

    companion object {
        private const val WORK_TAG = "com.kylecorry.trail_sense.BacktrackWorker"

        fun scheduler(context: Context): ITaskScheduler {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()
            return DeferredTaskScheduler(
                context,
                BacktrackWorker::class.java,
                WORK_TAG,
                constraints
            )
        }
    }

}