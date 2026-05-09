package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.bounds

import com.kylecorry.sol.science.geology.CoordinateBounds
import java.io.File

interface OfflineMapBoundsCalculator {
    suspend fun getBounds(file: File): CoordinateBounds?
}
