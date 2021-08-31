package com.kylecorry.trail_sense.tools.battery.infrastructure

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.jobs.ITaskScheduler
import com.kylecorry.andromeda.jobs.WorkTaskScheduler
import com.kylecorry.trail_sense.tools.battery.infrastructure.commands.BatteryLogCommand
import java.time.Duration
import java.time.LocalDateTime

class BatteryLogWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Log.d(javaClass.simpleName, "Started")
        try {
            BatteryLogCommand(applicationContext).execute()
        } catch (e: Exception) {
            throw e
        } finally {
            scheduler(applicationContext).schedule(Duration.ofHours(1))
            Log.d(javaClass.simpleName, "Scheduled next run at ${LocalDateTime.now().plusHours(1)}")
        }
        return Result.success()
    }

    companion object {
        private const val WORK_TAG = "com.kylecorry.trail_sense.BatteryLogWorker"

        fun scheduler(context: Context): ITaskScheduler {
            return WorkTaskScheduler(
                context,
                BatteryLogWorker::class.java,
                WORK_TAG,
                false
            )
        }
    }

}