package com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles

import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileMath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VisibleTileCacheTest {
    @Test
    fun reusesTilesUntilViewportOrZoomChanges() {
        val cache = VisibleTileCache(150)
        val bounds = CoordinateBounds(42.0, -71.0, 41.9, -71.1)
        val tiles = cache.get(bounds, 12, 0)
        assertEquals(TileMath.getTiles(bounds, 12), tiles)
        assertSame(tiles, cache.get(bounds.copy(), 12, 0))
        assertEquals(TileMath.getTiles(bounds, 13), cache.get(bounds, 13, 0))
        assertEquals(TileMath.getTiles(bounds, 11), cache.get(bounds, 12, -1))
        val moved = CoordinateBounds(43.0, -70.0, 42.9, -70.1)
        assertEquals(TileMath.getTiles(moved, 12), cache.get(moved, 12, 0))
    }

    @Test
    fun preservesAntimeridianTiles() {
        val cache = VisibleTileCache(150)
        val bounds = CoordinateBounds(1.0, -179.0, -1.0, 179.0)
        assertEquals(TileMath.getTiles(bounds, 5), cache.get(bounds, 5, 0))
    }

    @Test
    fun reducesZoomToStayWithinTileBudget() {
        val cache = VisibleTileCache(10)
        val bounds = CoordinateBounds(42.0, -71.0, 41.0, -72.0)
        val tiles = cache.get(bounds, 12, 0)
        assertTrue(tiles.isNotEmpty())
        assertTrue(tiles.size <= 10)
        assertTrue(tiles.first().z < 12)
        assertEquals(TileMath.getTiles(bounds, tiles.first().z), tiles)
        assertSame(tiles, cache.get(bounds, 12, 0))
    }
}
