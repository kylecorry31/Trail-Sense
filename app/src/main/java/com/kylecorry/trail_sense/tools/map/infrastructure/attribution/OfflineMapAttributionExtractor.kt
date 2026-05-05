package com.kylecorry.trail_sense.tools.map.infrastructure.attribution

import java.io.File

interface OfflineMapAttributionExtractor {
    suspend fun getAttribution(file: File): String?
}
