package com.kylecorry.trail_sense.shared.canvas

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.geometry.Rectangle

class PolygonClipper {

    fun clip(
        polygon: List<PixelCoordinate>,
        bounds: Rectangle
    ): List<PixelCoordinate> {
        var output = polygon

        // Clip against Left
        output = clipEdge(output, bounds.left, true, Axis.X)

        // Clip against Right
        output = clipEdge(output, bounds.right, false, Axis.X)

        // Clip against Top
        output = clipEdge(output, bounds.top, true, Axis.Y)

        // Clip against Bottom
        output = clipEdge(output, bounds.bottom, false, Axis.Y)

        return output
    }

    private fun clipEdge(
        polygon: List<PixelCoordinate>,
        edge: Float,
        keepGreater: Boolean,
        axis: Axis
    ): List<PixelCoordinate> {
        if (polygon.isEmpty()) return emptyList()

        val output = mutableListOf<PixelCoordinate>()
        var start = polygon.last()

        for (end in polygon) {
            val startVal = axis.get(start)
            val endVal = axis.get(end)

            val isStartInside = if (keepGreater) startVal >= edge else startVal <= edge
            val isEndInside = if (keepGreater) endVal >= edge else endVal <= edge

            if (isStartInside && isEndInside) {
                output.add(end)
            } else if (isStartInside && !isEndInside) {
                output.add(intersect(start, end, edge, axis))
            } else if (!isStartInside && isEndInside) {
                output.add(intersect(start, end, edge, axis))
                output.add(end)
            }

            start = end
        }

        return output
    }

    private fun intersect(
        start: PixelCoordinate,
        end: PixelCoordinate,
        edge: Float,
        axis: Axis
    ): PixelCoordinate {
        val startVal = axis.get(start)
        val endVal = axis.get(end)
        val t = (edge - startVal) / (endVal - startVal)

        return when (axis) {
            Axis.X -> PixelCoordinate(edge, start.y + (end.y - start.y) * t)
            Axis.Y -> PixelCoordinate(start.x + (end.x - start.x) * t, edge)
        }
    }

    private enum class Axis {
        X,
        Y;

        fun get(pixel: PixelCoordinate): Float {
            return if (this == X) pixel.x else pixel.y
        }
    }
}
