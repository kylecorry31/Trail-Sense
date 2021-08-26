package com.kylecorry.trail_sense.weather.infrastructure.persistence

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class PressureDatabaseMigrationWorker(private val context: Context,
                                      workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            context.deleteDatabase("weather")
            context.deleteFile("pressure.csv")
        } catch (e: Exception){
            // Do nothing - could not migrate DB, so user will lose their pressure history
        }
        return Result.success()
    }

}