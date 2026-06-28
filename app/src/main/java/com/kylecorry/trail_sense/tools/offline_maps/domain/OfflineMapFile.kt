package com.kylecorry.trail_sense.tools.offline_maps.domain

import com.kylecorry.trail_sense.shared.io.FileSubsystem

data class OfflineMapFile(
    val path: String,
    val sizeBytes: Long,
    val role: String
) {
    val isExternal = path.startsWith(FileSubsystem.SCHEME_CONTENT)
    val isAsset = path.startsWith(FileSubsystem.SCHEME_ASSETS)
}
