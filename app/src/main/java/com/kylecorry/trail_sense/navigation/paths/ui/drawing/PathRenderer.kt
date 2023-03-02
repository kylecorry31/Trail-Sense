package com.kylecorry.trail_sense.navigation.paths.ui.drawing

import android.graphics.Path
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate

class PathRenderer(private val mapper: (Coordinate) -> PixelCoordinate) : IRenderedPathFactory {
    override fun render(points: List<Coordinate>, path: Path): RenderedPath {
        val origin = CoordinateBounds.from(points).center
        val originPx = mapper(origin)
        for (i in 1 until points.size) {
            if (i == 1) {
                val start = mapper(points[0])
                path.moveTo(start.x - originPx.x, start.y - originPx.y)
            }

            val end = mapper(points[i])
            path.lineTo(end.x - originPx.x, end.y - originPx.y)
        }
        return RenderedPath(origin, path)
    }
}