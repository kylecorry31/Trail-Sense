package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.bounds

import com.kylecorry.sol.science.geology.CoordinateBounds

interface OfflineMapBoundsCalculator {
    suspend fun getBounds(path: String): CoordinateBounds?
}
