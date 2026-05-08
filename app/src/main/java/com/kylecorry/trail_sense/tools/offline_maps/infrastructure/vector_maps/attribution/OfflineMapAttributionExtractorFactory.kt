package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.attribution

import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.OfflineMapFileType

class OfflineMapAttributionExtractorFactory {
    fun getAttributionExtractor(type: OfflineMapFileType): OfflineMapAttributionExtractor {
        return when (type) {
            OfflineMapFileType.Mapsforge -> MapsforgeOfflineMapAttributionExtractor()
        }
    }
}
