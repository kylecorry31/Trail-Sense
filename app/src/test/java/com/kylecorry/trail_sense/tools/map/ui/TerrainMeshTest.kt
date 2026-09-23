package com.kylecorry.trail_sense.tools.map.ui

import com.kylecorry.sol.math.Vector2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TerrainMeshTest {
    @Test
    fun terrainPaddingCoversScreenEdgesWhenTerrainDropsAwayFromCenter() {
        val heights = FloatArray(TerrainMesh.VERTEX_COUNT) { -1000f }
        heights[(TerrainMesh.ROWS / 2) * (TerrainMesh.COLUMNS + 1) + TerrainMesh.COLUMNS / 2] = 1000f
        val groundHeight = TerrainMesh.groundHeight(800f, 2000f, 2f)
        val mesh = TerrainMesh(400f, 800f, heights, 2f, groundHeight)
        assertTrue(mesh.vertices[1] < 0f)
        assertTrue(mesh.vertices[mesh.vertices.size - 1] > 800f)
        assertTrue(mesh.toMap(Vector2(100f, 0f)) != null)
        assertTrue(mesh.toMap(Vector2(100f, 800f)) != null)
    }

    @Test
    fun flatTerrainHas45DegreeTiltAndKeepsCenterFixed() {
        val mesh = TerrainMesh(400f, 800f, FloatArray(TerrainMesh.VERTEX_COUNT), 2f)
        val center = mesh.toMap(Vector2(200f, 400f))!!
        assertEquals(200f, center.x, 0.001f)
        assertEquals(400f, center.y, 0.001f)
        val point = mesh.toMap(Vector2(123f, 500f))!!
        assertEquals(123f, point.x, 0.001f)
        assertEquals(400f + 100f / TerrainMesh.TILT_COS, point.y, 0.001f)
    }

    @Test
    fun absoluteElevationDoesNotMoveTheMap() {
        val seaLevel = TerrainMesh(400f, 800f, FloatArray(TerrainMesh.VERTEX_COUNT), 2f)
        val plateau = TerrainMesh(400f, 800f, FloatArray(TerrainMesh.VERTEX_COUNT) { 3000f }, 2f)
        seaLevel.vertices.forEachIndexed { index, value ->
            assertEquals(value, plateau.vertices[index], 0.001f)
        }
    }

    @Test
    fun elevatedTerrainCanBeSelectedAtItsRenderedPosition() {
        val elevations = FloatArray(TerrainMesh.VERTEX_COUNT) { index ->
            (index % (TerrainMesh.COLUMNS + 1)) * 10f
        }
        val mesh = TerrainMesh(400f, 800f, elevations, 2f)
        val index = 20 * (TerrainMesh.COLUMNS + 1) + 24
        val rendered = Vector2(mesh.vertices[index * 2], mesh.vertices[index * 2 + 1])
        val original = mesh.source(24, 20)
        val selected = mesh.toMap(rendered)!!
        assertEquals(original.x, selected.x, 0.001f)
        assertEquals(original.y, selected.y, 0.001f)
        val depth = 1f - (original.y - 400f).coerceAtMost(0f) * TerrainMesh.TILT_SIN / 2400f
        val flatY = 400f + (original.y - 400f) * TerrainMesh.TILT_COS / depth
        assertEquals(flatY - 40f * TerrainMesh.TILT_SIN * TerrainMesh.ELEVATION_EXAGGERATION / depth,
            rendered.y, 0.001f)
    }

    @Test
    fun pointsOutsideTerrainAreNotSelected() {
        val mesh = TerrainMesh(400f, 800f, FloatArray(TerrainMesh.VERTEX_COUNT), 2f)
        assertNull(mesh.toMap(Vector2(-1f, 400f)))
        assertNull(mesh.toMap(Vector2(200f, -1000f)))
    }
}
