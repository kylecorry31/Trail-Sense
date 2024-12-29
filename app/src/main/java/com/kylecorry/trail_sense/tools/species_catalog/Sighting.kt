package com.kylecorry.trail_sense.tools.species_catalog

import com.kylecorry.sol.units.Coordinate
import java.time.ZonedDateTime

data class Sighting(
    val time: ZonedDateTime,
    val location: Coordinate? = null,
    val harvested: Boolean = false,
    val notes: String? = null
)