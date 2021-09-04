package com.kylecorry.trail_sense.shared.paths

import com.kylecorry.andromeda.core.units.Coordinate
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