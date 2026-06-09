package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.attribution

interface OfflineMapAttributionExtractor {
    suspend fun getAttribution(path: String): String?
}
