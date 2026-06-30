package com.kylecorry.trail_sense.tools.offline_maps.domain

import com.kylecorry.sol.science.geology.CoordinateBounds
import java.time.Instant

interface OfflineMap : IMap {
    val files: List<OfflineMapFile>
    val visible: Boolean
    val state: OfflineMapState

    /**
     * The bounds that the map covers. May be null when the state is Draft.
     */
    val bounds: CoordinateBounds?
    val createdOn: Instant?
}
