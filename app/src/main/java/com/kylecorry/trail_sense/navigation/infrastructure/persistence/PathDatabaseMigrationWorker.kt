package com.kylecorry.trail_sense.navigation.infrastructure.persistence

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kylecorry.trail_sense.navigation.infrastructure.NavigationPreferences

class PathDatabaseMigrationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            val prefs = NavigationPreferences(context)
            val pathService = PathService.getInstance(context)
            MigrateBacktrackPathsCommand(pathService, prefs).execute()
        } catch (e: Exception) {
            // Could not migrate
            return Result.failure()
        }
        return Result.success()
    }

}