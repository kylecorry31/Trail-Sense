package com.kylecorry.trail_sense.shared.dem

import com.kylecorry.sol.math.trigonometry.Trigonometry
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.andromeda_temp.BinarySearch
import com.kylecorry.trail_sense.shared.andromeda_temp.ByteRangeReader
import com.kylecorry.trail_sense.shared.andromeda_temp.geopoint.GeoPointCell
import com.kylecorry.trail_sense.shared.andromeda_temp.geopoint.GeoPointFile
import java.io.Closeable
import java.io.IOException
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

class PeakOverlay private constructor(private val summits: GeoPointFile, private val radiusKm: Double) : Closeable {

    private val radiusDegrees = radiusKm / KM_PER_DEGREE

    /**
     * The elevation to add at a location, where the model is at [elevation].
     */
    suspend fun getOffset(latitude: Double, longitude: Double, elevation: Float): Float {
        return getOffsets(doubleArrayOf(latitude), doubleArrayOf(longitude), floatArrayOf(elevation), 1)[0]
    }

    /**
     * Sharpens a grid of elevations around the summits (the first channel, where it is above zero because zero is no data).
     * A point is raised by at least as much as the highest summit within the area it covers, so a summit keeps its
     * height on a grid coarser than its radius.
     *
     * @param gridLatitudes the latitude of every row, ascending and evenly spaced
     * @param gridLongitudes the longitude of every column, evenly spaced
     * @param data the grid, row by row, with [channels] values per cell
     */
    suspend fun addTo(
        gridLatitudes: DoubleArray,
        gridLongitudes: DoubleArray,
        data: FloatArray,
        channels: Int
    ) {
        val offsets = getOffsets(gridLatitudes, gridLongitudes, data, channels)
        for (i in offsets.indices) {
            data[i * channels] += offsets[i]
        }
    }

    // Every summit works from the elevation of the model, not from what other summits have added
    private suspend fun getOffsets(
        gridLatitudes: DoubleArray,
        gridLongitudes: DoubleArray,
        data: FloatArray,
        channels: Int
    ): FloatArray {
        if (gridLatitudes.isEmpty() || gridLongitudes.isEmpty()) {
            return FloatArray(0)
        }
        val grid = Grid(gridLatitudes, gridLongitudes, data, channels)
        val offsets = Offsets(gridLatitudes.size * gridLongitudes.size)
        val reach = radiusDegrees + grid.footprintDegrees
        // The widest the reach is in degrees of longitude is where the grid is closest to the equator
        val longitudeReach = getLongitudeReach(reach, Trigonometry.cosDegrees(gridLatitudes.minOf { abs(it) }))
        val (west, east) = getLongitudeSpan(gridLongitudes)
        val south = gridLatitudes.first() - reach
        val north = gridLatitudes.last() + reach
        for (cell in summits.getCells(south, north, west - longitudeReach, east + longitudeReach)) {
            val cellSummits = Summits(cell)
            var i = cell.firstIndexAtOrAbove(south)
            while (i < cell.size && cell.latitudes[i] <= north) {
                if (isWithinSpan(cell.longitudes[i], west, east, longitudeReach)) {
                    addSummitOffsets(cellSummits, i, grid, reach, offsets)
                }
                i++
            }
        }
        return offsets.combine()
    }

    private fun addSummitOffsets(cell: Summits, summit: Int, grid: Grid, reach: Double, offsets: Offsets) {
        val latitude = cell.latitudes[summit]
        var row = BinarySearch.firstIndexAtOrAbove(grid.latitudes, latitude - reach)
        while (row < grid.latitudes.size && grid.latitudes[row] <= latitude + reach) {
            val cosLatitude = grid.cosLatitudes[row]
            grid.forEachColumnNear(cell.longitudes[summit], getLongitudeReach(reach, cosLatitude)) { column ->
                val index = row * grid.longitudes.size + column
                val elevation = grid.data[index * grid.channels]
                if (elevation > 0f) {
                    val dy = (grid.latitudes[row] - latitude) * KM_PER_DEGREE
                    val dx = Coordinate.toLongitude(grid.longitudes[column] - cell.longitudes[summit]) *
                            KM_PER_DEGREE * cosLatitude
                    val distance = hypot(dx, dy)
                    offsets.add(index, getBump(cell, summit, distance, elevation))
                    offsets.keepHighest(index, getBump(cell, summit, distance - grid.footprintsKm[row], elevation))
                }
            }
            row++
        }
    }

    private fun getBump(cell: Summits, i: Int, distanceKm: Double, elevation: Float): Float {
        if (distanceKm >= radiusKm) {
            return 0f
        }
        val t = 1 - (distanceKm.coerceAtLeast(0.0) / radiusKm).let { it * it }
        return cell.scales[i] * (elevation - cell.bases[i]).coerceAtLeast(0f) * (t * t).toFloat()
    }

    private class Grid(val latitudes: DoubleArray, val longitudes: DoubleArray, val data: FloatArray, val channels: Int) {
        val cosLatitudes = DoubleArray(latitudes.size) { Trigonometry.cosDegrees(latitudes[it]) }

        private val latitudeSpacing = if (latitudes.size > 1) latitudes[1] - latitudes[0] else 0.0
        private val longitudeSpacing =
            if (longitudes.size > 1) abs(Coordinate.toLongitude(longitudes[1] - longitudes[0])) else 0.0

        // Half the diagonal of the area each point covers
        val footprintDegrees = hypot(latitudeSpacing, longitudeSpacing) / 2
        val footprintsKm = DoubleArray(latitudes.size) {
            hypot(latitudeSpacing, longitudeSpacing * cosLatitudes[it]) / 2 * KM_PER_DEGREE
        }

        private val wrappedLongitudes = DoubleArray(longitudes.size) { Coordinate.toLongitude(longitudes[it]) }
        private val sortedColumns = longitudes.indices.sortedBy { wrappedLongitudes[it] }.toIntArray()
        private val sortedLongitudes = DoubleArray(longitudes.size) { wrappedLongitudes[sortedColumns[it]] }

        /**
         * Calls [action] with every column within [reach] degrees of a longitude.
         */
        inline fun forEachColumnNear(longitude: Double, reach: Double, action: (Int) -> Unit) {
            if (reach >= 180.0) {
                longitudes.indices.forEach(action)
                return
            }
            val start = Coordinate.toLongitude(longitude - reach)
            val end = start + 2 * reach
            forEachColumnBetween(start, minOf(end, 180.0), action)
            if (end > 180.0) {
                forEachColumnBetween(-180.0, end - 360.0, action)
            }
        }

        private inline fun forEachColumnBetween(start: Double, end: Double, action: (Int) -> Unit) {
            var i = BinarySearch.firstIndexAtOrAbove(sortedLongitudes, start)
            while (i < sortedLongitudes.size && sortedLongitudes[i] <= end) {
                action(sortedColumns[i])
                i++
            }
        }
    }

    // The summed bumps at each point, and the highest bump within the area each point covers
    private class Offsets(size: Int) {
        private val sums = FloatArray(size)
        private val highest = FloatArray(size)

        fun add(index: Int, bump: Float) {
            sums[index] += bump
        }

        fun keepHighest(index: Int, bump: Float) {
            highest[index] = max(highest[index], bump)
        }

        fun combine(): FloatArray {
            return FloatArray(sums.size) { max(sums[it], highest[it]) }
        }
    }

    // A file without these fields is not opened
    private class Summits(cell: GeoPointCell) {
        val latitudes = cell.latitudes
        val longitudes = cell.longitudes
        val scales = checkNotNull(cell.getValues(SCALE_FIELD))
        val bases = checkNotNull(cell.getValues(BASE_FIELD))
    }

    private fun getLongitudeReach(reach: Double, cosLatitude: Double): Double {
        return (reach / cosLatitude.coerceAtLeast(MIN_COS_LATITUDE)).coerceAtMost(180.0)
    }

    private fun getLongitudeSpan(longitudes: DoubleArray): Pair<Double, Double> {
        val sorted = DoubleArray(longitudes.size) { Coordinate.toLongitude(longitudes[it]) }.also { it.sort() }
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

    private fun isWithinSpan(longitude: Double, west: Double, east: Double, margin: Double): Boolean {
        val eastOfWest = (longitude - west).mod(360.0)
        return eastOfWest <= east - west + margin || eastOfWest >= 360.0 - margin
    }

    override fun close() {
        summits.close()
    }

    companion object {
        private const val KM_PER_DEGREE = 111.0
        private const val MIN_COS_LATITUDE = 0.01
        private const val TYPE = "peaks"
        private const val SCALE_FIELD = "scale"
        private const val BASE_FIELD = "base"

        /**
         * Opens a peaks file, or returns null if it is not one this version understands (the reader is then closed).
         * Throws an [IOException] if it could not be read.
         */
        suspend fun open(reader: ByteRangeReader): PeakOverlay? {
            val overlay = try {
                GeoPointFile.open(reader)?.let { create(it) }
            } catch (e: IOException) {
                reader.close()
                throw e
            }
            if (overlay == null) {
                reader.close()
            }
            return overlay
        }

        private fun create(file: GeoPointFile): PeakOverlay? {
            val radiusMeters = file.getNumberProperty("radius_meters") ?: 0.0
            val hasFields = file.hasField(SCALE_FIELD) && file.hasField(BASE_FIELD)
            if (file.getStringProperty("type") != TYPE || radiusMeters <= 0 || !hasFields) {
                return null
            }
            return PeakOverlay(file, radiusMeters / 1000.0)
        }
    }
}
