package com.kylecorry.trail_sense.tools.battery.infrastructure

import android.content.Context
import androidx.work.Constraints
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.kylecorry.trailsensecore.infrastructure.tasks.DeferredTaskScheduler
import com.kylecorry.trailsensecore.infrastructure.tasks.ITaskScheduler

class BatteryLogWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        BatteryLogService.start(applicationContext)
        return Result.success()
    }

    companion object {
        private const val WORK_TAG = "com.kylecorry.trail_sense.BatteryLogWorker"

        fun scheduler(context: Context): ITaskScheduler {
            return DeferredTaskScheduler(
                context,
                BatteryLogWorker::class.java,
                WORK_TAG
            )
        }
    }

}