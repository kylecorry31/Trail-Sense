package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.trail_maps

import android.net.Uri
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.io.FileSubsystem

object MapFileTypeUtils {

    fun isMapsforgeMap(uri: Uri): Boolean {
        val name = getAppService<FileSubsystem>().getFileName(uri, fallbackToPathName = true)
            ?: uri.lastPathSegment

        return name.orEmpty().endsWith(".${MAPSFORGE_MAP_EXTENSION}", ignoreCase = true)
    }

    const val MAPSFORGE_MAP_EXTENSION = "map"
}
