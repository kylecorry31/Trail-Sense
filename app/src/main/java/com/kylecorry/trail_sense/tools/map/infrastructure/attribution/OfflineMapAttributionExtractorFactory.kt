package com.kylecorry.trail_sense.tools.map.infrastructure.attribution

import com.kylecorry.trail_sense.tools.map.domain.OfflineMapFileType

class OfflineMapAttributionExtractorFactory {
    fun getAttributionExtractor(type: OfflineMapFileType): OfflineMapAttributionExtractor {
        return when (type) {
            OfflineMapFileType.Mapsforge -> MapsforgeOfflineMapAttributionExtractor()
        }
    }
}
