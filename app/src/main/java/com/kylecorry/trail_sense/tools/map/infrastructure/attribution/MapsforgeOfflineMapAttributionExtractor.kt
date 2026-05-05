package com.kylecorry.trail_sense.tools.map.infrastructure.attribution

import com.kylecorry.andromeda.core.tryOrDefault
import org.mapsforge.map.reader.MapFile
import java.io.File

class MapsforgeOfflineMapAttributionExtractor : OfflineMapAttributionExtractor {
    override suspend fun getAttribution(file: File): String? {
        return tryOrDefault(null) {
            val mapFile = MapFile(file)
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
