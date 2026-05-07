package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.bounds

import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapFileType

class OfflineMapBoundsCalculatorFactory {
    fun getBoundsCalculator(type: OfflineMapFileType): OfflineMapBoundsCalculator {
        return when (type) {
            OfflineMapFileType.Mapsforge -> MapsforgeOfflineMapBoundsCalculator()
        }
    }
}
