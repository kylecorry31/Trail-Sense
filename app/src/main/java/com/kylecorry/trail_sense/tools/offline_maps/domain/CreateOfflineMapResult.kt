package com.kylecorry.trail_sense.tools.offline_maps.domain

data class CreateOfflineMapResult(
    val map: OfflineMap,
    val autoCalibrated: Boolean
)
