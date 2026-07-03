package com.kylecorry.trail_sense.tools.offline_maps.domain

import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.persistence.MapRepo

class OfflineMapCalibrationMigrator(private val repo: MapRepo) {
    suspend fun migrate() {
        repo.getPhotoMaps().forEach {
            if (it.georeference.calibrationPoints.isEmpty()) {
                return@forEach
            }
            val points = it.georeference.calibrationPoints.map { point ->
                point.copy(imageLocation = point.imageLocation.rotate(-it.baseRotation()))
            }
            val updated = it.copy(
                georeference = it.georeference.copy(calibrationPoints = points)
            )
            repo.add(updated)
        }
    }
}
