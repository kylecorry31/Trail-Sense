package com.kylecorry.trail_sense.navigation.paths.ui.drawing

import android.graphics.Path
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.maps.ICoordinateToPixelStrategy

class PathRenderer(private val strategy: ICoordinateToPixelStrategy): IRenderedPathFactory {
    override fun render(points: List<Coordinate>, path: Path): RenderedPath {
        val origin = CoordinateBounds.from(points).center
        val originPx = strategy.getPixels(origin)
        for (i in 1 until points.size) {
            if (i == 1) {
                val start = strategy.getPixels(points[0])
                path.moveTo(start.x - originPx.x, start.y - originPx.y)
            }

            val end = strategy.getPixels(points[i])
            path.lineTo(end.x - originPx.x, end.y - originPx.y)
        }
        return RenderedPath(origin, path)
    }
}