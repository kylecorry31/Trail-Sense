package com.kylecorry.trail_sense.shared.andromeda_temp.geopoint

import androidx.annotation.Keep
import com.kylecorry.andromeda.json.JsonConvert

/**
 * A value that every point of a [GeoPointFile] has, stored as an unsigned integer of [bytes] bytes.
 */
class GeoPointField(val name: String, val bytes: Int, private val step: Double, private val offset: Double) {
    fun decode(stored: Long): Float {
        return (stored * step + offset).toFloat()
    }
}

/**
 * The metadata of a [GeoPointFile], checked when the file is opened.
 */
class GeoPointMetadata(
    val grid: GeoPointGrid,
    val fields: List<GeoPointField>,
    val properties: Map<String, Any?>
) {
    companion object {
        /**
         * Parses the metadata, throwing an [IllegalArgumentException] if it is not valid.
         */
        fun parse(json: String): GeoPointMetadata {
            val metadata = requireNotNull(JsonConvert.fromJson<GeoPointMetadataJson>(json)) { "Invalid metadata" }
            val fields = metadata.fields.orEmpty().map {
                val bytes = requireNotNull(it.bytes) { "A field has no size" }
                require(bytes in 1..4) { "Fields must be 1 to 4 bytes" }
                GeoPointField(requireNotNull(it.name) { "A field has no name" }, bytes, it.step ?: 1.0, it.offset ?: 0.0)
            }
            val cellDegrees = requireNotNull(metadata.cell_degrees) { "No cell size" }
            val decimals = requireNotNull(metadata.decimals) { "No decimals" }
            return GeoPointMetadata(GeoPointGrid(cellDegrees, decimals), fields, metadata.properties.orEmpty())
        }
    }
}

@Keep
@Suppress("ConstructorParameterNaming")
private class GeoPointMetadataJson(
    val cell_degrees: Int?,
    val decimals: Int?,
    val fields: List<GeoPointFieldJson>?,
    val properties: Map<String, Any?>?
)

@Keep
private class GeoPointFieldJson(
    val name: String?,
    val bytes: Int?,
    val step: Double?,
    val offset: Double?
)
