package com.kylecorry.trail_sense.tools.backtrack.infrastructure

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.jobs.ITaskScheduler
import com.kylecorry.andromeda.jobs.TaskSchedulerFactory
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.commands.BacktrackCommand
import java.time.LocalDateTime


class BacktrackWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Log.d(javaClass.simpleName, "Started")
        try {
            BacktrackCommand(applicationContext).execute()
        } catch (e: Exception) {
            throw e
        } finally {
            val frequency = UserPreferences(applicationContext).backtrackRecordFrequency
            scheduler(applicationContext).schedule(frequency)
            Log.d(
                javaClass.simpleName,
                "Scheduled next run at ${LocalDateTime.now().plus(frequency)}"
            )

        }
        return Result.success()
    }

    companion object {
//        private const val WORK_TAG = "com.kylecorry.trail_sense.BacktrackWorker"

        fun scheduler(context: Context): ITaskScheduler {
            return TaskSchedulerFactory(context).deferrable(
                BacktrackWorker::class.java,
                7238542
            )
        }
    }

}