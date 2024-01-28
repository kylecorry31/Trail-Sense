package com.kylecorry.trail_sense.tools.paths.ui.drawing

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.canvas.LineClipper

class ClippedPathRenderer(
    private val bounds: Rectangle,
    private val mapper: (Coordinate) -> PixelCoordinate,
    private val filterEpsilon: Float? = null
) : IRenderedPathFactory {

    private val clipper = LineClipper()

    override fun render(points: List<Coordinate>, line: MutableList<Float>): RenderedPath {
        val origin = CoordinateBounds.from(points).center
        val originPx = mapper(origin)

        clipper.clip(points.map { mapper(it) }, bounds, line, originPx, rdpFilterEpsilon = filterEpsilon)

        return RenderedPath(origin, line)
    }

}