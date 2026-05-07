package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.bounds

import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.sol.science.geology.CoordinateBounds
import org.mapsforge.map.reader.MapFile
import java.io.File

class MapsforgeOfflineMapBoundsCalculator : OfflineMapBoundsCalculator {
    override suspend fun getBounds(file: File): CoordinateBounds? {
        return tryOrDefault(null) {
            val mapFile = MapFile(file)
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
