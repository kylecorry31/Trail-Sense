package com.kylecorry.trail_sense.tools.augmented_reality.domain.ground

import com.kylecorry.sol.math.Vector3
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OccupancyGridTest {

    private val camera = Vector3(0f, 0f, 0f)

    private fun OccupancyGrid.isOccupied(p: Vector3): Boolean {
        return occupied().any { (center(it) - p).magnitude() < voxelSize * 0.9f }
    }

    @Test
    fun needsTwoFeaturesToAgree() {
        val grid = OccupancyGrid(0.3f)
        val rock = Vector3(0.1f, 3.05f, -1.0f)
        val voxel = Vector3(0.15f, 3.15f, -0.9f)
        // One feature tracked over many frames is still one opinion
        repeat(10) { grid.add(camera, mapOf(1L to rock)) }
        assertFalse(grid.isOccupied(voxel))
        grid.add(camera, mapOf(1L to rock, 2L to Vector3(0.2f, 3.1f, -0.95f)))
        assertTrue(grid.isOccupied(voxel))
    }

    @Test
    fun seeingPastSomethingClearsIt() {
        val grid = OccupancyGrid(0.3f)
        val person = Vector3(0.1f, 2.05f, -0.4f)
        grid.add(camera, (1L..3L).associateWith { person })
        assertTrue(grid.isOccupied(Vector3(0.15f, 2.15f, -0.45f)))
        // They walked off; now new features on the wall behind are visible through where they stood
        val wall = Vector3(0.2f, 4.1f, -0.8f)
        grid.add(camera, (10L..19L).associateWith { wall })
        assertFalse(grid.isOccupied(Vector3(0.15f, 2.15f, -0.45f)))
        assertTrue(grid.isOccupied(Vector3(0.15f, 4.05f, -0.75f)))
    }
}
