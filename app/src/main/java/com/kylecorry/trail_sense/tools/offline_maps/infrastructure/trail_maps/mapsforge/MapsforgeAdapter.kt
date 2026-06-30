package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.trail_maps.mapsforge

import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import org.mapsforge.map.reader.MapFile

object MapsforgeAdapter {

    /**
     * Opens a mapsforge map file from either a local path or an external content URI. The caller
     * is responsible for closing the returned map file.
     */
    suspend fun open(path: String): MapFile? = onIO {
        val files = getAppService<FileSubsystem>()
        tryOrDefault(null) {
            if (files.isExternal(path)) {
                files.fileInputStream(path)?.let { MapFile(it) }
            } else {
                val file = files.get(path)
                if (file.isFile && file.length() > 0) {
                    MapFile(file)
                } else {
                    null
                }
            }
        }
    }

    /**
     * Gets information about the mapsforge map file if it is valid
     * @param path A local path or external content URI to a mapsforge map file
     * @return The map info, or null if the file is not a valid mapsforge map file
     */
    suspend fun getMapInfo(path: String): MapsforgeMapInfo? {
        val mapFile = open(path) ?: return null
        return tryOrDefault(null) {
            try {
                MapsforgeMapInfo(
                    getBounds(mapFile),
                    getAttribution(mapFile)
                )
            } finally {
                mapFile.close()
            }
        }
    }

    private fun getBounds(mapFile: MapFile): CoordinateBounds {
        val bounds = mapFile.mapFileInfo.boundingBox
        return CoordinateBounds(
            bounds.maxLatitude,
            bounds.maxLongitude,
            bounds.minLatitude,
            bounds.minLongitude
        )
    }

    private fun getAttribution(mapFile: MapFile): String? {
        return mapFile.mapFileInfo.comment
            ?.trim()
            ?.removePrefix("Map data (c)")
            ?.removePrefix("Map data ©")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    data class MapsforgeMapInfo(
        val bounds: CoordinateBounds,
        val attribution: String?
    )
}
