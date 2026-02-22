package com.kylecorry.trail_sense.shared.map_layers.tiles

import com.kylecorry.sol.units.Coordinate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TileMathTest {

    @Test
    fun getZoomLevel() {
        assertEquals(0.0f, TileMath.getZoomLevel(Coordinate.zero, 156543.03f), 0.01f)
        assertEquals(1.0f, TileMath.getZoomLevel(Coordinate.zero, 78271.52f), 0.01f)
        assertEquals(2.0f, TileMath.getZoomLevel(Coordinate.zero, 39135.76f), 0.01f)
        assertEquals(3.0f, TileMath.getZoomLevel(Coordinate.zero, 19567.88f), 0.01f)
        assertEquals(4.0f, TileMath.getZoomLevel(Coordinate.zero, 9783.94f), 0.01f)
        assertEquals(5.0f, TileMath.getZoomLevel(Coordinate.zero, 4891.97f), 0.01f)
        assertEquals(6.0f, TileMath.getZoomLevel(Coordinate.zero, 2445.98f), 0.01f)
        assertEquals(7.0f, TileMath.getZoomLevel(Coordinate.zero, 1222.99f), 0.01f)
        assertEquals(8.0f, TileMath.getZoomLevel(Coordinate.zero, 611.50f), 0.01f)
        assertEquals(9.0f, TileMath.getZoomLevel(Coordinate.zero, 305.75f), 0.01f)
        assertEquals(10.0f, TileMath.getZoomLevel(Coordinate.zero, 152.87f), 0.01f)
        assertEquals(11.0f, TileMath.getZoomLevel(Coordinate.zero, 76.44f), 0.01f)
        assertEquals(12.0f, TileMath.getZoomLevel(Coordinate.zero, 38.22f), 0.01f)
        assertEquals(13.0f, TileMath.getZoomLevel(Coordinate.zero, 19.11f), 0.01f)
        assertEquals(14.0f, TileMath.getZoomLevel(Coordinate.zero, 9.55f), 0.01f)
        assertEquals(15.0f, TileMath.getZoomLevel(Coordinate.zero, 4.78f), 0.01f)
        assertEquals(16.0f, TileMath.getZoomLevel(Coordinate.zero, 2.39f), 0.01f)
        assertEquals(17.0f, TileMath.getZoomLevel(Coordinate.zero, 1.19f), 0.01f)
        assertEquals(18.0f, TileMath.getZoomLevel(Coordinate.zero, 0.60f), 0.01f)
        assertEquals(19.0f, TileMath.getZoomLevel(Coordinate.zero, 0.30f), 0.01f)
        assertEquals(20.0f, TileMath.getZoomLevel(Coordinate.zero, 0.15f), 0.01f)
        assertEquals(1.0f, TileMath.getZoomLevel(Coordinate.zero, 0f), 0.01f)
        assertEquals(16.458f, TileMath.getZoomLevel(Coordinate(90.0, 0.0), 0.15f), 0.01f)
        assertEquals(16.458f, TileMath.getZoomLevel(Coordinate(-90.0, 0.0), 0.15f), 0.01f)
    }
}
