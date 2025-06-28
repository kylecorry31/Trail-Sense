package com.kylecorry.trail_sense.tools.photo_maps.domain

import com.kylecorry.trail_sense.shared.rotateInRect

internal class PhotoMapRotationService(private val map: PhotoMap) {

    fun getCalibrationPoints(): List<MapCalibrationPoint> {
        val newSize = map.calibratedSize()
        return map.calibration.calibrationPoints.map {
            // Convert to pixels
            val pixel = it.imageLocation.toPixels(map.metadata.size.width, map.metadata.size.height)
            // Rotate it around the center of the image
            val rotated = pixel.rotateInRect(map.calibration.rotation, map.metadata.size)
            // Convert back to percent
            val percent = PercentCoordinate(rotated.x / newSize.width, rotated.y / newSize.height)
            MapCalibrationPoint(it.location, percent)
        }
    }

}