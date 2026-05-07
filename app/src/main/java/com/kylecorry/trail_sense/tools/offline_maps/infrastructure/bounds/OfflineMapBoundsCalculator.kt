package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.bounds

import com.kylecorry.sol.science.geology.CoordinateBounds
import java.io.File

interface OfflineMapBoundsCalculator {
    suspend fun getBounds(file: File): CoordinateBounds?
}
