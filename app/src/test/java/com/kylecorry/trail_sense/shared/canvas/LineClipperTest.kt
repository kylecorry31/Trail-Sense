package com.kylecorry.trail_sense.shared.canvas

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.geometry.Rectangle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LineClipperTest {

    private val clipper = LineClipper()

    // Pixels are measured down from the top, so a pixel y of 0 sits at the rectangle's top
    private val bounds = Rectangle(0f, 100f, 100f, 0f)

    @Test
    fun producesNoLinesForFewerThanTwoPoints() {
        assertLines(emptyList(), clip(emptyList()))
        assertLines(emptyList(), clip(listOf(PixelCoordinate(10f, 10f))))
    }

    @Test
    fun keepsLinesThatAreFullyInside() {
        val lines = clip(listOf(PixelCoordinate(10f, 10f), PixelCoordinate(50f, 50f)))

        assertLines(listOf(10f, 10f, 50f, 50f), lines)
    }

    @Test
    fun emitsOneLinePerSegment() {
        val lines = clip(
            listOf(
                PixelCoordinate(10f, 10f),
                PixelCoordinate(50f, 50f),
                PixelCoordinate(80f, 20f)
            )
        )

        assertLines(listOf(10f, 10f, 50f, 50f, 50f, 50f, 80f, 20f), lines)
    }

    @Test
    fun offsetsLinesByTheOrigin() {
        val lines = clip(
            listOf(PixelCoordinate(10f, 10f), PixelCoordinate(50f, 50f)),
            origin = PixelCoordinate(5f, 5f)
        )

        assertLines(listOf(5f, 5f, 45f, 45f), lines)
    }

    @Test
    fun removesLinesThatAreFullyOutside() {
        val lines = clip(listOf(PixelCoordinate(200f, 10f), PixelCoordinate(300f, 50f)))

        assertLines(emptyList(), lines)
    }

    @Test
    fun clipsLinesThatLeaveTheBounds() {
        val lines = clip(listOf(PixelCoordinate(50f, 50f), PixelCoordinate(150f, 50f)))

        assertLines(listOf(50f, 50f, 100f, 50f), lines)
    }

    @Test
    fun clipsLinesThatEnterTheBounds() {
        val lines = clip(listOf(PixelCoordinate(150f, 50f), PixelCoordinate(50f, 50f)))

        assertLines(listOf(100f, 50f, 50f, 50f), lines)
    }

    @Test
    fun clipsLinesThatPassThroughTheBounds() {
        val lines = clip(listOf(PixelCoordinate(-50f, 50f), PixelCoordinate(150f, 50f)))

        assertLines(listOf(0f, 50f, 100f, 50f), lines)
    }

    @Test
    fun skipsRepeatedPoints() {
        val lines = clip(
            listOf(
                PixelCoordinate(10f, 10f),
                PixelCoordinate(10f, 10f),
                PixelCoordinate(50f, 50f)
            )
        )

        assertLines(listOf(10f, 10f, 50f, 50f), lines)
    }

    @Test
    fun skipsPointsThatAreNaN() {
        val lines = clip(
            listOf(
                PixelCoordinate(10f, 10f),
                PixelCoordinate(Float.NaN, 20f),
                PixelCoordinate(50f, 50f)
            )
        )

        assertLines(listOf(10f, 10f, 50f, 50f), lines)
    }

    @Test
    fun removesLinesThatWrapWhenRequested() {
        // A line from far off the right of the screen to far off the left is behind the camera
        val points = listOf(PixelCoordinate(300f, 50f), PixelCoordinate(-200f, 50f))

        assertLines(emptyList(), clip(points, preventLineWrapping = true))
    }

    /**
     * When both ends are outside, the clipped segment is ordered by the bounds' edges rather than
     * by the direction of travel.
     */
    @Test
    fun keepsWrappingLinesByDefault() {
        val points = listOf(PixelCoordinate(300f, 50f), PixelCoordinate(-200f, 50f))

        assertLines(listOf(0f, 50f, 100f, 50f), clip(points))
    }

    @Test
    fun simplifiesLinesWithTheRdpFilter() {
        val points = listOf(
            PixelCoordinate(10f, 10f),
            PixelCoordinate(30f, 30f),
            PixelCoordinate(50f, 50f)
        )

        // Without the filter the collinear midpoint is kept
        assertLines(listOf(10f, 10f, 30f, 30f, 30f, 30f, 50f, 50f), clip(points))

        assertLines(listOf(10f, 10f, 50f, 50f), clip(points, rdpFilterEpsilon = 1f))
    }

    @Test
    fun interpolatesZValuesForLinesThatAreInside() {
        val zOutput = mutableListOf<Float>()
        clip(
            listOf(PixelCoordinate(10f, 10f), PixelCoordinate(50f, 50f)),
            zValues = listOf(0f, 10f),
            zOutput = zOutput
        )

        assertLines(listOf(0f, 10f), zOutput)
    }

    @Test
    fun interpolatesZValuesAtTheClippedPoint() {
        val zOutput = mutableListOf<Float>()
        // Half of this line is inside the bounds, so the clipped end is halfway between the z values
        clip(
            listOf(PixelCoordinate(50f, 50f), PixelCoordinate(150f, 50f)),
            zValues = listOf(0f, 10f),
            zOutput = zOutput
        )

        assertLines(listOf(0f, 5f), zOutput)
    }

    private fun clip(
        pixels: List<PixelCoordinate>,
        origin: PixelCoordinate = PixelCoordinate(0f, 0f),
        preventLineWrapping: Boolean = false,
        rdpFilterEpsilon: Float? = null,
        zValues: List<Float>? = null,
        zOutput: MutableList<Float>? = null
    ): List<Float> {
        val output = mutableListOf<Float>()
        clipper.clip(
            pixels,
            bounds,
            output,
            origin,
            preventLineWrapping,
            rdpFilterEpsilon,
            zValues,
            zOutput
        )
        return output
    }

    private fun assertLines(expected: List<Float>, actual: List<Float>) {
        assertEquals(expected.size, actual.size, "Expected $expected but was $actual")
        expected.zip(actual).forEach { (e, a) ->
            assertEquals(e, a, 0.001f, "Expected $expected but was $actual")
        }
    }
}
