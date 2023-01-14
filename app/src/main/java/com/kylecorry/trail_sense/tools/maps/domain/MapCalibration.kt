package com.kylecorry.trail_sense.tools.maps.domain

data class MapCalibration(
    val warped: Boolean,
    val rotated: Boolean,
    val rotation: Int,
    val calibrationPoints: List<MapCalibrationPoint>
) {
    companion object {
        fun uncalibrated(): MapCalibration {
            return MapCalibration(
                warped = false,
                rotated = false,
                rotation = 0,
                calibrationPoints = emptyList()
            )
        }
    }
}