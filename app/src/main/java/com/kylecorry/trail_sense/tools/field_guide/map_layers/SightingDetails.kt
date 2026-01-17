package com.kylecorry.trail_sense.tools.field_guide.map_layers

import com.kylecorry.sol.units.Coordinate

data class SightingDetails(
    val pageId: Long,
    val sightingId: Long,
    val location: Coordinate,
    val name: String
)