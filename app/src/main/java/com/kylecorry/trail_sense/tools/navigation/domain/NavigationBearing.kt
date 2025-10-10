package com.kylecorry.trail_sense.tools.navigation.domain

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.data.Identifiable
import java.time.Instant

data class NavigationBearing(
    override val id: Long,
    val bearing: Float,
    val startLocation: Coordinate?,
    val startTime: Instant?,
    val isActive: Boolean
) : Identifiable