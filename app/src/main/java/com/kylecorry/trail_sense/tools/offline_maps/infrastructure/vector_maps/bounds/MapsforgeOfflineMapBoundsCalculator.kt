package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.bounds

import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.VectorMapFiles

class MapsforgeOfflineMapBoundsCalculator : OfflineMapBoundsCalculator {
    override suspend fun getBounds(path: String): CoordinateBounds? {
        val mapFile = VectorMapFiles.openMapsforge(path) ?: return null
        return tryOrDefault(null) {
            try {
                val bounds = mapFile.mapFileInfo.boundingBox
                CoordinateBounds(
                    bounds.maxLatitude,
                    bounds.maxLongitude,
                    bounds.minLatitude,
                    bounds.minLongitude
                )
            } finally {
                mapFile.close()
            }
        }
    }
}
