package com.kylecorry.trail_sense.tools.photo_maps.domain

data class MapCalibration(
    val warped: Boolean,
    val rotated: Boolean,
    val rotation: Float,
    val calibrationPoints: List<MapCalibrationPoint>
) {
    companion object {
        fun uncalibrated(): MapCalibration {
            return MapCalibration(
                warped = false,
                rotated = false,
                rotation = 0f,
                calibrationPoints = emptyList()
            )
        }
    }
}