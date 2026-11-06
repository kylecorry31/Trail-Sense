package com.kylecorry.trail_sense.shared.dem

import com.kylecorry.trail_sense.shared.data.ByteRangeReader
import com.kylecorry.trail_sense.shared.data.GeoPointFileBuilder
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.cos

class PeakOverlayTest {

    // A scale of 1 (63 steps of 1/63) and a base of 400 m (25 steps of 16 m)
    private val value = 63L or (25L shl 7)

    private fun open(vararg summits: Pair<Double, Double>): PeakOverlay = runBlocking {
        val builder = GeoPointFileBuilder(valueBytes = 2)
        summits.forEach { builder.add(it.first, it.second, value) }
        PeakOverlay.open(ByteRangeReader.fromBytes(builder.build()))!!
    }

    private val summit = 44.0 to -71.0

    // The position is rounded to about 68 m
    @Test
    fun scalesTheElevationAboveTheBaseAtTheSummit() = runBlocking {
        assertEquals(600f, open(summit).getOffset(44.0, -71.0, 1000f), 5f)
    }

    @Test
    fun addsNothingBelowTheBase() = runBlocking {
        assertEquals(0f, open(summit).getOffset(44.0, -71.0, 300f), 0f)
    }

    @Test
    fun followsTheElevationOfTheModel() = runBlocking {
        val overlay = open(summit)

        assertEquals(1200f, overlay.getOffset(44.0, -71.0, 1600f), 10f)
        assertEquals(300f, overlay.getOffset(44.0, -71.0, 700f), 5f)
    }

    @Test
    fun fadesToNothingAtTheRadius() = runBlocking {
        val overlay = open(summit)
        val degreesFor4Km = 4.0 / 111.0

        assertEquals(0f, overlay.getOffset(44.0 + degreesFor4Km * 1.02, -71.0, 1000f), 0f)
        assertEquals(0f, overlay.getOffset(44.0 + degreesFor4Km * 2, -71.0, 1000f), 0f)
        // Halfway there the amount is (1 - 0.25)^2 of what it is at the summit
        assertEquals(600f * 0.5625f, overlay.getOffset(44.0 + degreesFor4Km / 2, -71.0, 1000f), 15f)
    }

    @Test
    fun usesTheDistanceOnTheGroundInLongitude() = runBlocking {
        val overlay = open(summit)
        val degreesFor4Km = 4.0 / (111.0 * cos(Math.toRadians(44.0)))

        assertEquals(0f, overlay.getOffset(44.0, -71.0 + degreesFor4Km * 1.02, 1000f), 0f)
        assertTrue(overlay.getOffset(44.0, -71.0 + degreesFor4Km * 0.9, 1000f) > 0f)
    }

    @Test
    fun nearbySummitsAddUp() = runBlocking {
        assertEquals(1200f, open(summit, summit).getOffset(44.0, -71.0, 1000f), 10f)
    }

    @Test
    fun findsASummitInTheNextCell() = runBlocking {
        // 45 degrees is on the edge of two cells
        val overlay = open(44.999 to -71.0)

        assertTrue(overlay.getOffset(45.001, -71.0, 1000f) > 500f)
        assertTrue(overlay.getOffset(44.998, -71.0, 1000f) > 500f)
    }

    @Test
    fun wrapsAroundTheAntimeridian() = runBlocking {
        val overlay = open(10.0 to 179.995)

        assertTrue(overlay.getOffset(10.0, -179.995, 1000f) > 100f)
        assertTrue(overlay.getOffset(10.0, 179.99, 1000f) > 100f)
    }

    @Test
    fun addsToAGridOnlyWhereThereIsData() = runBlocking {
        val overlay = open(summit)
        val latitudes = doubleArrayOf(43.99, 44.0, 44.01)
        val longitudes = doubleArrayOf(-71.01, -71.0, -70.5)
        // Two channels, the second is not changed
        val data = FloatArray(3 * 3 * 2) { 1000f }
        data[(1 * 3 + 0) * 2] = 0f

        overlay.addTo(latitudes, longitudes, data, 2)

        assertEquals(1000f + overlay.getOffset(44.0, -71.0, 1000f), data[(1 * 3 + 1) * 2], 0.01f)
        assertEquals(1000f + overlay.getOffset(43.99, -71.01, 1000f), data[0], 0.01f)
        assertEquals(0f, data[(1 * 3 + 0) * 2], 0f)
        assertEquals(1000f, data[(1 * 3 + 2) * 2], 0f)
        assertEquals(1000f, data[(1 * 3 + 1) * 2 + 1], 0f)
    }

    private fun openCountingReads(lat: Double, lon: Double): Pair<PeakOverlay, () -> Int> = runBlocking {
        val builder = GeoPointFileBuilder(valueBytes = 2)
        builder.add(lat, lon, value)
        val bytes = ByteRangeReader.fromBytes(builder.build())
        var reads = 0
        val overlay = PeakOverlay.open { offset, length ->
            reads++
            bytes.read(offset, length)
        }!!
        val start = reads
        overlay to { reads - start }
    }

    @Test
    fun addsToAGridCrossingTheAntimeridian() = runBlocking {
        val overlay = open(10.0 to 179.995)
        val latitudes = doubleArrayOf(10.0)
        val longitudes = doubleArrayOf(179.99, -179.995, -179.98)
        val data = FloatArray(3) { 1000f }

        overlay.addTo(latitudes, longitudes, data, 1)

        assertEquals(1000f + overlay.getOffset(10.0, 179.99, 1000f), data[0], 0.01f)
        assertEquals(1000f + overlay.getOffset(10.0, -179.995, 1000f), data[1], 0.01f)
        assertTrue(data[0] > 1100f)
        assertTrue(data[1] > 1100f)
    }

    @Test
    fun doesNotReadDistantCellsForAGridCrossingTheAntimeridian() = runBlocking {
        val (overlay, reads) = openCountingReads(10.0, 0.0)

        overlay.addTo(doubleArrayOf(10.0), doubleArrayOf(179.99, -179.99), FloatArray(2) { 1000f }, 1)

        assertEquals(0, reads())
    }

    @Test
    fun readsTheCellsOfAGridNotCrossingTheAntimeridian() = runBlocking {
        val (overlay, reads) = openCountingReads(10.0, 0.0)
        val data = FloatArray(2) { 1000f }

        overlay.addTo(doubleArrayOf(10.0), doubleArrayOf(-0.01, 0.01), data, 1)

        assertTrue(reads() > 0)
        assertTrue(data[0] > 1000f)
    }

    @Test
    fun readsTheWholeWorldForAGlobalGrid() = runBlocking {
        val (overlay, reads) = openCountingReads(10.0, 0.0)

        overlay.addTo(doubleArrayOf(10.0), DoubleArray(8) { -180.0 + it * 45.0 }, FloatArray(8) { 1000f }, 1)

        assertTrue(reads() > 0)
    }

    @Test
    fun ignoresFilesItDoesNotUnderstand() = runBlocking {
        assertNull(PeakOverlay.open(ByteRangeReader.fromBytes(ByteArray(0))))
    }
}
