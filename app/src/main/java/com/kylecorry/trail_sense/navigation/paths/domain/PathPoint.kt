package com.kylecorry.trail_sense.navigation.paths.domain

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.andromeda.signal.CellNetworkQuality
import java.time.Instant

data class PathPoint(
    val id: Long,
    val pathId: Long,
    val coordinate: Coordinate,
    val elevation: Float? = null,
    val time: Instant? = null,
    val cellSignal: CellNetworkQuality? = null
)