package com.kylecorry.trail_sense.shared.canvas

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.geometry.Rectangle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PolygonClipperTest {

    private val clipper = PolygonClipper()

    // Canvas bounds: y grows downward, so top is the smaller value
    private val bounds = Rectangle(0f, 0f, 100f, 100f)

    @Test
    fun returnsEmptyForAnEmptyPolygon() {
        assertEquals(emptyList<PixelCoordinate>(), clipper.clip(emptyList(), bounds))
    }

    @Test
    fun keepsPolygonsThatAreFullyInside() {
        val polygon = listOf(
            PixelCoordinate(10f, 10f),
            PixelCoordinate(90f, 10f),
            PixelCoordinate(90f, 90f),
            PixelCoordinate(10f, 90f)
        )

        assertPolygon(polygon, clipper.clip(polygon, bounds))
    }

    @Test
    fun removesPolygonsThatAreFullyOutside() {
        val polygon = listOf(
            PixelCoordinate(200f, 200f),
            PixelCoordinate(300f, 200f),
            PixelCoordinate(300f, 300f),
            PixelCoordinate(200f, 300f)
        )

        assertEquals(emptyList<PixelCoordinate>(), clipper.clip(polygon, bounds))
    }

    @Test
    fun clipsPolygonsThatCrossAnEdge() {
        val polygon = listOf(
            PixelCoordinate(50f, 10f),
            PixelCoordinate(150f, 10f),
            PixelCoordinate(150f, 90f),
            PixelCoordinate(50f, 90f)
        )

        assertPolygon(
            listOf(
                PixelCoordinate(50f, 10f),
                PixelCoordinate(100f, 10f),
                PixelCoordinate(100f, 90f),
                PixelCoordinate(50f, 90f)
            ),
            clipper.clip(polygon, bounds)
        )
    }

    @Test
    fun clipsPolygonsThatSurroundTheBounds() {
        val polygon = listOf(
            PixelCoordinate(-50f, -50f),
            PixelCoordinate(150f, -50f),
            PixelCoordinate(150f, 150f),
            PixelCoordinate(-50f, 150f)
        )

        assertPolygon(
            listOf(
                PixelCoordinate(0f, 100f),
                PixelCoordinate(0f, 0f),
                PixelCoordinate(100f, 0f),
                PixelCoordinate(100f, 100f)
            ),
            clipper.clip(polygon, bounds)
        )
    }

    /**
     * The horizontal edges have to be interpolated along x, even when the vertex being clipped
     * happens to have the same x and y value.
     */
    @Test
    fun clipsHorizontalEdgesWhenTheVertexHasEqualCoordinates() {
        // Wider to the left than it is tall, so a vertex above the top edge can still be within
        // the left and right edges while having x == y
        val bounds = Rectangle(-200f, -100f, 100f, 100f)
        val polygon = listOf(
            PixelCoordinate(-150f, -150f),
            PixelCoordinate(50f, 0f),
            PixelCoordinate(-50f, 0f)
        )

        assertPolygon(
            listOf(
                PixelCoordinate(-116.667f, -100f),
                PixelCoordinate(-83.333f, -100f),
                PixelCoordinate(50f, 0f),
                PixelCoordinate(-50f, 0f)
            ),
            clipper.clip(polygon, bounds)
        )
    }

    @Test
    fun clippedVerticesStayWithinTheBounds() {
        val polygon = listOf(
            PixelCoordinate(-500f, 50f),
            PixelCoordinate(50f, -500f),
            PixelCoordinate(500f, 50f),
            PixelCoordinate(50f, 500f)
        )

        val clipped = clipper.clip(polygon, bounds)

        assertTrue(clipped.isNotEmpty())
        clipped.forEach {
            assertTrue(it.x in bounds.left..bounds.right, "x out of bounds: $it")
            assertTrue(it.y in bounds.top..bounds.bottom, "y out of bounds: $it")
        }
    }

    private fun assertPolygon(expected: List<PixelCoordinate>, actual: List<PixelCoordinate>) {
        assertEquals(expected.size, actual.size, "Expected $expected but was $actual")
        expected.zip(actual).forEach { (e, a) ->
            assertEquals(e.x, a.x, 0.001f, "Expected $expected but was $actual")
            assertEquals(e.y, a.y, 0.001f, "Expected $expected but was $actual")
        }
    }
}
