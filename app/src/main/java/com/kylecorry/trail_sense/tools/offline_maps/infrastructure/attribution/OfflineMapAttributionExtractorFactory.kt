package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.attribution

import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapFileType

class OfflineMapAttributionExtractorFactory {
    fun getAttributionExtractor(type: OfflineMapFileType): OfflineMapAttributionExtractor {
        return when (type) {
            OfflineMapFileType.Mapsforge -> MapsforgeOfflineMapAttributionExtractor()
        }
    }
}
