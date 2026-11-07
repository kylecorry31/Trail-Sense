package com.kylecorry.trail_sense.shared.andromeda_temp.geopoint

import com.kylecorry.trail_sense.shared.andromeda_temp.ByteRangeReader
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

class GeoPointFileTest {

    private fun open(builder: GeoPointFileBuilder): GeoPointFile = runBlocking {
        GeoPointFile.open(ByteRangeReader.fromBytes(builder.build()))!!
    }

    private fun countingReader(bytes: ByteArray, onRead: () -> Unit): ByteRangeReader {
        val reader = ByteRangeReader.fromBytes(bytes)
        return ByteRangeReader { offset, length ->
            onRead()
            reader.read(offset, length)
        }
    }

    @Test
    fun readsThePositionAndValuesOfAPoint() = runBlocking {
        val file = open(
            GeoPointFileBuilder(
                decimals = 4,
                fields = listOf(
                    GeoPointFileBuilder.Field("a", 1),
                    GeoPointFileBuilder.Field("b", 2, step = 16.0, offset = -100.0)
                )
            ).add(44.2706, -71.3033, 20, 7)
        )

        val cell = file.getCells(44.0, 45.0, -72.0, -71.0).single()

        assertEquals(1, cell.size)
        assertEquals(44.2706, cell.latitudes[0], 1e-9)
        assertEquals(-71.3033, cell.longitudes[0], 1e-9)
        assertArrayEquals(floatArrayOf(20f), cell.getValues("a"))
        assertArrayEquals(floatArrayOf(12f), cell.getValues("b"))
        assertNull(cell.getValues("c"))
        assertTrue(file.hasField("b"))
        assertFalse(file.hasField("c"))
    }

    @Test
    fun supportsOtherPrecisionsAndFieldSizes() = runBlocking {
        val file = open(
            GeoPointFileBuilder(
                cellDegrees = 15,
                decimals = 7,
                fields = listOf(GeoPointFileBuilder.Field("value", 3, step = 0.5))
            ).add(-33.9249123, 18.4241456, 1_000_000)
        )

        val cell = file.getCells(-34.0, -33.0, 18.0, 19.0).single()

        assertEquals(-33.9249123, cell.latitudes[0], 1e-9)
        assertEquals(18.4241456, cell.longitudes[0], 1e-9)
        assertArrayEquals(floatArrayOf(500_000f), cell.getValues("value"))
    }

    @Test
    fun supportsFilesWithoutFields() = runBlocking {
        val file = open(GeoPointFileBuilder(fields = emptyList()).add(10.0, 10.0))

        assertEquals(1, file.getCells(9.0, 11.0, 9.0, 11.0).single().size)
    }

    @Test
    fun sortsThePointsOfACellByLatitude() = runBlocking {
        val file = open(GeoPointFileBuilder().add(44.9, -71.0, 3).add(44.1, -71.2, 1).add(44.5, -71.4, 2))

        val cell = file.getCells(44.0, 45.0, -72.0, -71.0).single()

        assertEquals(listOf(1f, 2f, 3f), cell.getValues("value")!!.toList())
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
        assertEquals(0, file.getCells(-89.0, -88.0, -179.0, -178.0).size)
    }

    @Test
    fun wrapsAroundTheAntimeridian() = runBlocking {
        val file = open(GeoPointFileBuilder().add(10.0, 179.0).add(10.0, -179.0))

        assertEquals(2, file.getCells(9.0, 11.0, 178.0, 182.0).size)
        assertEquals(2, file.getCells(9.0, 11.0, -182.0, -178.0).size)
        assertEquals(1, file.getCells(9.0, 11.0, 178.0, 179.5).size)
    }

    @Test
    fun readsTheCellsOfABlockWithOneReadAndKeepsThem() = runBlocking {
        val bytes = GeoPointFileBuilder().add(44.0, -71.0).add(44.0, -66.0).add(44.0, -61.0).add(10.0, 10.0).build()
        var reads = 0
        val file = GeoPointFile.open(countingReader(bytes) { reads++ })!!
        val readByOpening = reads

        assertEquals(3, file.getCells(44.0, 44.5, -72.0, -60.0).size)
        assertEquals(1, reads - readByOpening)
        file.getCells(10.0, 10.5, 10.0, 10.5)
        file.getCells(44.0, 44.5, -72.0, -60.0)
        assertEquals(1, reads - readByOpening)
    }

    @Test
    fun readsOnlyTheBlocksThatAreNeeded() = runBlocking {
        val bytes = GeoPointFileBuilder(blockPoints = 1).add(44.0, -71.0).add(44.0, -66.0).add(10.0, 10.0).build()
        var reads = 0
        val file = GeoPointFile.open(countingReader(bytes) { reads++ })!!
        val readByOpening = reads

        assertEquals(1, file.getCells(44.0, 44.5, -72.0, -70.5).size)
        assertEquals(1, reads - readByOpening)
        assertEquals(2, file.getCells(44.0, 44.5, -72.0, -65.0).size)
        assertEquals(2, reads - readByOpening)
    }

    @Test
    fun hasTheProperties() = runBlocking {
        val file = open(GeoPointFileBuilder(properties = mapOf("type" to "peaks", "radius_meters" to 4000)))

        assertEquals("peaks", file.getStringProperty("type"))
        assertEquals(4000.0, file.getNumberProperty("radius_meters"))
        assertNull(file.getNumberProperty("type"))
        assertNull(file.getStringProperty("missing"))
    }

    @Test
    fun readsAFileWrittenByTheGenerator() = runBlocking {
        // Written by generate-geo-point-test-data.py in the earth model generator
        val bytes = javaClass.getResourceAsStream("/geo_points/generator_sample.tsgeo")!!.use { it.readBytes() }
        val file = GeoPointFile.open(ByteRangeReader.fromBytes(bytes))!!

        assertEquals("test", file.getStringProperty("type"))
        assertEquals(1234.5, file.getNumberProperty("radius_meters"))

        val washington = file.getCells(44.0, 44.5, -72.0, -71.0).single()
        assertEquals(listOf(44.27, 44.270583), washington.latitudes.toList())
        assertEquals(listOf(-71.3, -71.303333), washington.longitudes.toList())
        assertEquals(listOf(4f, 3f), washington.getValues("kind")!!.toList())
        assertEquals(listOf(1000f, 1916.5f), washington.getValues("height")!!.toList())

        val capeTown = file.getCells(-34.0, -33.0, 18.0, 19.0).single()
        assertEquals(-33.9249, capeTown.latitudes.single(), 1e-9)
        assertEquals(18.4241, capeTown.longitudes.single(), 1e-9)
        assertEquals(12f, capeTown.getValues("height")!!.single())

        val antimeridian = file.getCells(9.0, 11.0, 179.0, 180.0).single()
        assertEquals(179.999999, antimeridian.longitudes.single(), 1e-9)
        assertEquals(-100f, antimeridian.getValues("height")!!.single())

        val southPole = file.getCells(-90.0, -89.0, -180.0, -179.0).single()
        assertEquals(-90.0, southPole.latitudes.single(), 1e-9)
        assertEquals(-180.0, southPole.longitudes.single(), 1e-9)
    }

    @Test
    fun ignoresFilesItDoesNotUnderstand() = runBlocking {
        suspend fun open(bytes: ByteArray) = GeoPointFile.open(ByteRangeReader.fromBytes(bytes))

        val valid = GeoPointFileBuilder().add(1.0, 1.0).build()
        assertNotNull(open(valid))
        assertNotNull(open(GeoPointFileBuilder().build()))
        assertNull(open(ByteArray(0)))
        assertNull(open(GeoPointFileBuilder(magic = "NOPE").add(1.0, 1.0).build()))
        assertNull(open(GeoPointFileBuilder(version = 2).add(1.0, 1.0).build()))
        assertNull(open(GeoPointFileBuilder(cellDegrees = 7).build()))
        assertNull(open(GeoPointFileBuilder(fields = listOf(GeoPointFileBuilder.Field("value", 5))).build()))
        // Cut off in the directory
        assertNull(open(valid.copyOf(valid.size - 12)))
    }

    @Test
    fun triesABlockAgainIfItFailedToRead() = runBlocking {
        val bytes = GeoPointFileBuilder().add(44.0, -71.0).build()
        var failing = false
        val file = GeoPointFile.open { offset, length ->
            if (failing) throw IOException()
            ByteRangeReader.fromBytes(bytes).read(offset, length)
        }!!

        failing = true
        assertEquals(0, file.getCells(44.0, 45.0, -72.0, -71.0).size)
        failing = false
        assertEquals(1, file.getCells(44.0, 45.0, -72.0, -71.0).size)
    }

    @Test
    fun hasNoPointsWhereTheFileCannotBeRead() = runBlocking {
        val bytes = GeoPointFileBuilder(blockPoints = 1).add(10.0, 10.0).add(44.0, -71.0).build()
        val file = GeoPointFile.open(ByteRangeReader.fromBytes(bytes.copyOf(bytes.size - 1)))!!

        // The blocks are in order from the south, so the last block of the file has the cell at 44 degrees
        assertEquals(1, file.getCells(10.0, 11.0, 10.0, 11.0).size)
        assertEquals(0, file.getCells(44.0, 45.0, -72.0, -71.0).size)
    }

    @Test
    fun hasNoPointsWhereABlockIsCorrupt() = runBlocking {
        val bytes = GeoPointFileBuilder().add(44.0, -71.0).build()
        bytes[bytes.size - 3] = (bytes[bytes.size - 3].toInt() xor 0xFF).toByte()
        val file = GeoPointFile.open(ByteRangeReader.fromBytes(bytes))!!

        // Either the block does not inflate or it does not decode, but it does not throw
        assertTrue(file.getCells(44.0, 45.0, -72.0, -71.0).size <= 1)
    }
}
