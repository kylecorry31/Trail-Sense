package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.commands

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapService

class RebaseMapCalibrationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val service = getAppService<OfflineMapService>()

    override suspend fun doWork(): Result {
        try {
            service.rebasePhotoMapCalibrationsToBaseRotation()
        } catch (e: Exception) {
            // Could not migrate
            return Result.failure()
        }
        return Result.success()
    }
}
