package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.bounds

import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.OfflineMapFileType

class OfflineMapBoundsCalculatorFactory {
    fun getBoundsCalculator(type: OfflineMapFileType): OfflineMapBoundsCalculator {
        return when (type) {
            OfflineMapFileType.Mapsforge -> MapsforgeOfflineMapBoundsCalculator()
        }
    }
}
