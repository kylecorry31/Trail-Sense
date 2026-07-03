package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.commands

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapCalibrationMigrator

class RebaseMapCalibrationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val migration = OfflineMapCalibrationMigrator(getAppService())

    override suspend fun doWork(): Result {
        try {
            migration.migrate()
        } catch (_: Exception) {
            // Could not migrate
            return Result.failure()
        }
        return Result.success()
    }
}
