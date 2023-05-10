package com.kylecorry.trail_sense.tools.maps.infrastructure.commands

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapRepo

class RebaseMapCalibrationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val repo = MapRepo.getInstance(context)
            val maps = repo.getAllMaps()
            maps.forEach {
                if (it.calibration.calibrationPoints.isEmpty()){
                    return@forEach
                }
                // Convert all calibration points to rotation 0
                val points = it.calibration.calibrationPoints.map { point ->
                    point.copy(imageLocation = point.imageLocation.rotate(-it.baseRotation()))
                }
                repo.addMap(it.copy(calibration = it.calibration.copy(calibrationPoints = points)))
            }
        } catch (e: Exception) {
            // Could not migrate
            return Result.failure()
        }
        return Result.success()
    }
}