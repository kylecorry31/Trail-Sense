package com.kylecorry.trail_sense.tools.tides.domain

import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.science.oceanography.TideConstituent
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.database.Identifiable

data class TideTable(
    override val id: Long,
    val tides: List<Tide>,
    val name: String? = null,
    val location: Coordinate? = null,
    val isSemidiurnal: Boolean = true,
    val isVisible: Boolean = true
) : Identifiable {

    val principalFrequency: Float
        get() {
            return if (isSemidiurnal){
                TideConstituent.M2.speed
            } else {
                TideConstituent.M2.speed / 2
            }
        }

}