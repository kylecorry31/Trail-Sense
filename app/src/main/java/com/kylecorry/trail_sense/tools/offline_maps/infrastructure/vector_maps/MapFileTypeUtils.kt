package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps

import android.net.Uri
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.OfflineMapFileType

object MapFileTypeUtils {
    suspend fun getType(uri: Uri): OfflineMapFileType? = onIO {
        val name = getAppService<FileSubsystem>().getFileName(uri, fallbackToPathName = true)
            ?: uri.lastPathSegment
            ?: return@onIO null

        when {
            name.endsWith(".map", ignoreCase = true) -> OfflineMapFileType.Mapsforge
            else -> null
        }
    }

    suspend fun getExtension(type: OfflineMapFileType): String {
        return when (type) {
            OfflineMapFileType.Mapsforge -> "map"
        }
    }
}
