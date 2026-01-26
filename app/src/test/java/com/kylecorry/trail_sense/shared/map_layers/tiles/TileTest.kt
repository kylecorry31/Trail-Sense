package com.kylecorry.trail_sense.shared.map_layers.tiles

import android.util.Size
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

internal class TileTest {

    @Test
    fun xyz() {
        val tile = Tile(1, 2, 3)
        assertEquals(1, tile.x)
        assertEquals(2, tile.y)
        assertEquals(3, tile.z)
    }

    @Test
    fun getCenter() {
        val tile = Tile(0, 0, 0)
        val center = tile.getCenter()
        assertEquals(0.0, center.latitude, 0.001)
        assertEquals(0.0, center.longitude, 0.001)
    }

    @Test
    fun getBounds() {
        // Test zoom 0 bounds (Web Mercator projection limits)
        val tile0 = Tile(0, 0, 0)
        val bounds0 = tile0.getBounds()
        assertEquals(85.0511287798066, bounds0.north, 0.001)
        assertEquals(180.0, bounds0.east, 0.001)
        assertEquals(-85.0511287798066, bounds0.south, 0.001)
        assertEquals(-180.0, bounds0.west, 0.001)

        // Test zoom 1 bounds validity
        val tile1 = Tile(0, 0, 1)
        val bounds1 = tile1.getBounds()
        assertFalse(bounds1.north.isNaN())
        assertFalse(bounds1.south.isNaN())
        assertFalse(bounds1.east.isNaN())
        assertFalse(bounds1.west.isNaN())
        assertTrue(bounds1.north >= bounds1.south)
        assertTrue(bounds1.east >= bounds1.west)
    }

    @ParameterizedTest
    @MethodSource("neighborTestCases")
    fun getNeighbor(dx: Int, dy: Int, expectedX: Int, expectedY: Int, description: String) {
        val tile = Tile(0, 0, 1)
        val neighbor = tile.getNeighbor(dx, dy)
        assertEquals(expectedX, neighbor.x, description)
        assertEquals(expectedY, neighbor.y, description)
        assertEquals(1, neighbor.z, description)
    }

    @Test
    fun getParentAndChildren() {
        val tile = Tile(2, 3, 2)
        val parent = tile.getParent()
        assertNotNull(parent)
        assertEquals(1, parent!!.x)
        assertEquals(1, parent.y)
        assertEquals(1, parent.z)

        // Test zoom 0 has no parent
        val rootTile = Tile(0, 0, 0)
        assertNull(rootTile.getParent())


    }

    @Test
    fun getChildren() {
        val tile = Tile(2, 3, 2)
        val children = tile.getChildren()
        assertEquals(4, children.size)
        children.forEach { child -> assertEquals(3, child.z) }
    }

    @Test
    fun getResolution() {
        val mockSize = mock<Size> {
            on { width } doReturn 256
            on { height } doReturn 256
        }
        val tile = Tile(1, 100, 16, mockSize)
        assertEquals(0.208, tile.getResolution(), 0.0005)
    }


    companion object {
        @JvmStatic
        fun neighborTestCases() = listOf(
            Arguments.of(0, 0, 0, 0, "Same tile"),
            Arguments.of(1, 0, 1, 0, "East neighbor"),
            Arguments.of(-1, 0, 1, 0, "West neighbor (wraps around)"),
            Arguments.of(0, -1, 0, 1, "North neighbor (wraps around)"),
            Arguments.of(0, 1, 0, 1, "South neighbor")
        )
    }
}