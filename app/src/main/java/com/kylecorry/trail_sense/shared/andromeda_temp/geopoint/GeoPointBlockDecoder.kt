package com.kylecorry.trail_sense.shared.andromeda_temp.geopoint

import com.kylecorry.trail_sense.shared.andromeda_temp.BytePlanes
import com.kylecorry.trail_sense.shared.andromeda_temp.VarintReader

/**
 * Decodes a decompressed block of a [GeoPointFile] into its cells.
 */
class GeoPointBlockDecoder(private val grid: GeoPointGrid, private val fields: List<GeoPointField>) {

    /**
     * The cells of a block that holds cells [firstCell] to [lastCell], throwing an [IllegalArgumentException] (or an
     * [IndexOutOfBoundsException]) if it is not valid.
     */
    fun decode(data: ByteArray, firstCell: Int, lastCell: Int): Map<Int, GeoPointCell> {
        val reader = VarintReader(data)
        val cellIds = readCellIds(reader, firstCell, lastCell)
        val counts = IntArray(cellIds.size) { reader.nextInt() }
        val total = counts.fold(0L) { sum, count -> sum + count }
        val fieldBytes = fields.sumOf { it.bytes }
        require(total * fieldBytes <= data.size - reader.offset) { "The block has more points than bytes" }
        val positions = Array(cellIds.size) { readPositions(reader, counts[it]) }
        val fieldValues = readFields(data, reader.offset, total.toInt())

        val cells = HashMap<Int, GeoPointCell>(cellIds.size)
        var firstPoint = 0
        for (i in cellIds.indices) {
            val cell = cellIds[i]
            // The points are stored in Morton order, so they are sorted by position (latitude, then longitude) here
            val sortedIndices = positions[i].indices.sortedBy { positions[i][it] }
            val cellPositions = LongArray(counts[i]) { positions[i][sortedIndices[it]] }
            val values = fields.indices.associate { index ->
                fields[index].name to FloatArray(counts[i]) { fieldValues[index][firstPoint + sortedIndices[it]] }
            }
            cells[cell] = GeoPointCell(
                DoubleArray(counts[i]) { grid.getLatitude(cell, cellPositions[it]) },
                DoubleArray(counts[i]) { grid.getLongitude(cell, cellPositions[it]) },
                values
            )
            firstPoint += counts[i]
        }
        return cells
    }

    // Each cell is stored as the gap after the one before it
    private fun readCellIds(reader: VarintReader, firstCell: Int, lastCell: Int): IntArray {
        val count = reader.nextInt()
        require(count <= lastCell - firstCell + 1) { "The block has more cells than its range" }
        var cell = firstCell - 1
        return IntArray(count) {
            cell += reader.nextInt() + 1
            require(cell <= lastCell) { "A cell is outside of the block" }
            cell
        }
    }

    // Each point is stored as the difference from the one before it in its cell, in Morton order
    private fun readPositions(reader: VarintReader, count: Int): LongArray {
        var code = 0L
        return LongArray(count) {
            code += reader.nextLong()
            grid.getPosition(code)
        }
    }

    private fun readFields(data: ByteArray, offset: Int, count: Int): List<FloatArray> {
        var planeStart = offset
        return fields.map { field ->
            val stored = BytePlanes.readUnsigned(data, planeStart, count, field.bytes)
            planeStart += field.bytes * count
            FloatArray(count) { field.decode(stored[it]) }
        }
    }
}
