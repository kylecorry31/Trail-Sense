package com.kylecorry.trail_sense.shared.andromeda_temp.geopoint

import com.kylecorry.trail_sense.shared.andromeda_temp.BinarySearch

/**
 * The points of a cell of a [GeoPointFile], sorted by latitude (ascending).
 */
class GeoPointCell(
    val latitudes: DoubleArray,
    val longitudes: DoubleArray,
    private val values: Map<String, FloatArray>
) {
    val size get() = latitudes.size

    /**
     * The values of a field for every point, or null if the file has no such field.
     */
    fun getValues(field: String): FloatArray? {
        return values[field]
    }

    /**
     * The index of the first point at or north of a latitude (the size if there is none).
     */
    fun firstIndexAtOrAbove(latitude: Double): Int {
        return BinarySearch.firstIndexAtOrAbove(latitudes, latitude)
    }
}
