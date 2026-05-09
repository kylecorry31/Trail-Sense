package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.bounds

import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.VectorMapFileType

class OfflineMapBoundsCalculatorFactory {
    fun getBoundsCalculator(type: VectorMapFileType): OfflineMapBoundsCalculator {
        return when (type) {
            VectorMapFileType.Mapsforge -> MapsforgeOfflineMapBoundsCalculator()
        }
    }
}
