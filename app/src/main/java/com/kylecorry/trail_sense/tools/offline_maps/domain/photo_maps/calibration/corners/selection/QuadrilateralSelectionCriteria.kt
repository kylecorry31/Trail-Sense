package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.calibration.corners.selection

import com.kylecorry.sol.math.geometry.Polygon

internal data class QuadrilateralSelectionCriteria(
    val quadrilateral: Polygon,
    val imageWidth: Int,
    val imageHeight: Int
)
