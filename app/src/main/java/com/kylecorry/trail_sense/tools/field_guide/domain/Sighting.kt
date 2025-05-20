package com.kylecorry.trail_sense.tools.field_guide.domain

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.data.Identifiable
import java.time.Instant
import java.time.ZonedDateTime

data class Sighting(
    override val id: Long,
    val fieldGuidePageId: Long,
    val time: Instant? = null,
    val location: Coordinate? = null,
    val altitude: Float? = null,
    val harvested: Boolean? = null,
    val notes: String? = null
) : Identifiable