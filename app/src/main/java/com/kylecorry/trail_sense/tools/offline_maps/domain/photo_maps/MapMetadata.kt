package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps

import com.kylecorry.sol.math.geometry.Size

data class MapMetadata(
    val size: Size,
    val unscaledPdfSize: Size?,
    val fileSize: Long,
    val projection: MapProjectionType = MapProjectionType.Mercator,
    val imageSize: Size = size
)
