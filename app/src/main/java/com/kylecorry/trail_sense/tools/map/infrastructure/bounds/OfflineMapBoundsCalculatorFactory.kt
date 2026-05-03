package com.kylecorry.trail_sense.tools.map.infrastructure.bounds

import com.kylecorry.trail_sense.tools.map.domain.OfflineMapFileType

class OfflineMapBoundsCalculatorFactory {
    fun getBoundsCalculator(type: OfflineMapFileType): OfflineMapBoundsCalculator {
        return when (type) {
            OfflineMapFileType.Mapsforge -> MapsforgeOfflineMapBoundsCalculator()
        }
    }
}
