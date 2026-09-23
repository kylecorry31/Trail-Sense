package com.kylecorry.trail_sense.shared.dem

import com.kylecorry.andromeda.bitmaps.FloatBitmap
import com.kylecorry.sol.units.Coordinate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DEMElevationBitmapTest {
    private fun grid(longitudes: DoubleArray = doubleArrayOf(10.0, 11.0)): DEM.ElevationBitmap {
        val data = FloatBitmap(2, 2, 1)
        floatArrayOf(0f, 100f, 200f, 300f).copyInto(data.data)
        return DEM.ElevationBitmap(data, doubleArrayOf(0.0, 1.0), longitudes)
    }

    @Test
    fun interpolatesTheExistingDemGrid() {
        assertEquals(150f, grid().sample(Coordinate(0.5, 10.5)))
        assertEquals(75f, grid().sample(Coordinate(0.25, 10.25)))
        assertEquals(0f, grid().sample(Coordinate(0.0, 10.0)))
        assertEquals(300f, grid().sample(Coordinate(1.0, 11.0)))
    }

    @Test
    fun samplesAcrossTheDateLine() {
        val grid = grid(doubleArrayOf(179.0, -179.0))
        assertEquals(150f, grid.sample(Coordinate(0.5, 180.0)))
        assertEquals(300f, grid.sample(Coordinate(1.0, -179.0)))
    }

    @Test
    fun rejectsLocationsOutsideTheCachedGrid() {
        assertNull(grid().sample(Coordinate(-0.1, 10.5)))
        assertNull(grid().sample(Coordinate(0.5, 9.9)))
        assertNull(grid().sample(Coordinate(1.1, 10.5)))
        assertNull(grid().sample(Coordinate(0.5, 11.1)))
    }
}
