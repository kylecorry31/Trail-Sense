package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.trail_maps

import android.net.Uri
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.offline_maps.domain.trail_maps.TrailMapFileType

object MapFileTypeUtils {
    suspend fun getType(uri: Uri): TrailMapFileType? = onIO {
        val name = getAppService<FileSubsystem>().getFileName(uri, fallbackToPathName = true)
            ?: uri.lastPathSegment
            ?: return@onIO null

        when {
            name.endsWith(".map", ignoreCase = true) -> TrailMapFileType.Mapsforge
            else -> null
        }
    }

    fun getExtension(type: TrailMapFileType): String {
        return when (type) {
            TrailMapFileType.Mapsforge -> "map"
        }
    }
}
