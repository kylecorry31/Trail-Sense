package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps

import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.calibration.MapCalibrationPoint
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.projections.MapProjectionType

data class PhotoMapGeoreference(
    val size: Size,
    val unscaledPdfSize: Size? = null,
    val imageSize: Size = size,
    val projectionType: MapProjectionType = MapProjectionType.Mercator,
    val isWarpingCompleted: Boolean = false,
    val rotation: Float = 0f,
    val calibrationPoints: List<MapCalibrationPoint> = emptyList(),
    val isFullWorld: Boolean = false
)
