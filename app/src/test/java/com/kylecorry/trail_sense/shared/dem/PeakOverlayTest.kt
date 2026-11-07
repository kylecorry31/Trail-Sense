package com.kylecorry.trail_sense.shared.dem

import com.kylecorry.trail_sense.shared.andromeda_temp.ByteRangeReader
import com.kylecorry.trail_sense.shared.andromeda_temp.geopoint.GeoPointFileBuilder
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.cos

class PeakOverlayTest {

    private val fields = listOf(
        GeoPointFileBuilder.Field("scale", 1, step = 1 / 64.0),
        GeoPointFileBuilder.Field("base", 2, step = 16.0)
    )

    private fun builder(
        radiusMeters: Any? = 4000,
        type: String = "peaks",
        fields: List<GeoPointFileBuilder.Field> = this.fields
    ): GeoPointFileBuilder {
        val properties = mutableMapOf<String, Any>("type" to type)
        radiusMeters?.let { properties["radius_meters"] = it }
        return GeoPointFileBuilder(decimals = 4, fields = fields, properties = properties)
    }

    // A scale of 1 (64 steps of 1/64) and a base of 400 m (25 steps of 16 m)
    private fun GeoPointFileBuilder.addSummit(latitude: Double, longitude: Double): GeoPointFileBuilder {
        return add(latitude, longitude, 64, 25)
    }

    private fun open(vararg summits: Pair<Double, Double>, radiusMeters: Int = 4000): PeakOverlay = runBlocking {
        val builder = builder(radiusMeters)
        summits.forEach { builder.addSummit(it.first, it.second) }
        PeakOverlay.open(ByteRangeReader.fromBytes(builder.build()))!!
    }

    private val summit = 44.0 to -71.0

    @Test
    fun scalesTheElevationAboveTheBaseAtTheSummit() = runBlocking {
        assertEquals(600f, open(summit).getOffset(44.0, -71.0, 1000f), 0.01f)
    }

    @Test
    fun addsNothingBelowTheBase() = runBlocking {
        assertEquals(0f, open(summit).getOffset(44.0, -71.0, 300f), 0f)
    }

    @Test
    fun followsTheElevationOfTheModel() = runBlocking {
        val overlay = open(summit)

        assertEquals(1200f, overlay.getOffset(44.0, -71.0, 1600f), 0.01f)
        assertEquals(300f, overlay.getOffset(44.0, -71.0, 700f), 0.01f)
    }

    @Test
    fun fadesToNothingAtTheRadius() = runBlocking {
        val overlay = open(summit)
        val degreesFor4Km = 4.0 / 111.0

        assertEquals(0f, overlay.getOffset(44.0 + degreesFor4Km * 1.01, -71.0, 1000f), 0f)
        assertEquals(0f, overlay.getOffset(44.0 + degreesFor4Km * 2, -71.0, 1000f), 0f)
        // Halfway there the amount is (1 - 0.25)^2 of what it is at the summit
        assertEquals(600f * 0.5625f, overlay.getOffset(44.0 + degreesFor4Km / 2, -71.0, 1000f), 0.5f)
    }

    @Test
    fun usesTheRadiusOfTheFile() = runBlocking {
        val overlay = open(summit, radiusMeters = 2000)
        val degreesFor2Km = 2.0 / 111.0

        assertEquals(0f, overlay.getOffset(44.0 + degreesFor2Km * 1.01, -71.0, 1000f), 0f)
        assertEquals(600f * 0.5625f, overlay.getOffset(44.0 + degreesFor2Km / 2, -71.0, 1000f), 0.5f)
    }

    @Test
    fun usesTheDistanceOnTheGroundInLongitude() = runBlocking {
        val overlay = open(summit)
        val degreesFor4Km = 4.0 / (111.0 * cos(Math.toRadians(44.0)))

        assertEquals(0f, overlay.getOffset(44.0, -71.0 + degreesFor4Km * 1.01, 1000f), 0f)
        assertTrue(overlay.getOffset(44.0, -71.0 + degreesFor4Km * 0.9, 1000f) > 0f)
    }

    @Test
    fun nearbySummitsAddUp() = runBlocking {
        assertEquals(1200f, open(summit, summit).getOffset(44.0, -71.0, 1000f), 0.01f)
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
        // Fine enough that the area each point covers barely changes the amount
        val latitudes = doubleArrayOf(43.9999, 44.0, 44.0001)
        val longitudes = doubleArrayOf(-71.0001, -71.0, -70.9999)
        // Two channels, the second is not changed
        val data = FloatArray(3 * 3 * 2) { 1000f }
        data[(1 * 3 + 0) * 2] = 0f

        overlay.addTo(latitudes, longitudes, data, 2)

        assertEquals(1000f + overlay.getOffset(44.0, -71.0, 1000f), data[(1 * 3 + 1) * 2], 0.01f)
        assertEquals(1000f + overlay.getOffset(43.9999, -71.0001, 1000f), data[0], 0.5f)
        assertEquals(0f, data[(1 * 3 + 0) * 2], 0f)
        assertEquals(1000f, data[(1 * 3 + 1) * 2 + 1], 0f)
    }

    @Test
    fun leavesAGridAwayFromSummitsAlone() = runBlocking {
        val data = FloatArray(4) { 1000f }

        open(summit).addTo(doubleArrayOf(44.5, 44.51), doubleArrayOf(-71.0, -70.99), data, 1)

        assertTrue(data.all { it == 1000f })
    }

    @Test
    fun keepsASummitOnACoarseGrid() = runBlocking {
        // A 0.1 degree grid, so no point is within the radius of the summit
        val overlay = open(44.02 to -71.02)
        val data = FloatArray(4) { 1000f }

        overlay.addTo(doubleArrayOf(44.0, 44.1), doubleArrayOf(-71.0, -70.9), data, 1)

        // The summit is in the area the nearest point covers, so that point is raised as much as at the summit
        assertEquals(1600f, data[0], 0.01f)
        assertEquals(1000f, data[3], 0f)
    }

    @Test
    fun summitsInTheAreaOfAPointDoNotStack() = runBlocking {
        val overlay = open(44.02 to -71.02, 44.03 to -71.01)
        val data = FloatArray(4) { 1000f }

        overlay.addTo(doubleArrayOf(44.0, 44.1), doubleArrayOf(-71.0, -70.9), data, 1)

        assertEquals(1600f, data[0], 0.01f)
    }

    private fun openCountingReads(latitude: Double, longitude: Double): Pair<PeakOverlay, () -> Int> = runBlocking {
        val bytes = ByteRangeReader.fromBytes(builder().addSummit(latitude, longitude).build())
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
        val longitudes = doubleArrayOf(179.99, 179.995, -180.0, -179.995)
        val data = FloatArray(4) { 1000f }

        overlay.addTo(doubleArrayOf(10.0), longitudes, data, 1)

        assertEquals(1600f, data[1], 0.01f)
        assertTrue(data[0] > 1500f)
        assertTrue(data[2] > 1500f)
        assertTrue(data[3] > 1500f)
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
    fun ignoresFilesItDoesNotUnderstand() = runBlocking {
        suspend fun open(builder: GeoPointFileBuilder) = PeakOverlay.open(ByteRangeReader.fromBytes(builder.build()))

        assertNotNull(open(builder()))
        assertNull(PeakOverlay.open(ByteRangeReader.fromBytes(ByteArray(0))))
        assertNull(open(builder(type = "cell_towers")))
        assertNull(open(builder(radiusMeters = null)))
        assertNull(open(builder(radiusMeters = "4000")))
        assertNull(open(builder(radiusMeters = 0)))
        assertNull(open(builder(fields = fields.take(1))))
    }
}
