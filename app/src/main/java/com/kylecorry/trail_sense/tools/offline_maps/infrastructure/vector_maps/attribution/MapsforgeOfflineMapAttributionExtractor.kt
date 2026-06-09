package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.attribution

import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.VectorMapFiles

class MapsforgeOfflineMapAttributionExtractor : OfflineMapAttributionExtractor {
    override suspend fun getAttribution(path: String): String? {
        val mapFile = VectorMapFiles.openMapsforge(path) ?: return null
        return tryOrDefault(null) {
            try {
                mapFile.mapFileInfo.comment
                    ?.trim()
                    ?.removePrefix("Map data (c)")
                    ?.removePrefix("Map data ©")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
            } finally {
                mapFile.close()
            }
        }
    }
}
