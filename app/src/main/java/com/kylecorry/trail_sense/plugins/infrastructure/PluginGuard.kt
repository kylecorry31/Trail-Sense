package com.kylecorry.trail_sense.plugins.infrastructure

import com.kylecorry.andromeda.geojson.GeoJsonObject
import java.time.Duration

object PluginGuard {
    const val MAX_REGISTRATION_BYTES = 64 * 1024
    const val MAX_MAP_LAYERS = 25
    const val MAX_ENDPOINT_LENGTH = 100
    const val MAX_LAYER_NAME_LENGTH = 100
    const val MAX_LAYER_DESCRIPTION_LENGTH = 1000
    const val MAX_ATTRIBUTION_LENGTH = 500
    const val MAX_LONG_ATTRIBUTION_LENGTH = 2000
    const val MIN_ZOOM_LEVEL = 0
    const val MAX_ZOOM_LEVEL = 20
    const val MAX_GEOJSON_BYTES = 1024 * 1024
    const val MAX_GEOJSON_FEATURES = 1000
    const val MAX_GEOJSON_COORDINATES = 10000
    const val MAX_GEOJSON_GEOMETRY_DEPTH = 8
    const val MAX_TILE_BYTES = 1024 * 1024
    const val MAX_TILE_SIZE = 256

    val MIN_REFRESH_INTERVAL: Duration = Duration.ofSeconds(30)
    val MAX_REFRESH_INTERVAL: Duration = Duration.ofHours(1)
    val MAX_GEOJSON_PARSE_TIME: Duration = Duration.ofSeconds(2)

    private val endpointRegex = Regex("^/[A-Za-z0-9/_-]{1,${MAX_ENDPOINT_LENGTH - 1}}$")

    fun isValidEndpoint(endpoint: String): Boolean {
        return endpointRegex.matches(endpoint)
    }

    fun isValidRegistrationPayload(payload: ByteArray?): Boolean {
        return payload != null && payload.size <= MAX_REGISTRATION_BYTES
    }

    fun isValidGeoJsonPayload(payload: ByteArray): Boolean {
        return payload.size <= MAX_GEOJSON_BYTES
    }

    fun isValidGeoJson(geoJson: GeoJsonObject): Boolean {
        return PluginGeoJsonValidator.isValid(geoJson)
    }

    fun isValidTilePayload(payload: ByteArray): Boolean {
        return payload.size <= MAX_TILE_BYTES
    }

    fun isValidTileSize(width: Int, height: Int): Boolean {
        return width in 1..MAX_TILE_SIZE && height in 1..MAX_TILE_SIZE
    }
}
