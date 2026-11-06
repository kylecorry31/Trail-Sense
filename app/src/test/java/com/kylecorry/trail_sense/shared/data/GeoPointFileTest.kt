package com.kylecorry.trail_sense.shared.data

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GeoPointFileTest {

    private fun open(builder: GeoPointFileBuilder): GeoPointFile = runBlocking {
        GeoPointFile.open(ByteRangeReader.fromBytes(builder.build()))!!
    }

    @Test
    fun readsThePositionAndValueOfAPoint() = runBlocking {
        val file = open(GeoPointFileBuilder(valueStep = 16f).add(44.2706, -71.3033, 20))

        val cell = file.getCells(44.0, 45.0, -72.0, -71.0).single()

        assertEquals(1, cell.size)
        // The position is rounded to 1/4096 of a 5 degree cell
        assertEquals(44.2706, cell.latitudes[0], 0.0007)
        assertEquals(-71.3033, cell.longitudes[0], 0.0007)
        assertEquals(320f, cell.values[0], 0f)
    }

    @Test
    fun supportsOtherPositionAndValueSizes() = runBlocking {
        val file = open(
            GeoPointFileBuilder(cellDegrees = 15, bitsPerAxis = 16, valueBytes = 3, valueStep = 0.5f)
                .add(-33.9249, 18.4241, 1_000_000)
        )

        val cell = file.getCells(-34.0, -33.0, 18.0, 19.0).single()

        assertEquals(-33.9249, cell.latitudes[0], 0.0003)
        assertEquals(18.4241, cell.longitudes[0], 0.0003)
        assertEquals(500_000f, cell.values[0], 0f)
    }

    @Test
    fun sortsThePointsOfACellByLatitude() = runBlocking {
        val file = open(
            GeoPointFileBuilder()
                .add(44.9, -71.0, 3).add(44.1, -71.2, 1).add(44.5, -71.4, 2)
        )

        val cell = file.getCells(44.0, 45.0, -72.0, -71.0).single()

        assertEquals(listOf(1f, 2f, 3f), cell.values.toList())
        assertEquals(1, cell.firstIndexAtOrAbove(44.3))
        assertEquals(0, cell.firstIndexAtOrAbove(40.0))
        assertEquals(3, cell.firstIndexAtOrAbove(46.0))
    }

    @Test
    fun returnsOnlyTheCellsWithPointsInTheArea() = runBlocking {
        val file = open(GeoPointFileBuilder().add(44.0, -71.0).add(46.0, -71.0).add(10.0, 10.0))

        // 45 degrees is the edge between the first two points' cells
        assertEquals(2, file.getCells(44.0, 46.0, -72.0, -70.0).size)
        assertEquals(1, file.getCells(44.0, 44.5, -72.0, -70.0).size)
        assertEquals(0, file.getCells(0.0, 1.0, 0.0, 1.0).size)
    }

    @Test
    fun wrapsAroundTheAntimeridian() = runBlocking {
        val file = open(GeoPointFileBuilder().add(10.0, 179.0).add(10.0, -179.0))

        assertEquals(2, file.getCells(9.0, 11.0, 178.0, 182.0).size)
        assertEquals(2, file.getCells(9.0, 11.0, -182.0, -178.0).size)
        assertEquals(1, file.getCells(9.0, 11.0, 178.0, 179.5).size)
    }

    @Test
    fun readsEachCellOnlyOnce() = runBlocking {
        val bytes = GeoPointFileBuilder().add(44.0, -71.0).add(-30.0, 120.0).build()
        var reads = 0
        val file = GeoPointFile.open { offset, length ->
            reads++
            ByteRangeReader.fromBytes(bytes).read(offset, length)
        }!!
        val readByOpening = reads

        file.getCells(44.0, 45.0, -72.0, -71.0)
        val readByFirst = reads
        file.getCells(44.0, 45.0, -72.0, -71.0)

        // One read for each byte of a record (3 for the position and 1 for the value)
        assertEquals(4, readByFirst - readByOpening)
        assertEquals(readByFirst, reads)
    }

    @Test
    fun ignoresFilesItDoesNotUnderstand() = runBlocking {
        suspend fun open(bytes: ByteArray) = GeoPointFile.open(ByteRangeReader.fromBytes(bytes))

        assertNull(open(ByteArray(0)))
        assertNull(open(GeoPointFileBuilder(version = 2).add(1.0, 1.0).build()))
        assertNull(open(GeoPointFileBuilder(cellDegrees = 7).build()))
        assertNull(open(GeoPointFileBuilder().add(1.0, 1.0).build().copyOf(100)))
        assertNotNull(open(GeoPointFileBuilder().build()))
    }

    @Test
    fun triesACellAgainIfItFailedToRead() = runBlocking {
        val bytes = GeoPointFileBuilder().add(44.0, -71.0).build()
        var failing = false
        val file = GeoPointFile.open { offset, length ->
            if (failing) throw java.io.IOException()
            ByteRangeReader.fromBytes(bytes).read(offset, length)
        }!!

        failing = true
        assertEquals(0, file.getCells(44.0, 45.0, -72.0, -71.0).size)
        failing = false
        assertEquals(1, file.getCells(44.0, 45.0, -72.0, -71.0).sumOf { it.size })
    }

    @Test
    fun hasNoPointsWhereTheFileCannotBeRead() = runBlocking {
        val bytes = GeoPointFileBuilder().add(10.0, 10.0).add(44.0, -71.0).build()
        val file = GeoPointFile.open(ByteRangeReader.fromBytes(bytes.copyOf(bytes.size - 1)))!!

        // The cells are in order from the south, so the last record of the file belongs to the cell at 44 degrees
        assertEquals(1, file.getCells(10.0, 11.0, 10.0, 11.0).sumOf { it.size })
        assertEquals(0, file.getCells(44.0, 45.0, -72.0, -71.0).sumOf { it.size })
    }
}
