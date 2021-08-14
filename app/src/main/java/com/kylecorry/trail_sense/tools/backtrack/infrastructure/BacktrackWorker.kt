package com.kylecorry.trail_sense.tools.backtrack.infrastructure

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.jobs.DeferredTaskScheduler
import com.kylecorry.andromeda.jobs.ITaskScheduler
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.services.BacktrackService


class BacktrackWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        BacktrackService.start(applicationContext)
        return Result.success()
    }

    companion object {
        private const val WORK_TAG = "com.kylecorry.trail_sense.BacktrackWorker"

        fun scheduler(context: Context): ITaskScheduler {
            return DeferredTaskScheduler(
                context,
                BacktrackWorker::class.java,
                WORK_TAG)
        }
    }

}