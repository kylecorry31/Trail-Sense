package com.kylecorry.trail_sense.navigation.paths.ui.drawing

import android.graphics.Path
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.geometry.Geometry
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.toPixelCoordinate
import com.kylecorry.trail_sense.shared.toVector2

class ClippedPathRenderer(
    private val bounds: Rectangle,
    private val mapper: (Coordinate) -> PixelCoordinate
) : IRenderedPathFactory {
    override fun render(points: List<Coordinate>, path: Path): RenderedPath {
        val origin = CoordinateBounds.from(points).center
        val originPx = mapper(origin)
        for (i in 1 until points.size) {
            val start = mapper(points[i - 1])
            if (i == 1) {
                path.moveTo(start.x - originPx.x, start.y - originPx.y)
            }

            val end = mapper(points[i])
            drawLine(originPx, start, end, path)
        }
        return RenderedPath(origin, path)
    }

    private fun drawLine(
        origin: PixelCoordinate,
        start: PixelCoordinate,
        end: PixelCoordinate,
        path: Path
    ) {

        val a = start.toVector2(bounds.top)
        val b = end.toVector2(bounds.top)

        // Both are in
        if (bounds.contains(a) && bounds.contains(b)) {
            path.lineTo(end.x - origin.x, end.y - origin.y)
            return
        }

        val intersection =
            Geometry.getIntersection(a, b, bounds).map { it.toPixelCoordinate(bounds.top) }

        // A is in, B is not
        if (bounds.contains(a)) {
            if (intersection.any()) {
                path.lineTo(intersection[0].x - origin.x, intersection[0].y - origin.y)
            }
            path.moveTo(end.x - origin.x, end.y - origin.y)
            return
        }

        // B is in, A is not
        if (bounds.contains(b)) {
            if (intersection.any()) {
                path.moveTo(intersection[0].x - origin.x, intersection[0].y - origin.y)
            }
            path.lineTo(end.x - origin.x, end.y - origin.y)
            return
        }

        // Both are out, but may intersect
        if (intersection.size == 2) {
            path.moveTo(intersection[0].x - origin.x, intersection[0].y - origin.y)
            path.lineTo(intersection[1].x - origin.x, intersection[1].y - origin.y)
        }
        path.moveTo(end.x - origin.x, end.y - origin.y)
    }
}