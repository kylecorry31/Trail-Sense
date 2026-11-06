package com.kylecorry.trail_sense.shared.data

import kotlinx.coroutines.CancellationException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.floor

/**
 * The points of a cell of a [GeoPointFile], sorted by latitude (ascending).
 */
class GeoPointCell(
    val latitudes: DoubleArray,
    val longitudes: DoubleArray,
    val values: FloatArray
) {
    val size get() = latitudes.size

    fun firstIndexAtOrAbove(latitude: Double): Int {
        return latitudes.firstIndexAtOrAbove(latitude)
    }
}

/**
 * The index of the first value at or above the target in an ascending array (the size if there is none).
 */
fun DoubleArray.firstIndexAtOrAbove(target: Double): Int {
    var low = 0
    var high = size
    while (low < high) {
        val middle = (low + high) ushr 1
        if (this[middle] < target) {
            low = middle + 1
        } else {
            high = middle
        }
    }
    return low
}

/**
 * A file of points on the globe, each with a value, grouped into square cells (see scripts/geo_points.py in the earth
 * model generator for the format). What the values mean is up to the caller.
 *
 * Only the cells around a location are read (and kept for next time), so a large file can be used with little memory.
 */
class GeoPointFile private constructor(
    private val reader: ByteRangeReader,
    private val cellDegrees: Int,
    private val bitsPerAxis: Int,
    private val valueBytes: Int,
    private val valueStep: Float,
    // The number of points before each cell, plus the total
    private val cellStarts: IntArray
) {

    private val rows = 180 / cellDegrees
    private val columns = 360 / cellDegrees
    private val positionBytes = (2 * bitsPerAxis + 7) / 8
    private val recordBytes = positionBytes + valueBytes
    private val recordsOffset = HEADER_BYTES + cellStarts.size * 4L
    private val totalCount = cellStarts.last()
    private val cache = object : LinkedHashMap<Int, GeoPointCell>(CACHED_CELLS, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, GeoPointCell>?): Boolean {
            return size > CACHED_CELLS
        }
    }

    /**
     * The cells with points that cover an area. The area can extend past the antimeridian (a west of -190 is 170).
     */
    suspend fun getCells(
        south: Double,
        north: Double,
        west: Double,
        east: Double
    ): List<GeoPointCell> {
        val firstColumn = floor((west + 180) / cellDegrees).toInt()
        val lastColumn = floor((east + 180) / cellDegrees).toInt()
        val columnCount = (lastColumn - firstColumn + 1).coerceAtMost(columns)

        val cells = ArrayList<GeoPointCell>()
        for (row in getRow(south)..getRow(north)) {
            for (i in 0 until columnCount) {
                val index = row * columns + Math.floorMod(firstColumn + i, columns)
                if (cellStarts[index + 1] > cellStarts[index]) {
                    getCell(index)?.let { cells.add(it) }
                }
            }
        }
        return cells
    }

    private fun getRow(latitude: Double): Int {
        return floor((latitude + 90) / cellDegrees).toInt().coerceIn(0, rows - 1)
    }

    private suspend fun getCell(index: Int): GeoPointCell? {
        synchronized(cache) {
            cache[index]?.let { return it }
        }
        // A cell that could not be read is not kept, so it is tried again next time
        val cell = readCell(index) ?: return null
        synchronized(cache) {
            cache[index] = cell
        }
        return cell
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun readCell(index: Int): GeoPointCell? {
        val count = cellStarts[index + 1] - cellStarts[index]
        return try {
            // The records are stored by byte, so a cell is a piece of each of the record_bytes runs
            val bytes = Array(recordBytes) { byteIndex ->
                reader.read(recordsOffset + byteIndex.toLong() * totalCount + cellStarts[index], count)
            }
            val south = (index / columns) * cellDegrees - 90.0
            val west = (index % columns) * cellDegrees - 180.0
            val steps = (1L shl bitsPerAxis).toDouble()
            val mask = (1L shl bitsPerAxis) - 1
            var y = 0L

            val latitudes = DoubleArray(count)
            val longitudes = DoubleArray(count)
            val values = FloatArray(count)
            for (i in 0 until count) {
                val position = readUnsigned(bytes, i, 0, positionBytes)
                // Each latitude is stored as the step from the one before it
                y += (position shr bitsPerAxis) and mask
                latitudes[i] = south + (y + 0.5) / steps * cellDegrees
                longitudes[i] = west + ((position and mask) + 0.5) / steps * cellDegrees
                values[i] = readUnsigned(bytes, i, positionBytes, valueBytes) * valueStep
            }
            GeoPointCell(latitudes, longitudes, values)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // A file that cannot be read just has no points in that cell
            null
        }
    }

    private fun readUnsigned(bytes: Array<ByteArray>, record: Int, firstByte: Int, byteCount: Int): Long {
        var value = 0L
        for (i in 0 until byteCount) {
            value = value or ((bytes[firstByte + i][record].toLong() and 0xFF) shl (8 * i))
        }
        return value
    }

    companion object {
        // Files of a newer version may be laid out differently, so they are not used
        private const val FORMAT_VERSION = 1
        private const val HEADER_BYTES = 12
        private const val CACHED_CELLS = 32

        /**
         * Opens a file by reading its header and cell table, or returns null if it is not a file this version
         * understands.
         */
        @Suppress("TooGenericExceptionCaught", "SwallowedException")
        suspend fun open(reader: ByteRangeReader): GeoPointFile? {
            return try {
                val header = ByteBuffer.wrap(reader.read(0, HEADER_BYTES)).order(ByteOrder.LITTLE_ENDIAN)
                val version = header.get().toInt() and 0xFF
                val cellDegrees = header.get().toInt() and 0xFF
                val valueBytes = header.get().toInt() and 0xFF
                val bitsPerAxis = header.get().toInt() and 0xFF
                val count = header.getInt()
                val valueStep = header.getFloat()

                val validShape = cellDegrees > 0 && 180 % cellDegrees == 0 && 360 % cellDegrees == 0 &&
                        valueBytes in 1..4 && bitsPerAxis in 1..24
                if (version != FORMAT_VERSION || !validShape || count < 0) {
                    return null
                }

                val cellCount = (180 / cellDegrees) * (360 / cellDegrees)
                val table = ByteBuffer.wrap(reader.read(HEADER_BYTES.toLong(), (cellCount + 1) * 4))
                    .order(ByteOrder.LITTLE_ENDIAN)
                val starts = IntArray(cellCount + 1) { table.getInt() }
                val validTable = starts[0] == 0 && starts[cellCount] == count &&
                        (1..cellCount).all { starts[it] >= starts[it - 1] }
                if (!validTable) {
                    return null
                }
                GeoPointFile(reader, cellDegrees, bitsPerAxis, valueBytes, valueStep, starts)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }
    }
}
