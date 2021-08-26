package com.kylecorry.trail_sense.weather.infrastructure.persistence

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.core.tryOrNothing

class PressureDatabaseMigrationWorker(private val context: Context,
                                      workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        tryOrNothing {
            context.deleteDatabase("weather")
            context.deleteFile("pressure.csv")
        }
        return Result.success()
    }

}