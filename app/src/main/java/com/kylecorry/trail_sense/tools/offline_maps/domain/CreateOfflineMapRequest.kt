package com.kylecorry.trail_sense.tools.offline_maps.domain

import android.net.Uri
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.calibration.MapCalibrationPoint

data class CreateOfflineMapRequest(
    val uri: Uri,
    val name: String,
    val parentId: Long? = null,
    val photoMapCalibration: List<MapCalibrationPoint>? = null,
    val visible: Boolean = true
)
