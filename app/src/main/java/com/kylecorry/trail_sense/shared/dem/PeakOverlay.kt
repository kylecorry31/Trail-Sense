package com.kylecorry.trail_sense.shared.dem

import com.kylecorry.trail_sense.shared.data.ByteRangeReader
import com.kylecorry.trail_sense.shared.data.GeoPointCell
import com.kylecorry.trail_sense.shared.data.GeoPointFile
import com.kylecorry.trail_sense.shared.data.firstIndexAtOrAbove
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor

class PeakOverlay private constructor(private val summits: GeoPointFile) {

    private val latitudeRadius = RADIUS_KM / KM_PER_DEGREE

    /**
     * The elevation to add at a location, where the model is at [elevation].
     */
    suspend fun getOffset(latitude: Double, longitude: Double, elevation: Float): Float {
        val cosLatitude = cos(Math.toRadians(latitude))
        val longitudeRadius = getLongitudeRadius(cosLatitude)
        var offset = 0f
        for (cell in summits.getCells(
            latitude - latitudeRadius,
            latitude + latitudeRadius,
            longitude - longitudeRadius,
            longitude + longitudeRadius
        )) {
            var i = cell.firstIndexAtOrAbove(latitude - latitudeRadius)
            while (i < cell.size && cell.latitudes[i] <= latitude + latitudeRadius) {
                offset += getBump(cell, i, latitude, longitude, cosLatitude, elevation)
                i++
            }
        }
        return offset
    }

    /**
     * Sharpens a grid of elevations around the summits (the first channel, where it is above zero because zero is no data).
     *
     * @param gridLatitudes the latitude of every row, ascending
     * @param gridLongitudes the longitude of every column
     * @param data the grid, row by row, with [channels] values per cell
     */
    suspend fun addTo(
        gridLatitudes: DoubleArray,
        gridLongitudes: DoubleArray,
        data: FloatArray,
        channels: Int
    ) {
        if (gridLatitudes.isEmpty() || gridLongitudes.isEmpty()) {
            return
        }
        // The widest the radius is in degrees of longitude is where the grid is closest to the equator
        val longitudeRadius = getLongitudeRadius(cos(Math.toRadians(gridLatitudes.minOf { abs(it) })))
        val (west, east) = getLongitudeSpan(gridLongitudes)
        val cells = summits.getCells(
            gridLatitudes.first() - latitudeRadius,
            gridLatitudes.last() + latitudeRadius,
            west - longitudeRadius,
            east + longitudeRadius
        )

        val width = gridLongitudes.size
        // Every summit works from the elevation of the model, not from what other summits have added
        val offsets = FloatArray(gridLatitudes.size * width)
        for (cell in cells) {
            for (i in 0 until cell.size) {
                addSummitOffsets(cell, i, gridLatitudes, gridLongitudes, data, channels, offsets)
            }
        }
        for (i in offsets.indices) {
            data[i * channels] += offsets[i]
        }
    }

    private fun addSummitOffsets(
        cell: GeoPointCell,
        summit: Int,
        gridLatitudes: DoubleArray,
        gridLongitudes: DoubleArray,
        data: FloatArray,
        channels: Int,
        offsets: FloatArray
    ) {
        val width = gridLongitudes.size
        val latitude = cell.latitudes[summit]
        var row = gridLatitudes.firstIndexAtOrAbove(latitude - latitudeRadius)
        while (row < gridLatitudes.size && gridLatitudes[row] <= latitude + latitudeRadius) {
            val cosLatitude = cos(Math.toRadians(gridLatitudes[row]))
            for (column in 0 until width) {
                val cellIndex = row * width + column
                val elevation = data[cellIndex * channels]
                if (elevation > 0f) {
                    offsets[cellIndex] += getBump(
                        cell, summit, gridLatitudes[row], gridLongitudes[column], cosLatitude, elevation
                    )
                }
            }
            row++
        }
    }

    private fun getBump(
        cell: GeoPointCell,
        i: Int,
        latitude: Double,
        longitude: Double,
        cosLatitude: Double,
        elevation: Float
    ): Float {
        val dy = (latitude - cell.latitudes[i]) * KM_PER_DEGREE
        val dx = wrapLongitudeDifference(longitude - cell.longitudes[i]) * KM_PER_DEGREE * cosLatitude
        val distanceSquared = dx * dx + dy * dy
        if (distanceSquared >= RADIUS_KM * RADIUS_KM) {
            return 0f
        }
        val t = 1 - distanceSquared / (RADIUS_KM * RADIUS_KM)
        val packed = cell.values[i].toInt()
        val scale = (packed and SCALE_MASK) / SCALE_STEPS_PER_UNIT
        val base = (packed shr SCALE_BITS) * BASE_STEP_METERS
        return scale * (elevation - base).coerceAtLeast(0f) * (t * t).toFloat()
    }

    private fun getLongitudeRadius(cosLatitude: Double): Double {
        return (latitudeRadius / cosLatitude.coerceAtLeast(MIN_COS_LATITUDE)).coerceAtMost(180.0)
    }

    private fun getLongitudeSpan(longitudes: DoubleArray): Pair<Double, Double> {
        val sorted = DoubleArray(longitudes.size) { wrapLongitude(longitudes[it]) }.also { it.sort() }
        // The span is everything except the largest gap between neighboring columns
        var largestGap = sorted.first() + 360.0 - sorted.last()
        var westIndex = 0
        for (i in 1 until sorted.size) {
            val gap = sorted[i] - sorted[i - 1]
            if (gap > largestGap) {
                largestGap = gap
                westIndex = i
            }
        }
        val west = sorted[westIndex]
        val east = if (westIndex == 0) sorted.last() else sorted[westIndex - 1] + 360.0
        return west to east
    }

    private fun wrapLongitude(longitude: Double): Double {
        return longitude - 360.0 * floor((longitude + 180.0) / 360.0)
    }

    private fun wrapLongitudeDifference(difference: Double): Double {
        return difference - 360.0 * floor((difference + 180.0) / 360.0)
    }

    companion object {
        private const val RADIUS_KM = 4.0
        private const val KM_PER_DEGREE = 111.0
        private const val MIN_COS_LATITUDE = 0.01
        private const val SCALE_BITS = 7
        private const val SCALE_MASK = (1 shl SCALE_BITS) - 1
        private const val SCALE_STEPS_PER_UNIT = 63f
        private const val BASE_STEP_METERS = 16f

        suspend fun open(reader: ByteRangeReader): PeakOverlay? {
            return GeoPointFile.open(reader)?.let { PeakOverlay(it) }
        }
    }
}
