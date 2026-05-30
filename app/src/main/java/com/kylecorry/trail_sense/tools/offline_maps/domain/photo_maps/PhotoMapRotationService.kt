package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps

import com.kylecorry.trail_sense.shared.rotateInRect

internal class PhotoMapRotationService(private val map: PhotoMap) {

    fun getCalibrationPoints(usePdf: Boolean = true): List<MapCalibrationPoint> {
        val unrotatedSize = map.unrotatedSize(usePdf)
        val newSize = map.calibratedSize(usePdf)
        return map.calibration.calibrationPoints.map {
            // Convert to pixels
            val pixel = it.imageLocation.toPixels(unrotatedSize.width, unrotatedSize.height)
            // Rotate it around the center of the image
            val rotated = pixel.rotateInRect(map.calibration.rotation, unrotatedSize)
            // Convert back to percent
            val percent = PercentCoordinate(rotated.x / newSize.width, rotated.y / newSize.height)
            MapCalibrationPoint(it.location, percent)
        }
    }

}
