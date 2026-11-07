package com.kylecorry.trail_sense.shared.andromeda_temp.geopoint

import com.kylecorry.andromeda.json.JsonConvert
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.Deflater
import kotlin.math.roundToLong

/**
 * Writes a geo point file the way the earth model generator does, for tests.
 */
class GeoPointFileBuilder(
    private val cellDegrees: Int = 5,
    private val decimals: Int = 4,
    private val fields: List<Field> = listOf(Field("value", 1)),
    private val blockPoints: Int = 2048,
    private val properties: Map<String, Any> = emptyMap(),
    private val version: Int = 1,
    private val magic: String = "TSGP"
) {
    class Field(val name: String, val bytes: Int, val step: Double = 1.0, val offset: Double = 0.0)

    private class Point(val cell: Int, val code: Long, val values: LongArray)

    private val points = ArrayList<Point>()
    private val columns = 360 / cellDegrees
    private val scale = (1..decimals).fold(1L) { value, _ -> value * 10 }
    private val steps = cellDegrees * scale
    private val bits = 64 - java.lang.Long.numberOfLeadingZeros(steps - 1)

    /**
     * Adds a point with the stored (not scaled) value of each field.
     */
    fun add(latitude: Double, longitude: Double, vararg values: Long): GeoPointFileBuilder {
        val latitudeSteps = ((latitude + 90) * scale).roundToLong().coerceIn(0, 180 * scale - 1)
        val longitudeSteps = Math.floorMod(((longitude + 180) * scale).roundToLong(), 360 * scale)
        val cell = (latitudeSteps / steps * columns + longitudeSteps / steps).toInt()
        val code = interleave(latitudeSteps % steps, longitudeSteps % steps)
        points.add(Point(cell, code, LongArray(fields.size) { values.getOrElse(it) { 1 } }))
        return this
    }

    fun build(): ByteArray {
        val sorted = points.sortedWith(compareBy({ it.cell }, { it.code }))
        val byCell = sorted.groupBy { it.cell }.toSortedMap()

        val blocks = ArrayList<Triple<Int, Int, ByteArray>>()
        var current = ArrayList<Int>()
        var currentPoints = 0
        fun flush() {
            if (current.isNotEmpty()) {
                blocks.add(Triple(current.first(), current.last(), packBlock(current.map { it to byCell.getValue(it) })))
            }
            current = ArrayList()
            currentPoints = 0
        }
        for ((cell, cellPoints) in byCell) {
            if (current.isNotEmpty() && currentPoints + cellPoints.size > blockPoints) {
                flush()
            }
            current.add(cell)
            currentPoints += cellPoints.size
        }
        flush()

        val metadata = JsonConvert.toJson(
            mapOf(
                "cell_degrees" to cellDegrees,
                "decimals" to decimals,
                "count" to points.size,
                "fields" to fields.map {
                    mapOf("name" to it.name, "bytes" to it.bytes, "step" to it.step, "offset" to it.offset)
                },
                "properties" to properties
            )
        ).toByteArray(Charsets.UTF_8)

        val output = ByteArrayOutputStream()
        val header = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
        header.put(magic.toByteArray(Charsets.US_ASCII)).put(version.toByte()).put(0).put(0).put(0)
        header.putInt(metadata.size).putInt(blocks.size)
        output.write(header.array())
        output.write(metadata)

        val directory = ByteBuffer.allocate((blocks.size + 1) * 12).order(ByteOrder.LITTLE_ENDIAN)
        var offset = 16 + metadata.size + (blocks.size + 1) * 12
        for ((firstCell, lastCell, block) in blocks) {
            directory.putInt(firstCell).putInt(lastCell).putInt(offset)
            offset += block.size
        }
        val cellCount = (180 / cellDegrees) * columns
        directory.putInt(cellCount).putInt(cellCount).putInt(offset)
        output.write(directory.array())
        blocks.forEach { output.write(it.third) }
        return output.toByteArray()
    }

    private fun packBlock(cells: List<Pair<Int, List<Point>>>): ByteArray {
        val raw = ByteArrayOutputStream()
        writeVarint(raw, cells.size.toLong())
        var previous = cells.first().first - 1
        for ((cell, _) in cells) {
            writeVarint(raw, (cell - previous - 1).toLong())
            previous = cell
        }
        cells.forEach { writeVarint(raw, it.second.size.toLong()) }
        for ((_, cellPoints) in cells) {
            var code = 0L
            for (point in cellPoints) {
                writeVarint(raw, point.code - code)
                code = point.code
            }
        }
        val all = cells.flatMap { it.second }
        for ((index, field) in fields.withIndex()) {
            for (b in 0 until field.bytes) {
                all.forEach { raw.write(((it.values[index] shr (8 * b)) and 0xFF).toInt()) }
            }
        }

        val deflater = Deflater(9, true)
        deflater.setInput(raw.toByteArray())
        deflater.finish()
        val compressed = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        while (!deflater.finished()) {
            compressed.write(buffer, 0, deflater.deflate(buffer))
        }
        deflater.end()
        return compressed.toByteArray()
    }

    // The bits of x at the even bits of the code and of y at the odd bits
    private fun interleave(y: Long, x: Long): Long {
        var code = 0L
        for (i in 0 until bits) {
            code = code or (((x shr i) and 1L) shl (2 * i))
            code = code or (((y shr i) and 1L) shl (2 * i + 1))
        }
        return code
    }

    private fun writeVarint(output: ByteArrayOutputStream, value: Long) {
        var remaining = value
        while (remaining >= 0x80) {
            output.write(((remaining and 0x7F) or 0x80).toInt())
            remaining = remaining ushr 7
        }
        output.write(remaining.toInt())
    }
}
