package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps

import com.kylecorry.sol.math.arithmetic.Arithmetic
import com.kylecorry.sol.math.geometry.Size

object MapCalibrationValidator {

    private const val MIN_METERS_PER_PIXEL = 0.01f
    private const val MAX_METERS_PER_PIXEL = 100000f

    fun validate(map: PhotoMap): MapCalibrationValidationResult {
        return when {
            map.isFullWorld -> MapCalibrationValidationResult.Valid
            !hasCalibrationData(map) -> MapCalibrationValidationResult.Uncalibrated
            else -> validate(map.calibration, map.calibratedSize())
        }
    }

    private fun hasCalibrationData(map: PhotoMap): Boolean {
        return map.calibration.calibrationPoints.size >= 2 &&
                map.metadata.size.width > 0 &&
                map.metadata.size.height > 0
    }

    private fun validate(
        calibration: MapCalibration,
        calibratedSize: Size
    ): MapCalibrationValidationResult {
        val points = calibration.calibrationPoints
        return when {
            isSamePixel(points[0], points[1], calibratedSize) -> MapCalibrationValidationResult.SamePixel
            Arithmetic.isZero(points[0].location.distanceTo(points[1].location)) -> MapCalibrationValidationResult.SameLocation
            isSamePixelAxis(points[0], points[1], calibratedSize) -> MapCalibrationValidationResult.SameImageAxis
            hasPlausibleScale(points[0], points[1], calibratedSize) -> MapCalibrationValidationResult.Valid
            else -> MapCalibrationValidationResult.ImplausibleScale
        }
    }

    private fun hasPlausibleScale(
        first: MapCalibrationPoint,
        second: MapCalibrationPoint,
        size: Size
    ): Boolean {
        val meters = first.location.distanceTo(second.location)
        val pixels = pixelDistance(first, second, size)
        val metersPerPixel = meters / pixels
        return metersPerPixel in MIN_METERS_PER_PIXEL..MAX_METERS_PER_PIXEL
    }

    private fun isSamePixel(
        first: MapCalibrationPoint,
        second: MapCalibrationPoint,
        size: Size
    ): Boolean {
        val firstPixel = first.imageLocation.toPixels(size.width, size.height)
        val secondPixel = second.imageLocation.toPixels(size.width, size.height)
        return firstPixel.x.toInt() == secondPixel.x.toInt() &&
                firstPixel.y.toInt() == secondPixel.y.toInt()
    }

    private fun isSamePixelAxis(
        first: MapCalibrationPoint,
        second: MapCalibrationPoint,
        size: Size
    ): Boolean {
        val firstPixel = first.imageLocation.toPixels(size.width, size.height)
        val secondPixel = second.imageLocation.toPixels(size.width, size.height)
        return firstPixel.x.toInt() == secondPixel.x.toInt() ||
                firstPixel.y.toInt() == secondPixel.y.toInt()
    }

    private fun pixelDistance(
        first: MapCalibrationPoint,
        second: MapCalibrationPoint,
        size: Size
    ): Float {
        val firstPixel = first.imageLocation.toPixels(size.width, size.height)
        val secondPixel = second.imageLocation.toPixels(size.width, size.height)
        return firstPixel.distanceTo(secondPixel)
    }
}
