package com.kylecorry.trail_sense.tools.signal_finder.infrastructure

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * An index of the cells (a square of 1 / cellsPerDegree degrees) that contain a cell tower, see cell_tower_index.py in the Earth Model
 * project for the format. The cell at (row, column) is centered on latitude row / cellsPerDegree - 90 and longitude
 * column / cellsPerDegree - 180.
 */
class CellTowerIndex private constructor(
    private val source: ByteSource,
    cellsPerDegree: Int,
    header: ByteArray
) {

    /**
     * Reads a range of the index file, so only the header and the tiles that are queried are held in memory.
     */
    interface ByteSource {
        suspend fun read(offset: Int, size: Int): ByteArray
    }

    private class Tile(val offset: Int, val size: Int, val count: Int)

    private val tileBits = header[0].toInt()
    private val tileColumns = (360 * cellsPerDegree) / (1 shl tileBits) + 1
    private val tiles = mutableMapOf<Int, Tile>()
    private val lock = Mutex()

    // The symbols are ordered by code length, then by value
    private val symbols: IntArray
    private val codesPerLength: IntArray

    private val cache = object : LinkedHashMap<Int, LongArray>(CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, LongArray>?): Boolean {
            return size > CACHE_SIZE
        }
    }

    init {
        var position = 1
        val symbolCount = header[position++].toInt()
        val lengths = IntArray(symbolCount) { header[position++].toInt() }
        val maxLength = lengths.max()
        codesPerLength = IntArray(maxLength + 1)
        lengths.forEach { codesPerLength[it]++ }
        symbols = lengths.indices.sortedWith(compareBy({ lengths[it] }, { it })).toIntArray()

        fun varint(): Int {
            var value = 0
            var shift = 0
            while (true) {
                val byte = header[position++].toInt() and 0xFF
                value = value or ((byte and 127) shl shift)
                if (byte < 128) {
                    return value
                }
                shift += 7
            }
        }

        val tileCount = varint()
        val ids = IntArray(tileCount)
        val counts = IntArray(tileCount)
        val sizes = IntArray(tileCount)
        var id = 0
        for (i in 0 until tileCount) {
            id += varint()
            ids[i] = id
            counts[i] = varint()
            sizes[i] = varint()
        }
        var offset = position
        for (i in 0 until tileCount) {
            tiles[ids[i]] = Tile(offset, sizes[i], counts[i])
            offset += sizes[i]
        }
    }

    /**
     * Gets the cells in the range (inclusive) as packed values, the row in the upper 32 bits and the column in the lower 32 bits.
     */
    suspend fun getCells(
        minRow: Int,
        maxRow: Int,
        minColumn: Int,
        maxColumn: Int
    ): List<Long> = lock.withLock {
        val cells = mutableListOf<Long>()
        for (tileRow in (minRow shr tileBits)..(maxRow shr tileBits)) {
            for (tileColumn in (minColumn shr tileBits)..(maxColumn shr tileBits)) {
                val id = tileRow * tileColumns + tileColumn
                val tileCells = cache[id] ?: decode(id, tileRow, tileColumn).also { cache[id] = it }
                for (cell in tileCells) {
                    val row = (cell shr 32).toInt()
                    val column = cell.toInt()
                    if (row in minRow..maxRow && column in minColumn..maxColumn) {
                        cells.add(cell)
                    }
                }
            }
        }
        cells
    }

    private suspend fun decode(id: Int, tileRow: Int, tileColumn: Int): LongArray {
        val tile = tiles[id] ?: return LongArray(0)
        val payload = source.read(tile.offset, tile.size)
        var bitPosition = 0L

        fun readBit(): Int {
            val bit = (payload[(bitPosition shr 3).toInt()].toInt() shr (7 - (bitPosition and 7).toInt())) and 1
            bitPosition++
            return bit
        }

        fun readSymbol(): Int {
            var code = 0
            var first = 0
            var index = 0
            for (length in 1 until codesPerLength.size) {
                code = code or readBit()
                val count = codesPerLength[length]
                if (code - count < first) {
                    return symbols[index + code - first]
                }
                index += count
                first = (first + count) shl 1
                code = code shl 1
            }
            throw IllegalStateException("Invalid cell tower index")
        }

        val rowStart = tileRow shl tileBits
        val columnStart = tileColumn shl tileBits
        val cells = LongArray(tile.count)
        var morton = -1
        for (i in 0 until tile.count) {
            val bits = readSymbol()
            var gap = 1 shl bits
            for (b in bits - 1 downTo 0) {
                gap = gap or (readBit() shl b)
            }
            morton += gap

            var row = 0
            var column = 0
            for (b in 0 until tileBits) {
                row = row or (((morton shr (2 * b)) and 1) shl b)
                column = column or (((morton shr (2 * b + 1)) and 1) shl b)
            }
            cells[i] = ((rowStart + row).toLong() shl 32) or (columnStart + column).toLong()
        }
        return cells
    }

    companion object {
        private const val CACHE_SIZE = 8

        // The header has at most one entry per tile of the world (a few bytes each)
        private const val MAX_HEADER_SIZE = 64 * 1024

        suspend fun open(source: ByteSource, cellsPerDegree: Int): CellTowerIndex {
            return CellTowerIndex(source, cellsPerDegree, source.read(0, MAX_HEADER_SIZE))
        }
    }
}
