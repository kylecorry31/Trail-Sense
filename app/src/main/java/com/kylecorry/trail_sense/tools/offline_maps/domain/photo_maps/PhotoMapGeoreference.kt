package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps

import com.kylecorry.sol.math.geometry.Size

data class PhotoMapGeoreference(
    val size: Size,
    val unscaledPdfSize: Size?,
    val projection: MapProjectionType = MapProjectionType.Mercator,
    val imageSize: Size = size,
    val isWarped: Boolean = false,
    val isRotated: Boolean = false,
    val rotation: Float = 0f,
    val calibrationPoints: List<MapCalibrationPoint> = emptyList(),
    val isFullWorld: Boolean = false
)
