package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.attribution

import java.io.File

interface OfflineMapAttributionExtractor {
    suspend fun getAttribution(file: File): String?
}
