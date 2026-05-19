package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.commands

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService

class RebaseMapCalibrationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val service = getAppService<MapService>()

    override suspend fun doWork(): Result {
        try {
            val maps = service.getAllPhotoMaps()
            maps.forEach {
                if (it.calibration.calibrationPoints.isEmpty()) {
                    return@forEach
                }
                // Convert all calibration points to rotation 0
                val points = it.calibration.calibrationPoints.map { point ->
                    point.copy(imageLocation = point.imageLocation.rotate(-it.baseRotation()))
                }
                service.add(it.copy(calibration = it.calibration.copy(calibrationPoints = points)))
            }
        } catch (e: Exception) {
            // Could not migrate
            return Result.failure()
        }
        return Result.success()
    }
}
