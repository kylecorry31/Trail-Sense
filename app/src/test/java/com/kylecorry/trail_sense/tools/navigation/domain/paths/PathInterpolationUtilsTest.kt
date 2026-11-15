package com.kylecorry.trail_sense.tools.navigation.domain.paths

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class PathInterpolationUtilsTest {

    private fun points(startElevation: Float?, endElevation: Float?) = listOf(
        PathPoint(1, 1, Coordinate(0.0, 0.0), startElevation),
        PathPoint(2, 1, Coordinate(0.0, 0.002), endElevation)
    )

    private fun snap(percent: Float) = Snap(0, percent, 0f, 0f)

    @Test
    fun `elevation at the ends of a segment is the recorded elevation`() {
        val points = points(100f, 200f)

        assertEquals(100f, PathInterpolationUtils.elevationAt(points, snap(0f)))
        assertEquals(200f, PathInterpolationUtils.elevationAt(points, snap(1f)))
    }

    @Test
    fun `elevation within a segment is linearly interpolated`() {
        assertEquals(125f, PathInterpolationUtils.elevationAt(points(100f, 200f), snap(0.25f))!!, 0.001f)
    }

    @Test
    fun `elevation within a segment is unknown if either end is unknown`() {
        assertNull(PathInterpolationUtils.elevationAt(points(null, 200f), snap(0.5f)))
        assertNull(PathInterpolationUtils.elevationAt(points(100f, null), snap(0.5f)))
        assertNull(PathInterpolationUtils.elevationAt(points(null, null), snap(0.5f)))
    }

    @Test
    fun `elevation at an end ignores the unknown elevation of the other end`() {
        assertEquals(100f, PathInterpolationUtils.elevationAt(points(100f, null), snap(0f)))
        assertEquals(200f, PathInterpolationUtils.elevationAt(points(null, 200f), snap(1f)))
    }

    @Test
    fun `point is at the snapped coordinate and keeps the other fields of its segment`() {
        val points = points(100f, 200f)
        val geometry = RouteGeometry(points.map { it.coordinate })

        val point = PathInterpolationUtils.pointAt(points, geometry, snap(0.5f))

        assertEquals(0.001, point.coordinate.longitude, 0.00001)
        assertEquals(150f, point.elevation!!, 0.001f)
        assertEquals(1L, point.id)
    }
}
