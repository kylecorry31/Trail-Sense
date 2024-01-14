package com.kylecorry.trail_sense.navigation.paths.ui.drawing

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.extensions.isSamePixel

class PathRenderer(private val mapper: (Coordinate) -> PixelCoordinate) : IRenderedPathFactory {
    override fun render(points: List<Coordinate>, path: MutableList<Float>): RenderedPath {
        val origin = CoordinateBounds.from(points).center
        val originPx = mapper(origin)
        var lastPoint: PixelCoordinate? = null
        for (i in 1 until points.size) {
            if (i == 1) {
                val start = mapper(points[0])
                lastPoint = start
            }

            val start = lastPoint ?: continue
            val end = mapper(points[i])
            // If the end point is the same as the previous, don't draw a line
            if (start.isSamePixel(end)) {
                continue
            }
            lastPoint = end
            path.add(start.x - originPx.x)
            path.add(start.y - originPx.y)
            path.add(end.x - originPx.x)
            path.add(end.y - originPx.y)
        }
        return RenderedPath(origin, path)
    }
}