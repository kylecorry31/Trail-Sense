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
        output = clipEdge(output, bounds.left, true) { it.x }

        // Clip against Right
        output = clipEdge(output, bounds.right, false) { it.x }

        // Clip against Top
        output = clipEdge(output, bounds.top, true) { it.y }

        // Clip against Bottom
        output = clipEdge(output, bounds.bottom, false) { it.y }

        return output
    }

    private fun clipEdge(
        polygon: List<PixelCoordinate>,
        edge: Float,
        keepGreater: Boolean,
        getValue: (PixelCoordinate) -> Float
    ): List<PixelCoordinate> {
        if (polygon.isEmpty()) return emptyList()

        val output = mutableListOf<PixelCoordinate>()
        var start = polygon.last()

        for (end in polygon) {
            val startVal = getValue(start)
            val endVal = getValue(end)

            val isStartInside = if (keepGreater) startVal >= edge else startVal <= edge
            val isEndInside = if (keepGreater) endVal >= edge else endVal <= edge

            if (isStartInside && isEndInside) {
                output.add(end)
            } else if (isStartInside && !isEndInside) {
                output.add(intersect(start, end, edge, getValue))
            } else if (!isStartInside && isEndInside) {
                output.add(intersect(start, end, edge, getValue))
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
        getValue: (PixelCoordinate) -> Float
    ): PixelCoordinate {
        val startVal = getValue(start)
        val endVal = getValue(end)
        val t = (edge - startVal) / (endVal - startVal)

        return if (getValue(start) == start.x) {
            // Vertical edge (x is constant)
            val y = start.y + (end.y - start.y) * t
            PixelCoordinate(edge, y)
        } else {
            // Horizontal edge (y is constant)
            val x = start.x + (end.x - start.x) * t
            PixelCoordinate(x, edge)
        }
    }
}
