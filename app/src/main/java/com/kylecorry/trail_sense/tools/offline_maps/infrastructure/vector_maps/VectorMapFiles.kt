package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps

import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import org.mapsforge.map.reader.MapFile

object VectorMapFiles {

    /**
     * Opens a mapsforge map file from either a local path or an external content URI. The caller
     * is responsible for closing the returned map file.
     */
    suspend fun openMapsforge(path: String): MapFile? = onIO {
        val files = getAppService<FileSubsystem>()
        try {
            if (files.isExternal(path)) {
                val stream = files.fileInputStream(path) ?: return@onIO null
                MapFile(stream)
            } else {
                val file = files.get(path)
                if (!file.isFile || file.length() == 0L) {
                    return@onIO null
                }
                MapFile(file)
            }
        } catch (e: Exception) {
            null
        }
    }
}
