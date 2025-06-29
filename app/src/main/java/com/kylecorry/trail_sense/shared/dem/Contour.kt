package com.kylecorry.trail_sense.shared.dem

import com.kylecorry.sol.units.Coordinate

data class Contour(
    val elevation: Float,
    val lines: List<List<Coordinate>>,
    val slopeAngles: List<Pair<Coordinate, Float>>
)
