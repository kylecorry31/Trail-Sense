package com.kylecorry.trail_sense.shared.andromeda_temp.geopoint

import com.kylecorry.trail_sense.shared.andromeda_temp.BinarySearch
import com.kylecorry.trail_sense.shared.andromeda_temp.ByteRangeReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Where the blocks of a [GeoPointFile] are, and which cells each one holds.
 */
class GeoPointDirectory private constructor(
    // The first and last cell with points of each block
    private val firstCells: IntArray,
    private val lastCells: IntArray,
    // The offset of each block, plus the end of the last one
    private val offsets: LongArray
) {

    val size = firstCells.size

    fun getFirstCell(block: Int): Int = firstCells[block]

    fun getLastCell(block: Int): Int = lastCells[block]

    fun getOffset(block: Int): Long = offsets[block]

    fun getLength(block: Int): Long = offsets[block + 1] - offsets[block]

    /**
     * The block that holds a cell, or -1 if no block can (the cell has no points).
     */
    fun findBlock(cell: Int): Int {
        // The last block that starts at or before the cell
        val block = BinarySearch.firstIndex(size) { firstCells[it] > cell } - 1
        return if (block >= 0 && cell <= lastCells[block]) block else -1
    }

    companion object {
        private const val ENTRY_BYTES = 12

        /**
         * Reads the directory at [start], throwing an [IllegalArgumentException] if it is not valid.
         */
        suspend fun read(reader: ByteRangeReader, start: Long, blockCount: Int, cellCount: Int): GeoPointDirectory {
            require(blockCount in 0..cellCount) { "Too many blocks" }
            val buffer = ByteBuffer.wrap(reader.read(start, (blockCount + 1) * ENTRY_BYTES))
                .order(ByteOrder.LITTLE_ENDIAN)
            val firstCells = IntArray(blockCount + 1)
            val lastCells = IntArray(blockCount + 1)
            val offsets = LongArray(blockCount + 1)
            for (i in 0..blockCount) {
                firstCells[i] = buffer.getInt()
                lastCells[i] = buffer.getInt()
                offsets[i] = buffer.getInt().toLong() and 0xFFFFFFFFL
            }

            // The blocks are in order and do not overlap, and the last entry only marks the end
            require(firstCells[blockCount] == cellCount) { "The directory does not end with the cell count" }
            require(offsets[0] == start + (blockCount + 1) * ENTRY_BYTES) { "The blocks do not follow the directory" }
            for (i in 0 until blockCount) {
                require(firstCells[i] in 0..lastCells[i] && lastCells[i] < firstCells[i + 1]) { "Blocks overlap" }
                require(offsets[i] <= offsets[i + 1]) { "Blocks are out of order" }
            }
            return GeoPointDirectory(firstCells.copyOf(blockCount), lastCells.copyOf(blockCount), offsets)
        }
    }
}
