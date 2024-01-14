package com.kylecorry.trail_sense.navigation.paths.ui.drawing

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.geometry.Geometry
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.extensions.isSamePixel
import com.kylecorry.trail_sense.shared.toPixelCoordinate
import com.kylecorry.trail_sense.shared.toVector2

class ClippedPathRenderer(
    private val bounds: Rectangle,
    private val mapper: (Coordinate) -> PixelCoordinate
) : IRenderedPathFactory {
    override fun render(points: List<Coordinate>, path: MutableList<Float>): RenderedPath {
        val origin = CoordinateBounds.from(points).center
        val originPx = mapper(origin)
        for (i in 1 until points.size) {
            val start = mapper(points[i - 1])
            val end = mapper(points[i])
            // If the start and end are the same, don't draw a line
            if (start.isSamePixel(end)) {
                continue
            }
            addLine(bounds, originPx, start, end, path)
        }
        return RenderedPath(origin, path)
    }

    private fun addLine(
        bounds: Rectangle,
        origin: PixelCoordinate,
        start: PixelCoordinate,
        end: PixelCoordinate,
        lines: MutableList<Float>
    ) {
        val a = start.toVector2(bounds.top)
        val b = end.toVector2(bounds.top)

        // Both are in
        if (bounds.contains(a) && bounds.contains(b)) {
            lines.add(start.x - origin.x)
            lines.add(start.y - origin.y)
            lines.add(end.x - origin.x)
            lines.add(end.y - origin.y)
            return
        }

        val intersection =
            Geometry.getIntersection(a, b, bounds).map { it.toPixelCoordinate(bounds.top) }

        // A is in, B is not
        if (bounds.contains(a)) {
            if (intersection.any()) {
                lines.add(start.x - origin.x)
                lines.add(start.y - origin.y)
                lines.add(intersection[0].x - origin.x)
                lines.add(intersection[0].y - origin.y)
            }
            return
        }

        // B is in, A is not
        if (bounds.contains(b)) {
            if (intersection.any()) {
                lines.add(intersection[0].x - origin.x)
                lines.add(intersection[0].y - origin.y)
                lines.add(end.x - origin.x)
                lines.add(end.y - origin.y)
            }
            return
        }

        // Both are out, but may intersect
        if (intersection.size == 2) {
            lines.add(intersection[0].x - origin.x)
            lines.add(intersection[0].y - origin.y)
            lines.add(intersection[1].x - origin.x)
            lines.add(intersection[1].y - origin.y)
        }
    }
}