package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration.selection

import com.kylecorry.sol.math.geometry.Polygon

data class QuadrilateralSelectionCriteria(val quadrilateral: Polygon, val imageWidth: Int, val imageHeight: Int)
