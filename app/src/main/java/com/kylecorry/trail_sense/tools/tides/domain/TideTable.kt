package com.kylecorry.trail_sense.tools.tides.domain

import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.database.Identifiable

data class TideTable(
    override val id: Long,
    val tides: List<Tide>,
    val name: String? = null,
    val location: Coordinate? = null
) : Identifiable {

    val isSemidiurnal
        get() = true

}