package com.kylecorry.trail_sense.tools.maps.domain

import com.kylecorry.andromeda.core.units.PixelCoordinate

data class MapCalibration(
    val corners: PixelBounds,
    val rotation: Float,
    val calibrationPoints: List<MapCalibrationPoint>
) {
    companion object {
        fun default(width: Float, height: Float): MapCalibration {
            return MapCalibration(
                PixelBounds(
                    PixelCoordinate(0f, 0f),
                    PixelCoordinate(width, 0f),
                    PixelCoordinate(0f, height),
                    PixelCoordinate(width, height)
                ),
                0f,
                emptyList()
            )
        }
    }
}