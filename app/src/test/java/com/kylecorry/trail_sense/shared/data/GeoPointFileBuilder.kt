package com.kylecorry.trail_sense.shared.data

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.floor

/**
 * Writes a geo point file the way the earth model generator does (scripts/geo_points.py), for tests.
 */
class GeoPointFileBuilder(
    private val cellDegrees: Int = 5,
    private val bitsPerAxis: Int = 12,
    private val valueBytes: Int = 1,
    private val valueStep: Float = 1f,
    private val version: Int = 1
) {
    private class Point(val latitude: Double, val longitude: Double, val value: Long)

    private val points = ArrayList<Point>()
    private val rows = 180 / cellDegrees
    private val columns = 360 / cellDegrees
    private val positionBytes = (2 * bitsPerAxis + 7) / 8

    fun add(latitude: Double, longitude: Double, value: Long = 1): GeoPointFileBuilder {
        points.add(Point(latitude, longitude, value))
        return this
    }

    private fun cellOf(point: Point): Int {
        val row = floor((point.latitude + 90) / cellDegrees).toInt()
        val column = floor((point.longitude + 180) / cellDegrees).toInt()
        return row * columns + column
    }

    fun build(): ByteArray {
        val sorted = points.sortedWith(compareBy({ cellOf(it) }, { it.latitude }, { it.longitude }))
        val buffer = ByteBuffer.allocate(12 + (rows * columns + 1) * 4 + (positionBytes + valueBytes) * points.size)
            .order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(version.toByte()).put(cellDegrees.toByte()).put(valueBytes.toByte()).put(bitsPerAxis.toByte())
        buffer.putInt(points.size).putFloat(valueStep)

        var before = 0
        for (cell in 0 until rows * columns) {
            buffer.putInt(before)
            before += sorted.count { cellOf(it) == cell }
        }
        buffer.putInt(before)

        val steps = 1L shl bitsPerAxis
        val records = ArrayList<ByteArray>()
        var previousCell = -1
        var previousY = 0L
        for (point in sorted) {
            val cell = cellOf(point)
            val south = (cell / columns) * cellDegrees - 90.0
            val west = (cell % columns) * cellDegrees - 180.0
            val y = floor((point.latitude - south) / cellDegrees * steps).toLong()
            val x = floor((point.longitude - west) / cellDegrees * steps).toLong()
            val yStep = if (cell == previousCell) y - previousY else y
            previousCell = cell
            previousY = y

            val record = ByteBuffer.allocate(positionBytes + valueBytes)
            putUnsigned(record, x or (yStep shl bitsPerAxis), positionBytes)
            putUnsigned(record, point.value, valueBytes)
            records.add(record.array())
        }
        // The records are stored by byte
        for (byteIndex in 0 until positionBytes + valueBytes) {
            for (record in records) {
                buffer.put(record[byteIndex])
            }
        }
        return buffer.array()
    }

    private fun putUnsigned(buffer: ByteBuffer, value: Long, bytes: Int) {
        for (i in 0 until bytes) {
            buffer.put(((value shr (8 * i)) and 0xFF).toByte())
        }
    }
}
